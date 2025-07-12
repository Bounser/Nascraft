package me.bounser.nascraft.distributed;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * Plugin-only distributed market synchronization using standard Redis operations.
 * No server configuration required - works with any default Redis installation.
 */
public class DistributedMarketSync {

    private final Nascraft plugin;
    private final JedisPool jedisPool;
    private final String serverId;
    private final ConcurrentHashMap<String, ReentrantLock> itemLocks;
    private final ScheduledExecutorService scheduler;
    private volatile boolean enabled = false;
    private Thread pubSubThread;
    private final MarketSyncListener syncListener;
    
    // Simple conflict resolution using timestamps and server priority
    private final long serverStartTime;
    
    // Noise master management
    private volatile String currentNoiseMaster = null;
    private volatile boolean isNoiseMaster = false;
    private final Object noiseMasterLock = new Object();
    
    public DistributedMarketSync(Nascraft plugin, JedisPool jedisPool) {
        this.plugin = plugin;
        this.jedisPool = jedisPool;
        this.serverId = generateUniqueServerId();
        this.itemLocks = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.syncListener = new MarketSyncListener();
        this.serverStartTime = System.currentTimeMillis();
        
        plugin.getLogger().info("Initialized DistributedMarketSync with server ID: " + serverId);
    }
    
    private String generateUniqueServerId() {
        // Create unique server ID using server name + random component
        String serverName = plugin.getServer().getName();
        if (serverName == null || serverName.isEmpty()) {
            serverName = "server";
        }
        return serverName + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    public void enable() {
        if (enabled) return;
        
        try {
            // Test Redis connection first
            if (!testRedisConnection()) {
                plugin.getLogger().warning("Redis connection failed - distributed sync disabled");
                return;
            }
            
            // Start server heartbeat
            startHeartbeat();
            
            // Initialize noise master system
            initializeNoiseMaster();
            
            // Start listening for price updates
            startSyncListener();
            
            // Load current market state
            loadMarketState();
            
            enabled = true;
            plugin.getLogger().info("Distributed Market Sync enabled successfully");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to enable distributed market sync", e);
        }
    }
    
    public void disable() {
        if (!enabled) return;
        
        enabled = false;
        
        // Stop scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        // Stop pub/sub listener
        if (pubSubThread != null && pubSubThread.isAlive()) {
            pubSubThread.interrupt();
        }
        
        // Remove server from active list
        removeServerHeartbeat();
        
        // Step down as noise master if we are one
        if (isNoiseMaster) {
            stepDownAsNoiseMaster();
        }
        
        plugin.getLogger().info("Distributed Market Sync disabled");
    }
    
    private boolean testRedisConnection() {
        try (Jedis jedis = jedisPool.getResource()) {
            String response = jedis.ping();
            return "PONG".equals(response);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Redis connection test failed", e);
            return false;
        }
    }
    
    private void startHeartbeat() {
        // Send heartbeat every 15 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String heartbeatKey = "nascraft:server:" + serverId + ":heartbeat";
                jedis.setex(heartbeatKey, 45, String.valueOf(System.currentTimeMillis()));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to send heartbeat", e);
            }
        }, 0, 15, TimeUnit.SECONDS);
    }
    
    private void startSyncListener() {
        pubSubThread = new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(syncListener, "nascraft:market:updates");
            } catch (Exception e) {
                if (enabled) {
                    plugin.getLogger().log(Level.WARNING, "Market sync listener disconnected", e);
                    // Auto-reconnect after 5 seconds
                    scheduler.schedule(this::startSyncListener, 5, TimeUnit.SECONDS);
                }
            }
        });
        pubSubThread.setDaemon(true);
        pubSubThread.start();
    }
    
    private void loadMarketState() {
        // Load current prices for all items from Redis
        MarketManager marketManager = MarketManager.getInstance();
        
        for (Item item : marketManager.getAllItems()) {
            try {
                loadItemState(item);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load state for item: " + item.getIdentifier(), e);
            }
        }
    }
    
    private void loadItemState(Item item) {
        try (Jedis jedis = jedisPool.getResource()) {
            String priceKey = "nascraft:item:" + item.getIdentifier() + ":price";
            String stockKey = "nascraft:item:" + item.getIdentifier() + ":stock";
            
            String priceStr = jedis.get(priceKey);
            String stockStr = jedis.get(stockKey);
            
            if (priceStr != null && stockStr != null) {
                try {
                    float stock = Float.parseFloat(stockStr);
                    item.getPrice().setStock(stock);
                    
                    plugin.getLogger().info("Loaded item " + item.getIdentifier() + 
                                           " - Stock: " + stock);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid price data for item: " + item.getIdentifier());
                }
            }
        }
    }
    
    /**
     * Synchronize item stock change across all servers
     * Uses Redis WATCH/MULTI/EXEC for atomic operations
     */
    public boolean syncStockChange(Item item, float stockChange) {
        if (!enabled) {
            return false;
        }
        
        String identifier = item.getIdentifier();
        ReentrantLock lock = itemLocks.computeIfAbsent(identifier, k -> new ReentrantLock());
        
        lock.lock();
        try {
            return executeAtomicStockUpdate(item, stockChange);
        } finally {
            lock.unlock();
        }
    }
    
    private boolean executeAtomicStockUpdate(Item item, float stockChange) {
        String identifier = item.getIdentifier();
        String stockKey = "nascraft:item:" + identifier + ":stock";
        String updateKey = "nascraft:item:" + identifier + ":last_update";
        String serverKey = "nascraft:item:" + identifier + ":updated_by";
        
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try (Jedis jedis = jedisPool.getResource()) {
                // Start watching the stock key for changes
                jedis.watch(stockKey);
                
                // Get current stock
                String currentStockStr = jedis.get(stockKey);
                float currentStock = 0.0f;
                
                if (currentStockStr != null) {
                    try {
                        currentStock = Float.parseFloat(currentStockStr);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid stock value for " + identifier + ": " + currentStockStr);
                    }
                }
                
                // Calculate new stock
                float newStock = currentStock + stockChange;
                
                // Start transaction
                Transaction transaction = jedis.multi();
                transaction.set(stockKey, String.valueOf(newStock));
                transaction.set(updateKey, String.valueOf(System.currentTimeMillis()));
                transaction.set(serverKey, serverId);
                
                // Execute transaction
                List<Object> results = transaction.exec();
                
                if (results != null) {
                    // Transaction succeeded
                    // Update local item
                    item.getPrice().setStock(newStock);
                    
                    // Publish update to other servers
                    publishStockUpdate(identifier, stockChange, newStock);
                    
                    plugin.getLogger().info("Successfully synced stock change for " + identifier + 
                                           " - Change: " + stockChange + ", New Stock: " + newStock);
                    return true;
                } else {
                    // Transaction failed due to concurrent modification
                    if (attempt < maxRetries - 1) {
                        // Wait with exponential backoff
                        Thread.sleep(50 * (1L << attempt));
                        continue;
                    }
                    plugin.getLogger().warning("Failed to sync stock change for " + identifier + 
                                             " after " + maxRetries + " attempts");
                    return false;
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error in atomic stock update for " + identifier, e);
                if (attempt < maxRetries - 1) {
                    try {
                        Thread.sleep(100 * (1L << attempt));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        
        return false;
    }
    
    private void publishStockUpdate(String itemId, float stockChange, float newStock) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Create simple message format: server|item|change|newStock|timestamp
            String message = String.format("%s|%s|%.2f|%.2f|%d", 
                serverId, itemId, stockChange, newStock, System.currentTimeMillis());
            
            jedis.publish("nascraft:market:updates", message);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to publish stock update", e);
        }
    }
    
    private void removeServerHeartbeat() {
        try (Jedis jedis = jedisPool.getResource()) {
            String heartbeatKey = "nascraft:server:" + serverId + ":heartbeat";
            jedis.del(heartbeatKey);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to remove server heartbeat", e);
        }
    }
    
    /**
     * Pub/Sub listener for market updates from other servers
     */
    private class MarketSyncListener extends JedisPubSub {
        
        @Override
        public void onMessage(String channel, String message) {
            if (!enabled || !"nascraft:market:updates".equals(channel)) {
                return;
            }
            
            try {
                handleMarketUpdate(message);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error handling market update: " + message, e);
            }
        }
        
        private void handleMarketUpdate(String message) {
            try {
                // Parse message: server|item|change|newStock|timestamp
                String[] parts = message.split("\\|");
                if (parts.length != 5) {
                    plugin.getLogger().warning("Invalid market update message format: " + message);
                    return;
                }
                
                String sourceServerId = parts[0];
                String itemId = parts[1];
                float stockChange = Float.parseFloat(parts[2]);
                float newStock = Float.parseFloat(parts[3]);
                long timestamp = Long.parseLong(parts[4]);
                
                // Ignore our own updates
                if (serverId.equals(sourceServerId)) {
                    return;
                }
                
                // Apply update to local item
                Item item = MarketManager.getInstance().getItem(itemId);
                if (item != null) {
                    // Use lock to prevent conflicts with local updates
                    ReentrantLock lock = itemLocks.computeIfAbsent(itemId, k -> new ReentrantLock());
                    
                    if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                        try {
                            // Check if this update is newer than our last local update
                            if (shouldApplyUpdate(itemId, timestamp)) {
                                item.getPrice().setStock(newStock);
                                
                                plugin.getLogger().info("Applied market update from " + sourceServerId + 
                                                       " for " + itemId + " - New Stock: " + newStock);
                            }
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        // Couldn't get lock, schedule retry
                        scheduler.schedule(() -> handleMarketUpdate(message), 50, TimeUnit.MILLISECONDS);
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error processing market update", e);
            }
        }
        
        private boolean shouldApplyUpdate(String itemId, long updateTimestamp) {
            try (Jedis jedis = jedisPool.getResource()) {
                String lastUpdateKey = "nascraft:item:" + itemId + ":last_update";
                String lastUpdateStr = jedis.get(lastUpdateKey);
                
                if (lastUpdateStr == null) {
                    return true; // No previous update
                }
                
                long lastUpdate = Long.parseLong(lastUpdateStr);
                
                // Apply update if it's newer, or if timestamps are equal but from a "higher priority" server
                if (updateTimestamp > lastUpdate) {
                    return true;
                } else if (updateTimestamp == lastUpdate) {
                    // Use server start time as tiebreaker for deterministic ordering
                    return serverStartTime > System.currentTimeMillis() - 1000; // Recent start = lower priority
                }
                
                return false;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error checking update timestamp for " + itemId, e);
                return false;
            }
        }
    }
    
    /**
     * Get list of active servers for monitoring
     */
    public java.util.Set<String> getActiveServers() {
        try (Jedis jedis = jedisPool.getResource()) {
            java.util.Set<String> activeServers = new java.util.HashSet<>();
            
            // Scan for heartbeat keys
            String pattern = "nascraft:server:*:heartbeat";
            java.util.Set<String> keys = jedis.keys(pattern);
            
            long currentTime = System.currentTimeMillis();
            for (String key : keys) {
                try {
                    String timestampStr = jedis.get(key);
                    if (timestampStr != null) {
                        long timestamp = Long.parseLong(timestampStr);
                        // Consider server active if heartbeat is less than 60 seconds old
                        if (currentTime - timestamp < 60000) {
                            // Extract server ID from key
                            String serverIdFromKey = key.replace("nascraft:server:", "").replace(":heartbeat", "");
                            activeServers.add(serverIdFromKey);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid heartbeat
                }
            }
            
            return activeServers;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error getting active servers", e);
            return new java.util.HashSet<>();
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getServerId() {
        return serverId;
    }

    private void initializeNoiseMaster() {
        if (!Config.getInstance().getDistributedSyncEnabled() || 
            !Config.getInstance().getNoiseMasterEnabled()) {
            return;
        }
        
        // Check if there's a manually specified noise master
        String specifiedMaster = Config.getInstance().getNoiseMasterServerId();
        if (specifiedMaster != null && !specifiedMaster.isEmpty()) {
            if (specifiedMaster.equals(serverId)) {
                becomeNoiseMaster();
            } else {
                // Wait for specified master to come online
                currentNoiseMaster = specifiedMaster;
                isNoiseMaster = false;
            }
        } else if (Config.getInstance().getNoiseMasterAutoElect()) {
            // Try to become noise master if auto-elect is enabled
            tryBecomeNoiseMaster();
        }
        
        // Start noise master health check
        startNoiseMasterHealthCheck();
    }
    
    private void startNoiseMasterHealthCheck() {
        int healthCheckInterval = Config.getInstance().getNoiseMasterHealthCheckInterval();
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkNoiseMasterHealth();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error in noise master health check", e);
            }
        }, healthCheckInterval, healthCheckInterval, TimeUnit.SECONDS);
    }
    
    private void checkNoiseMasterHealth() {
        synchronized (noiseMasterLock) {
            if (currentNoiseMaster != null) {
                // Check if current master is still alive
                if (!isServerAlive(currentNoiseMaster)) {
                    plugin.getLogger().warning("Noise master " + currentNoiseMaster + " is no longer responsive");
                    currentNoiseMaster = null;
                    isNoiseMaster = false;
                    
                    // Try to become the new master if auto-elect is enabled
                    if (Config.getInstance().getNoiseMasterAutoElect()) {
                        tryBecomeNoiseMaster();
                    }
                }
            } else if (Config.getInstance().getNoiseMasterAutoElect()) {
                // No current master, try to become one
                tryBecomeNoiseMaster();
            }
        }
    }
    
    private boolean isServerAlive(String serverId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String heartbeatKey = "nascraft:server:" + serverId + ":heartbeat";
            String timestampStr = jedis.get(heartbeatKey);
            
            if (timestampStr == null) {
                return false;
            }
            
            long timestamp = Long.parseLong(timestampStr);
            long currentTime = System.currentTimeMillis();
            int masterTimeout = Config.getInstance().getNoiseMasterTimeout();
            
            return (currentTime - timestamp) < (masterTimeout * 1000L);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking server health for " + serverId, e);
            return false;
        }
    }
    
    private boolean tryBecomeNoiseMaster() {
        try (Jedis jedis = jedisPool.getResource()) {
            String noiseMasterKey = "nascraft:noise-master";
            
            // Try to set ourselves as noise master with expiration using SetParams
            redis.clients.jedis.params.SetParams params = new redis.clients.jedis.params.SetParams();
            params.nx().ex(Config.getInstance().getNoiseMasterTimeout());
            
            String result = jedis.set(noiseMasterKey, serverId, params);
            
            if ("OK".equals(result)) {
                becomeNoiseMaster();
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error trying to become noise master", e);
            return false;
        }
    }
    
    private void becomeNoiseMaster() {
        synchronized (noiseMasterLock) {
            isNoiseMaster = true;
            currentNoiseMaster = serverId;
            
            // Refresh our noise master lock regularly
            scheduler.scheduleAtFixedRate(this::refreshNoiseMasterLock, 
                Config.getInstance().getNoiseMasterTimeout() / 2, 
                Config.getInstance().getNoiseMasterTimeout() / 2, 
                TimeUnit.SECONDS);
            
            plugin.getLogger().info("This server is now the NOISE MASTER - applying noise to all items");
        }
    }
    
    private void refreshNoiseMasterLock() {
        if (!isNoiseMaster) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            String noiseMasterKey = "nascraft:noise-master";
            jedis.setex(noiseMasterKey, Config.getInstance().getNoiseMasterTimeout(), serverId);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error refreshing noise master lock", e);
        }
    }
    
    private void stepDownAsNoiseMaster() {
        synchronized (noiseMasterLock) {
            if (!isNoiseMaster) return;
            
            isNoiseMaster = false;
            currentNoiseMaster = null;
            
            try (Jedis jedis = jedisPool.getResource()) {
                String noiseMasterKey = "nascraft:noise-master";
                jedis.del(noiseMasterKey);
                plugin.getLogger().info("Stepped down as noise master");
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error stepping down as noise master", e);
            }
        }
    }
    
    /**
     * Check if this server should apply noise to items
     * @return true if this server is the designated noise master
     */
    public boolean shouldApplyNoise() {
        if (!Config.getInstance().getDistributedSyncEnabled() || 
            !Config.getInstance().getNoiseMasterEnabled()) {
            // If distributed sync or noise master is disabled, all servers can apply noise
            return true;
        }
        
        return isNoiseMaster;
    }
    
    /**
     * Get the current noise master server ID
     * @return the server ID of the current noise master, or null if none
     */
    public String getCurrentNoiseMaster() {
        return currentNoiseMaster;
    }
    
    /**
     * Check if this server is the current noise master
     * @return true if this server is the noise master
     */
    public boolean isNoiseMaster() {
        return isNoiseMaster;
    }
    
    /**
     * Manually set a specific server as the noise master
     * @param serverId the server ID to set as noise master
     * @return true if successful
     */
    public boolean setNoiseMaster(String serverId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String noiseMasterKey = "nascraft:noise-master";
            
            // Set the new noise master
            jedis.setex(noiseMasterKey, Config.getInstance().getNoiseMasterTimeout(), serverId);
            
            synchronized (noiseMasterLock) {
                if (serverId.equals(this.serverId)) {
                    becomeNoiseMaster();
                } else {
                    // Step down if we were the master
                    if (isNoiseMaster) {
                        isNoiseMaster = false;
                        plugin.getLogger().info("Stepped down as noise master - new master: " + serverId);
                    }
                    currentNoiseMaster = serverId;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error setting noise master", e);
            return false;
        }
    }
} 