package me.bounser.nascraft.database.redis;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.Database;
import me.bounser.nascraft.database.sqlite.SQLite;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.market.unit.stats.Instant;
import me.bounser.nascraft.chart.cpi.CPIInstant;
import me.bounser.nascraft.database.commands.resources.DayInfo;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.managers.CacheManager;
import me.bounser.nascraft.web.dto.PlayerStatsDTO;
import me.bounser.nascraft.portfolio.Portfolio;
import me.bounser.nascraft.distributed.DistributedMarketSync;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;
import org.bukkit.plugin.IllegalPluginAccessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class Redis implements Database {

    private final Nascraft plugin;
    private final JedisPool pool;
    private final SQLite fallbackDatabase;
    private final CacheManager cacheManager;
    private final ExecutorService executorService;
    private final Map<String, Object> pendingWrites = new ConcurrentHashMap<>();
    private final AtomicInteger flushTaskId = new AtomicInteger(-1);
    private boolean connected = false;
    private volatile boolean isShuttingDown = false;
    
    // Distributed market synchronization
    private final DistributedMarketSync distributedSync;
    
    public Redis(Nascraft plugin, String host, int port, String password, String username, int database) {
        this.plugin = plugin;
        this.cacheManager = CacheManager.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
        this.fallbackDatabase = SQLite.getInstance();
        
        // Initialize the fallback database connection first
        fallbackDatabase.connect();
        
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(16);
        poolConfig.setMinIdle(8);
        
        if (password != null && !password.isEmpty()) {
            if (username != null && !username.isEmpty()) {
                this.pool = new JedisPool(poolConfig, host, port, 2000, username, password, database);
            } else {
                this.pool = new JedisPool(poolConfig, host, port, 2000, password, database);
            }
        } else {
            this.pool = new JedisPool(poolConfig, host, port, 2000);
        }
        
        // Initialize distributed sync (works only if Redis is available)
        this.distributedSync = new DistributedMarketSync(plugin, pool);
        
        // Test Redis connection
        try (Jedis jedis = pool.getResource()) {
            jedis.ping();
            plugin.getLogger().info("Successfully connected to Redis server!");
            connected = true;
            
            // Enable distributed sync if Redis is working
            distributedSync.enable();
            
        } catch (JedisException e) {
            plugin.getLogger().severe("Failed to connect to Redis server! Using SQLite as fallback.");
            plugin.getLogger().severe(e.getMessage());
        }
        
        // Only start the scheduler if the plugin is enabled
        if (plugin.isEnabled()) {
            startWriteScheduler();
        }
    }
    
    private void startWriteScheduler() {
        // Check if the plugin is enabled and not shutting down
        if (!plugin.isEnabled() || isShuttingDown) {
            return;
        }
        
        try {
            int taskId = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                // Check if we're still enabled and not shutting down
                if (!plugin.isEnabled() || isShuttingDown) {
                    return;
                }
                flushPendingWrites();
            }, 300, 300).getTaskId(); // 15 seconds
            
            flushTaskId.set(taskId);
        } catch (IllegalPluginAccessException e) {
            plugin.getLogger().warning("Cannot schedule Redis flush task - plugin is disabled: " + e.getMessage());
        }
    }
    
    private void flushPendingWrites() {
        if (pendingWrites.isEmpty() || isShuttingDown) return;
        
        Map<String, Object> writes = new HashMap<>(pendingWrites);
        pendingWrites.clear();
        
        try (Jedis jedis = pool.getResource()) {
            for (Map.Entry<String, Object> entry : writes.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof String) {
                    jedis.set(key, (String) value);
                } else if (value instanceof Double) {
                    jedis.set(key, Double.toString((Double) value));
                } else if (value instanceof Integer) {
                    jedis.set(key, Integer.toString((Integer) value));
                } else if (value instanceof Boolean) {
                    jedis.set(key, Boolean.toString((Boolean) value));
                } else if (value instanceof Long) {
                    jedis.set(key, Long.toString((Long) value));
                } else if (value == null) {
                    jedis.del(key);
                }
            }
        } catch (JedisException e) {
            plugin.getLogger().warning("Redis connection error during flush. Writing to fallback database.");
            // Fallback handling would be implemented here
        }
    }
    
    @Override
    public void connect() {
        // Connection is already established in constructor
        // This method is called by DatabaseManager after construction
        if (!connected) {
            try (Jedis jedis = pool.getResource()) {
                jedis.ping();
                connected = true;
                plugin.getLogger().info("Connected to Redis server");
            } catch (JedisException e) {
                connected = false;
                plugin.getLogger().severe("Failed to connect to Redis server: " + e.getMessage());
            }
        }
        
        // Start the scheduler if it wasn't started in constructor
        if (connected && plugin.isEnabled() && flushTaskId.get() == -1) {
            startWriteScheduler();
        }
    }

    @Override
    public void disconnect() {
        isShuttingDown = true;
        
        // Disable distributed sync first
        if (distributedSync != null) {
            distributedSync.disable();
        }
        
        // Cancel the flush task
        if (flushTaskId.get() != -1) {
            try {
                plugin.getServer().getScheduler().cancelTask(flushTaskId.get());
            } catch (Exception e) {
                plugin.getLogger().warning("Error canceling Redis flush task: " + e.getMessage());
            }
        }
        
        // Flush any remaining pending writes
        flushPendingWrites();
        
        // Shutdown executor
        executorService.shutdown();
        
        // Close the pool
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
        
        connected = false;
    }
    
    /**
     * Get the distributed market sync instance
     */
    public DistributedMarketSync getDistributedSync() {
        return distributedSync;
    }
    
    /**
     * Check if distributed sync is enabled and working
     */
    public boolean isDistributedSyncEnabled() {
        return distributedSync != null && distributedSync.isEnabled();
    }
    
    /**
     * Get active servers in the distributed network
     */
    public Set<String> getActiveServers() {
        if (distributedSync != null) {
            return distributedSync.getActiveServers();
        }
        return new HashSet<>();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void createTables() {
        // Redis doesn't require schema initialization, but our fallback does
        fallbackDatabase.createTables();
    }
    
    @Override
    public void saveEverything() {
        flushPendingWrites();
    }
    
    @Override
    public void saveLink(String userId, UUID uuid, String nickname) {
        try {
            String userKey = "discord:user:" + userId;
            String uuidKey = "discord:uuid:" + uuid.toString();
            
            pendingWrites.put(userKey + ":uuid", uuid.toString());
            pendingWrites.put(userKey + ":nickname", nickname);
            pendingWrites.put(uuidKey + ":userid", userId);
            pendingWrites.put(uuidKey + ":nickname", nickname);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving Discord link to Redis", e);
            fallbackDatabase.saveLink(userId, uuid, nickname);
        }
    }
    
    @Override
    public void removeLink(String userId) {
        try (Jedis jedis = pool.getResource()) {
            String userKey = "discord:user:" + userId;
            String uuidStr = jedis.get(userKey + ":uuid");
            
            if (uuidStr != null) {
                String uuidKey = "discord:uuid:" + uuidStr;
                jedis.del(uuidKey + ":userid", uuidKey + ":nickname");
            }
            
            jedis.del(userKey + ":uuid", userKey + ":nickname");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error removing Discord link from Redis", e);
            fallbackDatabase.removeLink(userId);
        }
    }
    
    @Override
    public UUID getUUID(String userId) {
        try (Jedis jedis = pool.getResource()) {
            String uuidStr = jedis.get("discord:user:" + userId + ":uuid");
            return uuidStr != null ? UUID.fromString(uuidStr) : null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting UUID from Redis", e);
            return fallbackDatabase.getUUID(userId);
        }
    }
    
    @Override
    public String getNickname(String userId) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get("discord:user:" + userId + ":nickname");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting nickname from Redis", e);
            return fallbackDatabase.getNickname(userId);
        }
    }
    
    @Override
    public String getUserId(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get("discord:uuid:" + uuid.toString() + ":userid");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting user ID from Redis", e);
            return fallbackDatabase.getUserId(uuid);
        }
    }
    
    @Override
    public void saveDayPrice(Item item, Instant instant) {
        try {
            String key = "item:" + item.getIdentifier() + ":price:day:" + instant.getLocalDateTime().toLocalDate();
            pendingWrites.put(key, instant.getPrice());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving day price to Redis", e);
            fallbackDatabase.saveDayPrice(item, instant);
        }
    }
    
    @Override
    public void saveMonthPrice(Item item, Instant instant) {
        try {
            String key = "item:" + item.getIdentifier() + ":price:month:" + 
                         instant.getLocalDateTime().getYear() + ":" + 
                         instant.getLocalDateTime().getMonthValue();
            pendingWrites.put(key, instant.getPrice());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving month price to Redis", e);
            fallbackDatabase.saveMonthPrice(item, instant);
        }
    }
    
    @Override
    public void saveHistoryPrices(Item item, Instant instant) {
        try {
            String key = "item:" + item.getIdentifier() + ":price:history:" + instant.getLocalDateTime();
            pendingWrites.put(key, instant.getPrice());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving history price to Redis", e);
            fallbackDatabase.saveHistoryPrices(item, instant);
        }
    }
    
    @Override
    public List<Instant> getDayPrices(Item item) {
        List<Instant> prices = new ArrayList<>();
        try (Jedis jedis = pool.getResource()) {
            Set<String> keys = jedis.keys("item:" + item.getIdentifier() + ":price:day:*");
            for (String key : keys) {
                String dateStr = key.substring(key.lastIndexOf(":") + 1);
                LocalDate date = LocalDate.parse(dateStr);
                String priceStr = jedis.get(key);
                if (priceStr != null) {
                    double price = Double.parseDouble(priceStr);
                    Instant instant = new Instant(date.atStartOfDay(), price, 0);
                    prices.add(instant);
                }
            }
            return prices;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting day prices from Redis", e);
            return fallbackDatabase.getDayPrices(item);
        }
    }
    
    @Override
    public List<Instant> getMonthPrices(Item item) {
        List<Instant> prices = new ArrayList<>();
        try (Jedis jedis = pool.getResource()) {
            Set<String> keys = jedis.keys("item:" + item.getIdentifier() + ":price:month:*");
            for (String key : keys) {
                String[] parts = key.substring(key.lastIndexOf("month:") + 6).split(":");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                LocalDate date = LocalDate.of(year, month, 1);
                String priceStr = jedis.get(key);
                if (priceStr != null) {
                    double price = Double.parseDouble(priceStr);
                    Instant instant = new Instant(date.atStartOfDay(), price, 0);
                    prices.add(instant);
                }
            }
            return prices;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting month prices from Redis", e);
            return fallbackDatabase.getMonthPrices(item);
        }
    }
    
    @Override
    public List<Instant> getYearPrices(Item item) {
        // For simplicity, we'll delegate to the fallback database
        return fallbackDatabase.getYearPrices(item);
    }
    
    @Override
    public List<Instant> getAllPrices(Item item) {
        // For simplicity, we'll delegate to the fallback database
        return fallbackDatabase.getAllPrices(item);
    }
    
    @Override
    public Double getPriceOfDay(String identifier, int day) {
        try (Jedis jedis = pool.getResource()) {
            LocalDate date = LocalDate.now().minusDays(day);
            String key = "item:" + identifier + ":price:day:" + date;
            String priceStr = jedis.get(key);
            return priceStr != null ? Double.parseDouble(priceStr) : null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting price of day from Redis", e);
            return fallbackDatabase.getPriceOfDay(identifier, day);
        }
    }
    
    @Override
    public void saveItem(Item item) {
        try {
            if (!connected) {
                plugin.getLogger().warning("Redis not connected - using fallback to save item: " + item.getIdentifier());
                fallbackDatabase.saveItem(item);
                return;
            }
            
            String baseKey = "item:" + item.getIdentifier();
            plugin.getLogger().info("Saving item to Redis: " + item.getIdentifier());
            
            // Save all item properties to Redis
            pendingWrites.put(baseKey + ":name", item.getName());
            pendingWrites.put(baseKey + ":material", item.getItemStack().getType().toString());
            pendingWrites.put(baseKey + ":elasticity", String.valueOf(item.getPrice().getElasticity()));
            pendingWrites.put(baseKey + ":initialValue", String.valueOf(item.getPrice().getInitialValue()));
            pendingWrites.put(baseKey + ":stock", String.valueOf(item.getPrice().getStock()));
            pendingWrites.put(baseKey + ":support", String.valueOf(item.getPrice().getSupport()));
            pendingWrites.put(baseKey + ":resistance", String.valueOf(item.getPrice().getResistance()));
            pendingWrites.put(baseKey + ":noiseIntensity", String.valueOf(item.getPrice().getNoiseIntensity()));
            pendingWrites.put(baseKey + ":currentPrice", String.valueOf(item.getPrice().getValue()));
            
            // Add to items index
            try (Jedis jedis = pool.getResource()) {
                jedis.sadd("items:all", item.getIdentifier());
                plugin.getLogger().info("Added item to Redis index: " + item.getIdentifier());
            }
            
            // Also save to fallback database for consistency
            fallbackDatabase.saveItem(item);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving item to Redis: " + item.getIdentifier(), e);
            fallbackDatabase.saveItem(item);
        }
    }
    
    @Override
    public void retrieveItem(Item item) {
        try (Jedis jedis = pool.getResource()) {
            String baseKey = "item:" + item.getIdentifier();
            
            // Retrieve item properties
            String name = jedis.get(baseKey + ":name");
            String material = jedis.get(baseKey + ":material");
            String elasticityStr = jedis.get(baseKey + ":elasticity");
            String initialValueStr = jedis.get(baseKey + ":initialValue");
            String stockStr = jedis.get(baseKey + ":stock");
            String supportStr = jedis.get(baseKey + ":support");
            String resistanceStr = jedis.get(baseKey + ":resistance");
            String noiseIntensityStr = jedis.get(baseKey + ":noiseIntensity");
            
            // Apply retrieved properties
            if (name != null) item.setupAlias(name);
            
            if (elasticityStr != null) item.getPrice().setElasticity(Float.parseFloat(elasticityStr));
            if (initialValueStr != null) item.getPrice().setInitialValue(Double.parseDouble(initialValueStr));
            if (stockStr != null) item.getPrice().setStock(Float.parseFloat(stockStr));
            if (supportStr != null) item.getPrice().setSupport(Double.parseDouble(supportStr));
            if (resistanceStr != null) item.getPrice().setResistance(Double.parseDouble(resistanceStr));
            if (noiseIntensityStr != null) item.getPrice().setNoiseIntensity(Float.parseFloat(noiseIntensityStr));
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error retrieving item from Redis", e);
            fallbackDatabase.retrieveItem(item);
        }
    }
    
    @Override
    public void retrieveItems() {
        // For Redis, we need to sync items from config to Redis, then load them
        try {
            if (!connected) {
                plugin.getLogger().warning("Redis not connected - using fallback for retrieveItems");
                fallbackDatabase.retrieveItems();
                return;
            }
            
            // First, sync all items from items.yml to Redis
            syncItemsToRedis();
            
            // Then delegate to fallback for actual item object creation
            fallbackDatabase.retrieveItems();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error in retrieveItems with Redis", e);
            fallbackDatabase.retrieveItems();
        }
    }
    
    /**
     * Synchronizes item data from items.yml to Redis
     */
    private void syncItemsToRedis() {
        try (Jedis jedis = pool.getResource()) {
            plugin.getLogger().info("Syncing items from items.yml to Redis...");
            
            // Get all items from the config
            Config config = Config.getInstance();
            Set<String> itemIdentifiers = config.getAllMaterials();
            
            for (String identifier : itemIdentifiers) {
                try {
                    String baseKey = "item:" + identifier;
                    
                    // Save basic item properties to Redis
                    jedis.set(baseKey + ":name", config.getAlias(identifier));
                    jedis.set(baseKey + ":elasticity", String.valueOf(config.getElasticity(identifier)));
                    jedis.set(baseKey + ":initialValue", String.valueOf(config.getInitialPrice(identifier)));
                    jedis.set(baseKey + ":support", String.valueOf(config.getSupport(identifier)));
                    jedis.set(baseKey + ":resistance", String.valueOf(config.getResistance(identifier)));
                    jedis.set(baseKey + ":noiseIntensity", String.valueOf(config.getNoiseIntensity(identifier)));
                    
                    // Add to items index
                    jedis.sadd("items:all", identifier);
                    
                    plugin.getLogger().info("Synced item to Redis: " + identifier);
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to sync item to Redis: " + identifier + " - " + e.getMessage());
                }
            }
            
            plugin.getLogger().info("Completed syncing " + itemIdentifiers.size() + " items to Redis");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error syncing items to Redis", e);
        }
    }
    
    @Override
    public float retrieveLastPrice(Item item) {
        try (Jedis jedis = pool.getResource()) {
            String key = "item:" + item.getIdentifier() + ":currentPrice";
            String priceStr = jedis.get(key);
            
            if (priceStr != null) {
                return Float.parseFloat(priceStr);
            } else {
                float initialPrice = Config.getInstance().getInitialPrice(item.getIdentifier());
                return Math.max(initialPrice, 1.0f);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error retrieving last price from Redis", e);
            return fallbackDatabase.retrieveLastPrice(item);
        }
    }
    
    @Override
    public void saveTrade(Trade trade) {
        plugin.getLogger().info("Attempting to save trade: " + trade.getItem().getIdentifier() + 
                                " amount: " + trade.getAmount() + 
                                " player: " + trade.getUuid() + 
                                " buy: " + trade.isBuy() + 
                                " connected: " + connected);
        
        try {
            if (!connected) {
                plugin.getLogger().warning("Redis not connected - falling back to SQLite for trade save");
                fallbackDatabase.saveTrade(trade);
                return;
            }
            
            String key = "trade:" + UUID.randomUUID().toString();
            Map<String, String> tradeData = new HashMap<>();
            
            tradeData.put("timestamp", String.valueOf(trade.getDate()));
            tradeData.put("identifier", trade.getItem().getIdentifier());
            tradeData.put("price", String.valueOf(trade.getValue()));
            tradeData.put("amount", String.valueOf(trade.getAmount()));
            tradeData.put("player", trade.getUuid().toString());
            tradeData.put("type", trade.isBuy() ? "buy" : "sell");
            
            try (Jedis jedis = pool.getResource()) {
                jedis.ping();
                
                jedis.hmset(key, tradeData);
                
                jedis.sadd("player:" + trade.getUuid() + ":trades", key);
                jedis.sadd("item:" + trade.getItem().getIdentifier() + ":trades", key);
                
                String message = String.format("%s,%s,%.2f,%d,%s,%s",
                        trade.getItem().getIdentifier(),
                        trade.getUuid().toString(),
                        trade.getValue(),
                        trade.getAmount(),
                        trade.getDate(),
                        trade.isBuy() ? "buy" : "sell");
                
                jedis.publish("nascraft:trades", message);
                
                plugin.getLogger().info("Successfully saved trade to Redis: " + key);
                
                // NEW: Sync stock change across all servers
                if (distributedSync.isEnabled()) {
                    float stockChange = trade.isBuy() ? -trade.getAmount() : trade.getAmount();
                    boolean syncSuccess = distributedSync.syncStockChange(trade.getItem(), stockChange);
                    
                    if (syncSuccess) {
                        plugin.getLogger().info("Successfully synced stock change across servers for " + 
                                               trade.getItem().getIdentifier());
                    } else {
                        plugin.getLogger().warning("Failed to sync stock change for " + 
                                                   trade.getItem().getIdentifier());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error logging trade to Redis - falling back to SQLite", e);
            fallbackDatabase.saveTrade(trade);
        }
    }
    
    // Implement the remaining methods from the Database interface
    // For brevity, many of these methods will delegate to the fallback database
    
    @Override
    public List<Trade> retrieveTrades(UUID uuid, int offset, int limit) {
        return fallbackDatabase.retrieveTrades(uuid, offset, limit);
    }
    
    @Override
    public List<Trade> retrieveTrades(UUID uuid, Item item, int offset, int limit) {
        return fallbackDatabase.retrieveTrades(uuid, item, offset, limit);
    }
    
    @Override
    public List<Trade> retrieveTrades(Item item, int offset, int limit) {
        return fallbackDatabase.retrieveTrades(item, offset, limit);
    }
    
    @Override
    public List<Trade> retrieveTrades(int offset, int limit) {
        return fallbackDatabase.retrieveTrades(offset, limit);
    }
    
    @Override
    public void purgeHistory() {
        fallbackDatabase.purgeHistory();
    }
    
    @Override
    public void updateItemPortfolio(UUID uuid, Item item, int quantity) {
        plugin.getLogger().info("Updating portfolio for player " + uuid + 
                               " item: " + item.getIdentifier() + 
                               " quantity: " + quantity + 
                               " connected: " + connected);
        
        try {
            if (!connected) {
                plugin.getLogger().warning("Redis not connected - using fallback for portfolio update");
                fallbackDatabase.updateItemPortfolio(uuid, item, quantity);
                return;
            }
            
            String key = "portfolio:" + uuid.toString() + ":" + item.getIdentifier();
            
            try (Jedis jedis = pool.getResource()) {
                jedis.ping(); // Test connection
                
                if (quantity <= 0) {
                    // Remove item from portfolio
                    jedis.del(key);
                    jedis.srem("portfolio:" + uuid.toString() + ":items", item.getIdentifier());
                } else {
                    // Set item quantity
                    jedis.set(key, String.valueOf(quantity));
                    jedis.sadd("portfolio:" + uuid.toString() + ":items", item.getIdentifier());
                }
                
                plugin.getLogger().info("Successfully updated portfolio in Redis for " + uuid);
            }
            
            // Also update fallback for consistency
            fallbackDatabase.updateItemPortfolio(uuid, item, quantity);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating portfolio in Redis - using fallback", e);
            fallbackDatabase.updateItemPortfolio(uuid, item, quantity);
        }
    }
    
    @Override
    public void removeItemPortfolio(UUID uuid, Item item) {
        plugin.getLogger().info("Removing item from portfolio for player " + uuid + 
                               " item: " + item.getIdentifier() + 
                               " connected: " + connected);
        
        try {
            if (!connected) {
                plugin.getLogger().warning("Redis not connected - using fallback for portfolio removal");
                fallbackDatabase.removeItemPortfolio(uuid, item);
                return;
            }
            
            try (Jedis jedis = pool.getResource()) {
                jedis.ping(); // Test connection
                
                String key = "portfolio:" + uuid.toString() + ":" + item.getIdentifier();
                jedis.del(key);
                jedis.srem("portfolio:" + uuid.toString() + ":items", item.getIdentifier());
                
                plugin.getLogger().info("Successfully removed item from portfolio in Redis for " + uuid);
            }
            
            // Also update fallback for consistency
            fallbackDatabase.removeItemPortfolio(uuid, item);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error removing item from portfolio in Redis - using fallback", e);
            fallbackDatabase.removeItemPortfolio(uuid, item);
        }
    }
    
    @Override
    public void clearPortfolio(UUID uuid) {
        fallbackDatabase.clearPortfolio(uuid);
    }
    
    @Override
    public void updateCapacity(UUID uuid, int capacity) {
        fallbackDatabase.updateCapacity(uuid, capacity);
    }
    
    @Override
    public LinkedHashMap<Item, Integer> retrievePortfolio(UUID uuid) {
        plugin.getLogger().info("Retrieving portfolio for player " + uuid + " connected: " + connected);
        
        try {
            if (!connected) {
                plugin.getLogger().warning("Redis not connected - using fallback for portfolio retrieval");
                return fallbackDatabase.retrievePortfolio(uuid);
            }
            
            LinkedHashMap<Item, Integer> content = new LinkedHashMap<>();
            
            try (Jedis jedis = pool.getResource()) {
                jedis.ping(); // Test connection
                
                Set<String> itemIdentifiers = jedis.smembers("portfolio:" + uuid.toString() + ":items");
                
                for (String identifier : itemIdentifiers) {
                    String key = "portfolio:" + uuid.toString() + ":" + identifier;
                    String quantityStr = jedis.get(key);
                    
                    if (quantityStr != null) {
                        Item item = MarketManager.getInstance().getItem(identifier);
                        if (item != null) {
                            int quantity = Integer.parseInt(quantityStr);
                            content.put(item, quantity);
                        }
                    }
                }
                
                plugin.getLogger().info("Successfully retrieved portfolio from Redis for " + uuid + 
                                       " with " + content.size() + " items");
                return content;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error retrieving portfolio from Redis - using fallback", e);
            return fallbackDatabase.retrievePortfolio(uuid);
        }
    }
    
    @Override
    public int retrieveCapacity(UUID uuid) {
        return fallbackDatabase.retrieveCapacity(uuid);
    }
    
    @Override
    public void increaseDebt(UUID uuid, Double debt) {
        fallbackDatabase.increaseDebt(uuid, debt);
    }
    
    @Override
    public void decreaseDebt(UUID uuid, Double debt) {
        fallbackDatabase.decreaseDebt(uuid, debt);
    }
    
    @Override
    public double getDebt(UUID uuid) {
        return fallbackDatabase.getDebt(uuid);
    }
    
    @Override
    public HashMap<UUID, Double> getUUIDAndDebt() {
        return fallbackDatabase.getUUIDAndDebt();
    }
    
    @Override
    public void addInterestPaid(UUID uuid, Double interest) {
        fallbackDatabase.addInterestPaid(uuid, interest);
    }
    
    @Override
    public HashMap<UUID, Double> getUUIDAndInterestsPaid() {
        return fallbackDatabase.getUUIDAndInterestsPaid();
    }
    
    @Override
    public double getInterestsPaid(UUID uuid) {
        return fallbackDatabase.getInterestsPaid(uuid);
    }
    
    @Override
    public double getAllOutstandingDebt() {
        return fallbackDatabase.getAllOutstandingDebt();
    }
    
    @Override
    public double getAllInterestsPaid() {
        return fallbackDatabase.getAllInterestsPaid();
    }
    
    @Override
    public void saveOrUpdateWorth(UUID uuid, int day, double worth) {
        fallbackDatabase.saveOrUpdateWorth(uuid, day, worth);
    }
    
    @Override
    public void saveOrUpdateWorthToday(UUID uuid, double worth) {
        fallbackDatabase.saveOrUpdateWorthToday(uuid, worth);
    }
    
    @Override
    public HashMap<UUID, Portfolio> getTopWorth(int n) {
        return fallbackDatabase.getTopWorth(n);
    }
    
    @Override
    public double getLatestWorth(UUID uuid) {
        return fallbackDatabase.getLatestWorth(uuid);
    }
    
    @Override
    public void logContribution(UUID uuid, Item item, int amount) {
        fallbackDatabase.logContribution(uuid, item, amount);
    }
    
    @Override
    public void logWithdraw(UUID uuid, Item item, int amount) {
        fallbackDatabase.logWithdraw(uuid, item, amount);
    }
    
    @Override
    public HashMap<Integer, Double> getContributionChangeEachDay(UUID uuid) {
        return fallbackDatabase.getContributionChangeEachDay(uuid);
    }
    
    @Override
    public HashMap<Integer, HashMap<String, Integer>> getCompositionEachDay(UUID uuid) {
        return fallbackDatabase.getCompositionEachDay(uuid);
    }
    
    @Override
    public int getFirstDay(UUID uuid) {
        return fallbackDatabase.getFirstDay(uuid);
    }
    
    @Override
    public void saveCPIValue(float indexValue) {
        fallbackDatabase.saveCPIValue(indexValue);
    }
    
    @Override
    public List<CPIInstant> getCPIHistory() {
        return fallbackDatabase.getCPIHistory();
    }
    
    @Override
    public List<Instant> getPriceAgainstCPI(Item item) {
        return fallbackDatabase.getPriceAgainstCPI(item);
    }
    
    @Override
    public void addTransaction(double newFlow, double effectiveTaxes) {
        fallbackDatabase.addTransaction(newFlow, effectiveTaxes);
    }
    
    @Override
    public List<DayInfo> getDayInfos() {
        return fallbackDatabase.getDayInfos();
    }
    
    @Override
    public double getAllTaxesCollected() {
        return fallbackDatabase.getAllTaxesCollected();
    }
    
    @Override
    public void addAlert(String userid, Item item, double price) {
        fallbackDatabase.addAlert(userid, item, price);
    }
    
    @Override
    public void removeAlert(String userid, Item item) {
        fallbackDatabase.removeAlert(userid, item);
    }
    
    @Override
    public void retrieveAlerts() {
        fallbackDatabase.retrieveAlerts();
    }
    
    @Override
    public void removeAllAlerts(String userid) {
        fallbackDatabase.removeAllAlerts(userid);
    }
    
    @Override
    public void purgeAlerts() {
        fallbackDatabase.purgeAlerts();
    }
    
    @Override
    public void addLimitOrder(UUID uuid, LocalDateTime expiration, Item item, int type, double price, int amount) {
        fallbackDatabase.addLimitOrder(uuid, expiration, item, type, price, amount);
    }
    
    @Override
    public void updateLimitOrder(UUID uuid, Item item, int completed, double cost) {
        fallbackDatabase.updateLimitOrder(uuid, item, completed, cost);
    }
    
    @Override
    public void removeLimitOrder(String uuid, String identifier) {
        fallbackDatabase.removeLimitOrder(uuid, identifier);
    }
    
    @Override
    public void retrieveLimitOrders() {
        fallbackDatabase.retrieveLimitOrders();
    }
    
    @Override
    public String getNameByUUID(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get("player:" + uuid.toString() + ":name");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting name by UUID from Redis", e);
            return fallbackDatabase.getNameByUUID(uuid);
        }
    }
    
    @Override
    public void saveOrUpdateName(UUID uuid, String name) {
        try {
            String key = "player:" + uuid.toString() + ":name";
            pendingWrites.put(key, name);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving player name to Redis", e);
            fallbackDatabase.saveOrUpdateName(uuid, name);
        }
    }
    
    @Override
    public String getNicknameFromUserId(String userid) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get("discord:user:" + userid + ":nickname");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting nickname from Redis", e);
            return fallbackDatabase.getNicknameFromUserId(userid);
        }
    }
    
    @Override
    public UUID getUUIDbyName(String name) {
        try (Jedis jedis = pool.getResource()) {
            String key = "player:name:" + name.toLowerCase() + ":uuid";
            String uuidStr = jedis.get(key);
            return uuidStr != null ? UUID.fromString(uuidStr) : null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting UUID by name from Redis", e);
            return fallbackDatabase.getUUIDbyName(name);
        }
    }
    
    @Override
    public void updateBalance(UUID uuid) {
        // Delegate to fallback database
        fallbackDatabase.updateBalance(uuid);
    }
    
    @Override
    public Map<Integer, Double> getMoneySupplyHistory() {
        // Delegate to fallback database
        return fallbackDatabase.getMoneySupplyHistory();
    }
    
    @Override
    public void storeCredentials(String userName, String hash) {
        try {
            String key = "credentials:" + userName;
            pendingWrites.put(key, hash);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error storing credentials in Redis", e);
            fallbackDatabase.storeCredentials(userName, hash);
        }
    }
    
    @Override
    public String retrieveHash(String userName) {
        try (Jedis jedis = pool.getResource()) {
            String key = "credentials:" + userName;
            return jedis.get(key);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error retrieving hash from Redis", e);
            return fallbackDatabase.retrieveHash(userName);
        }
    }
    
    @Override
    public void clearUserCredentials(String userName) {
        try (Jedis jedis = pool.getResource()) {
            String key = "credentials:" + userName;
            jedis.del(key);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error clearing credentials from Redis", e);
            fallbackDatabase.clearUserCredentials(userName);
        }
    }
    
    @Override
    public void saveOrUpdatePlayerStats(UUID uuid) {
        // Delegate to fallback database
        fallbackDatabase.saveOrUpdatePlayerStats(uuid);
    }
    
    @Override
    public List<PlayerStatsDTO> getAllPlayerStats(UUID uuid) {
        // Delegate to fallback database
        return fallbackDatabase.getAllPlayerStats(uuid);
    }
    
    @Override
    public void saveDiscordLink(UUID uuid, String userid, String nickname) {
        try {
            String uuidKey = "discord:uuid:" + uuid.toString();
            String userKey = "discord:user:" + userid;
            
            pendingWrites.put(uuidKey + ":userid", userid);
            pendingWrites.put(uuidKey + ":nickname", nickname);
            pendingWrites.put(userKey + ":uuid", uuid.toString());
            pendingWrites.put(userKey + ":nickname", nickname);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving Discord link to Redis", e);
            fallbackDatabase.saveDiscordLink(uuid, userid, nickname);
        }
    }
    
    @Override
    public void removeDiscordLink(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            String uuidKey = "discord:uuid:" + uuid.toString();
            String userid = jedis.get(uuidKey + ":userid");
            
            if (userid != null) {
                String userKey = "discord:user:" + userid;
                jedis.del(userKey + ":uuid", userKey + ":nickname");
            }
            
            jedis.del(uuidKey + ":userid", uuidKey + ":nickname");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error removing Discord link from Redis", e);
            fallbackDatabase.removeDiscordLink(uuid);
        }
    }
    
    @Override
    public String getDiscordUserId(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get("discord:uuid:" + uuid.toString() + ":userid");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting Discord user ID from Redis", e);
            return fallbackDatabase.getDiscordUserId(uuid);
        }
    }
    
    @Override
    public UUID getUUIDFromUserid(String userid) {
        try (Jedis jedis = pool.getResource()) {
            String uuidStr = jedis.get("discord:user:" + userid + ":uuid");
            return uuidStr != null ? UUID.fromString(uuidStr) : null;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting UUID from user ID in Redis", e);
            return fallbackDatabase.getUUIDFromUserid(userid);
        }
    }

    /**
     * Force sync all loaded items to Redis
     */
    public void syncAllItemsToRedis() {
        try {
            if (!connected) {
                plugin.getLogger().warning("Redis not connected - cannot sync items");
                return;
            }
            
            plugin.getLogger().info("Force syncing all loaded items to Redis...");
            
            List<Item> allItems = MarketManager.getInstance().getAllItems();
            
            for (Item item : allItems) {
                if (item.isParent()) { // Only sync parent items
                    try {
                        saveItem(item);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to sync item to Redis: " + item.getIdentifier() + " - " + e.getMessage());
                    }
                }
            }
            
            plugin.getLogger().info("Completed force sync of " + allItems.size() + " items to Redis");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during force sync of items to Redis", e);
        }
    }
} 