package me.bounser.nascraft.distributed;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class DistributedMarketSync {

    private static final String CHANNEL_PRICE_UPDATE = "nascraft:price:updates";
    private static final String CHANNEL_TRANSACTION = "nascraft:transactions";
    private static final String CHANNEL_BATCH_UPDATE = "nascraft:batch:updates";
    private static final String KEY_MASTER = "nascraft:master";
    private static final String KEY_ITEM_STOCK = "nascraft:item:%s:stock";
    private static final String KEY_SERVER_HEARTBEAT = "nascraft:server:%s:heartbeat";
    private static final int BATCH_SIZE = 50;
    private static final int BATCH_INTERVAL_MS = 100;

    private final Nascraft plugin;
    private final JedisPool jedisPool;
    private final String serverId;
    private final boolean isMaster;
    private final String masterServerId;
    private final ConcurrentHashMap<String, ReentrantLock> itemLocks;
    private final ConcurrentHashMap<String, Float> pendingUpdates;
    private final ConcurrentLinkedQueue<PendingTransaction> transactionQueue;
    private final ScheduledExecutorService scheduler;
    private volatile boolean enabled = false;
    private Thread priceListenerThread;
    private Thread transactionListenerThread;
    private ScheduledFuture<?> heartbeatTask;
    private ScheduledFuture<?> masterCheckTask;
    private ScheduledFuture<?> batchProcessTask;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private static final int MAX_RECONNECT_ATTEMPTS = 10;

    private static class PendingTransaction {
        final String itemId;
        final float stockChange;
        final long timestamp;
        
        PendingTransaction(String itemId, float stockChange) {
            this.itemId = itemId;
            this.stockChange = stockChange;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public DistributedMarketSync(Nascraft plugin, JedisPool jedisPool) {
        this.plugin = plugin;
        this.jedisPool = jedisPool;
        this.serverId = Config.getInstance().getServerId();
        this.isMaster = Config.getInstance().isMasterServer();
        this.masterServerId = Config.getInstance().getMasterServerId();
        this.itemLocks = new ConcurrentHashMap<>();
        this.pendingUpdates = new ConcurrentHashMap<>();
        this.transactionQueue = new ConcurrentLinkedQueue<>();
        this.scheduler = Executors.newScheduledThreadPool(4);

        plugin.getLogger().info("DistributedMarketSync initialized - Server: " + serverId + 
                               ", Role: " + (isMaster ? "MASTER" : "SLAVE"));
    }

    public void enable() {
        if (enabled) return;

        try {
            if (!testConnection()) {
                plugin.getLogger().warning("Redis connection failed - distributed sync disabled");
                return;
            }

            startHeartbeat();

            if (isMaster) {
                registerAsMaster();
                startPriceListener();
                startTransactionListener();
            } else {
                startPriceListener();
                startMasterHealthCheck();
            }

            if (!isMaster) {
                loadMarketStateFromMaster();
            }

            enabled = true;
            plugin.getLogger().info("Distributed sync enabled - Role: " + (isMaster ? "MASTER" : "SLAVE"));

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to enable distributed sync", e);
        }
    }

    public void disable() {
        if (!enabled) return;
        enabled = false;

        if (heartbeatTask != null) heartbeatTask.cancel(false);
        if (masterCheckTask != null) masterCheckTask.cancel(false);
        
        scheduler.shutdown();

        if (priceListenerThread != null) priceListenerThread.interrupt();
        if (transactionListenerThread != null) transactionListenerThread.interrupt();

        removeHeartbeat();

        if (isMaster) {
            unregisterAsMaster();
        }

        plugin.getLogger().info("Distributed sync disabled");
    }

    private boolean testConnection() {
        try (Jedis jedis = jedisPool.getResource()) {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Redis connection test failed", e);
            return false;
        }
    }

    private void startHeartbeat() {
        int interval = Config.getInstance().getHeartbeatInterval();
        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                String key = String.format(KEY_SERVER_HEARTBEAT, serverId);
                jedis.setex(key, interval * 3, String.valueOf(System.currentTimeMillis()));
            } catch (Exception e) {
                if (Config.getInstance().getDebugLogging()) {
                    plugin.getLogger().log(Level.WARNING, "Heartbeat failed", e);
                }
            }
        }, 0, interval, TimeUnit.SECONDS);
    }

    private void removeHeartbeat() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(String.format(KEY_SERVER_HEARTBEAT, serverId));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to remove heartbeat", e);
        }
    }

    private void registerAsMaster() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(KEY_MASTER, serverId);
            plugin.getLogger().info("Registered as master server");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to register as master", e);
        }
    }

    private void unregisterAsMaster() {
        try (Jedis jedis = jedisPool.getResource()) {
            String current = jedis.get(KEY_MASTER);
            if (serverId.equals(current)) {
                jedis.del(KEY_MASTER);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to unregister as master", e);
        }
    }

    private void startMasterHealthCheck() {
        masterCheckTask = scheduler.scheduleAtFixedRate(() -> {
            if (!isMasterAlive()) {
                plugin.getLogger().warning("Master server is not responding");
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private boolean isMasterAlive() {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = String.format(KEY_SERVER_HEARTBEAT, masterServerId);
            String timestamp = jedis.get(key);
            if (timestamp == null) return false;
            long lastBeat = Long.parseLong(timestamp);
            return (System.currentTimeMillis() - lastBeat) < 60000;
        } catch (Exception e) {
            return false;
        }
    }

    private void startPriceListener() {
        priceListenerThread = new Thread(() -> {
            while (enabled) {
                try (Jedis jedis = jedisPool.getResource()) {
                    reconnectAttempts.set(0);
                    jedis.subscribe(new PriceUpdateListener(), CHANNEL_PRICE_UPDATE);
                } catch (Exception e) {
                    if (enabled) {
                        int attempts = reconnectAttempts.incrementAndGet();
                        if (attempts <= MAX_RECONNECT_ATTEMPTS) {
                            long backoff = Math.min(5000 * (1L << attempts), 60000);
                            plugin.getLogger().warning("Price listener disconnected, reconnecting in " + backoff + "ms");
                            try {
                                Thread.sleep(backoff);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        } else {
                            plugin.getLogger().severe("Max reconnect attempts reached for price listener");
                            break;
                        }
                    }
                }
            }
        }, "Nascraft-PriceListener");
        priceListenerThread.setDaemon(true);
        priceListenerThread.start();
    }

    private void startTransactionListener() {
        if (!isMaster) return;

        transactionListenerThread = new Thread(() -> {
            while (enabled) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(new TransactionListener(), CHANNEL_TRANSACTION);
                } catch (Exception e) {
                    if (enabled) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }, "Nascraft-TransactionListener");
        transactionListenerThread.setDaemon(true);
        transactionListenerThread.start();
    }

    private void loadMarketStateFromMaster() {
        for (Item item : MarketManager.getInstance().getAllItems()) {
            try (Jedis jedis = jedisPool.getResource()) {
                String key = String.format(KEY_ITEM_STOCK, item.getIdentifier());
                String stockStr = jedis.get(key);
                if (stockStr != null) {
                    float stock = Float.parseFloat(stockStr);
                    item.getPrice().setStock(stock);
                }
            } catch (Exception e) {
                if (Config.getInstance().getDebugLogging()) {
                    plugin.getLogger().warning("Failed to load state for: " + item.getIdentifier());
                }
            }
        }
    }

    public boolean syncStockChange(Item item, float stockChange) {
        if (!enabled) return false;

        if (isMaster) {
            return executeStockUpdate(item, stockChange);
        } else {
            return sendTransactionToMaster(item, stockChange);
        }
    }

    private boolean executeStockUpdate(Item item, float stockChange) {
        String identifier = item.getIdentifier();
        ReentrantLock lock = itemLocks.computeIfAbsent(identifier, k -> new ReentrantLock());

        if (!lock.tryLock()) {
            return false;
        }

        try {
            String stockKey = String.format(KEY_ITEM_STOCK, identifier);
            int maxRetries = Config.getInstance().getMaxRetries();

            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.watch(stockKey);

                    String currentStr = jedis.get(stockKey);
                    float current = currentStr != null ? Float.parseFloat(currentStr) : 0f;
                    float newStock = current + stockChange;

                    Transaction tx = jedis.multi();
                    tx.set(stockKey, String.valueOf(newStock));
                    List<Object> result = tx.exec();

                    if (result != null) {
                        item.getPrice().setStock(newStock);
                        publishPriceUpdate(identifier, newStock);
                        return true;
                    }

                    Thread.sleep(Config.getInstance().getRetryBackoff() * (1L << attempt));

                } catch (Exception e) {
                    if (attempt == maxRetries - 1) {
                        plugin.getLogger().log(Level.WARNING, "Stock update failed for " + identifier, e);
                    }
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private boolean sendTransactionToMaster(Item item, float stockChange) {
        try (Jedis jedis = jedisPool.getResource()) {
            String message = String.format("%s|%s|%.4f|%d",
                    serverId, item.getIdentifier(), stockChange, System.currentTimeMillis());
            jedis.publish(CHANNEL_TRANSACTION, message);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to send transaction to master", e);
            return false;
        }
    }

    private void publishPriceUpdate(String itemId, float newStock) {
        try (Jedis jedis = jedisPool.getResource()) {
            String message = String.format("%s|%.4f|%d", itemId, newStock, System.currentTimeMillis());
            jedis.publish(CHANNEL_PRICE_UPDATE, message);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to publish price update", e);
        }
    }

    public Set<String> getActiveServers() {
        Set<String> servers = new HashSet<>();
        try (Jedis jedis = jedisPool.getResource()) {
            ScanParams params = new ScanParams().match("nascraft:server:*:heartbeat").count(100);
            String cursor = "0";
            long now = System.currentTimeMillis();

            do {
                ScanResult<String> result = jedis.scan(cursor, params);
                for (String key : result.getResult()) {
                    String ts = jedis.get(key);
                    if (ts != null && (now - Long.parseLong(ts)) < 60000) {
                        String id = key.replace("nascraft:server:", "").replace(":heartbeat", "");
                        servers.add(id);
                    }
                }
                cursor = result.getCursor();
            } while (!"0".equals(cursor));

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get active servers", e);
        }
        return servers;
    }

    public boolean shouldApplyNoise() {
        return isMaster;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getServerId() {
        return serverId;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public String getMasterServerId() {
        return masterServerId;
    }

    private class PriceUpdateListener extends JedisPubSub {
        @Override
        public void onMessage(String channel, String message) {
            if (!enabled || isMaster) return;

            try {
                String[] parts = message.split("\\|");
                if (parts.length < 3) return;

                String itemId = parts[0];
                float newStock = Float.parseFloat(parts[1]);

                Item item = MarketManager.getInstance().getItem(itemId);
                if (item != null) {
                    item.getPrice().setStock(newStock);
                }
            } catch (Exception e) {
                if (Config.getInstance().getDebugLogging()) {
                    plugin.getLogger().log(Level.WARNING, "Error processing price update", e);
                }
            }
        }
    }

    private class TransactionListener extends JedisPubSub {
        @Override
        public void onMessage(String channel, String message) {
            if (!enabled || !isMaster) return;

            try {
                String[] parts = message.split("\\|");
                if (parts.length < 4) return;

                String sourceServer = parts[0];
                String itemId = parts[1];
                float stockChange = Float.parseFloat(parts[2]);

                if (serverId.equals(sourceServer)) return;

                Item item = MarketManager.getInstance().getItem(itemId);
                if (item != null) {
                    executeStockUpdate(item, stockChange);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error processing transaction", e);
            }
        }
    }
}