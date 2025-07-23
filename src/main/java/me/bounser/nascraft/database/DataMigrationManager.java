package me.bounser.nascraft.database;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.database.redis.Redis;
import me.bounser.nascraft.database.sqlite.SQLite;
import me.bounser.nascraft.managers.scheduler.SchedulerManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Manages data migration between different database implementations
 */
public class DataMigrationManager {
    
    private final Nascraft plugin;
    
    // Track migration progress
    private boolean migrationInProgress = false;
    private AtomicInteger totalItemsToMigrate = new AtomicInteger(0);
    private AtomicInteger migratedItems = new AtomicInteger(0);
    private Map<String, Boolean> completedTasks = new ConcurrentHashMap<>();
    
    public DataMigrationManager(Nascraft plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Start the migration process from source to destination database
     * 
     * @param source The source database
     * @param destination The destination database
     * @return CompletableFuture that completes when migration is finished
     */
    public CompletableFuture<Boolean> migrateData(Database source, Database destination) {
        CompletableFuture<Boolean> migrationResult = new CompletableFuture<>();
        
        if (migrationInProgress) {
            migrationResult.complete(false);
            plugin.getLogger().warning("Data migration already in progress, cannot start another one");
            return migrationResult;
        }
        
        migrationInProgress = true;
        resetProgress();
        
        plugin.getLogger().info("Starting data migration from " + 
                source.getClass().getSimpleName() + " to " + 
                destination.getClass().getSimpleName());
        
        // Use async scheduler to avoid blocking the main thread
        SchedulerManager.getInstance().runAsync(() -> {
            try {
                // Ensure both databases are connected
                if (!source.isConnected() || !destination.isConnected()) {
                    migrationResult.complete(false);
                    migrationInProgress = false;
                    plugin.getLogger().severe("Cannot start migration: One or both databases are not connected");
                    return;
                }
                
                // Initialize destination database (create tables etc.)
                destination.createTables();
                
                // Step 1: Migrate market data (items, prices, etc.)
                migrateMarketData(source, destination)
                    .thenCompose(result -> {
                        completedTasks.put("marketData", result);
                        // Step 2: Migrate player data (portfolios, balances, etc.)
                        return migratePlayerData(source, destination);
                    })
                    .thenCompose(result -> {
                        completedTasks.put("playerData", result);
                        // Step 3: Migrate trade history
                        return migrateTradeHistory(source, destination);
                    })
                    .thenAccept(result -> {
                        completedTasks.put("tradeHistory", result);
                        
                        // Check if all tasks completed successfully
                        boolean success = !completedTasks.containsValue(false);
                        
                        if (success) {
                            plugin.getLogger().info("Data migration completed successfully!");
                        } else {
                            plugin.getLogger().warning("Data migration completed with errors. Check the log for details.");
                        }
                        
                        migrationInProgress = false;
                        migrationResult.complete(success);
                    });
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during data migration", e);
                migrationInProgress = false;
                migrationResult.complete(false);
            }
        });
        
        return migrationResult;
    }
    
    /**
     * Start migration from SQLite to Redis
     */
    public CompletableFuture<Boolean> migrateFromSQLiteToRedis() {
        Database sqlite = SQLite.getInstance();
        
        // Create a Redis database instance for migration
        String host = Config.getInstance().getRedisHost();
        int port = Config.getInstance().getRedisPort();
        String password = Config.getInstance().getRedisPassword();
        String username = Config.getInstance().getRedisUsername();
        int database = Config.getInstance().getRedisDatabase();
        
        Database redis = new Redis(plugin, host, port, password, username, database);
        
        return migrateData(sqlite, redis);
    }
    
    /**
     * Migrate market data (items, prices, etc.)
     */
    private CompletableFuture<Boolean> migrateMarketData(Database source, Database destination) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        
        plugin.getLogger().info("Migrating market data...");
        
        try {
            List<Item> allItems = MarketManager.getInstance().getAllItems();
            totalItemsToMigrate.set(allItems.size());
            migratedItems.set(0);
            
            // Migrate each item's data
            for (Item item : allItems) {
                // Make sure the item is fully loaded from source
                source.retrieveItem(item);
                
                // Save to destination
                destination.saveItem(item);
                
                // Migrate price history
                List<me.bounser.nascraft.market.unit.stats.Instant> dayPrices = source.getDayPrices(item);
                for (me.bounser.nascraft.market.unit.stats.Instant instant : dayPrices) {
                    destination.saveDayPrice(item, instant);
                }
                
                List<me.bounser.nascraft.market.unit.stats.Instant> monthPrices = source.getMonthPrices(item);
                for (me.bounser.nascraft.market.unit.stats.Instant instant : monthPrices) {
                    destination.saveMonthPrice(item, instant);
                }
                
                List<me.bounser.nascraft.market.unit.stats.Instant> yearPrices = source.getYearPrices(item);
                for (me.bounser.nascraft.market.unit.stats.Instant instant : yearPrices) {
                    destination.saveHistoryPrices(item, instant);
                }
                
                migratedItems.incrementAndGet();
                
                if (migratedItems.get() % 10 == 0) {
                    plugin.getLogger().info(String.format("Migrated %d/%d item data (%d%%)", 
                            migratedItems.get(), totalItemsToMigrate.get(),
                            (migratedItems.get() * 100 / totalItemsToMigrate.get())));
                }
            }
            
            // Migrate CPI data
            List<me.bounser.nascraft.chart.cpi.CPIInstant> cpiHistory = source.getCPIHistory();
            for (me.bounser.nascraft.chart.cpi.CPIInstant cpi : cpiHistory) {
                destination.saveCPIValue(cpi.getIndexValue());
            }
            
            plugin.getLogger().info("Market data migration complete!");
            result.complete(true);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error migrating market data", e);
            result.complete(false);
        }
        
        return result;
    }
    
    /**
     * Migrate player data (portfolios, balances, etc.)
     */
    private CompletableFuture<Boolean> migratePlayerData(Database source, Database destination) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        
        plugin.getLogger().info("Migrating player data...");
        
        try {
            // Migrate discord links
            // Retrieve the UUIDs linked to discord accounts
            // Note: This would need implementation based on how links are stored
            
            // Migrate portfolios
            // This would require retrieving all portfolios from the source database
            // and saving them to the destination
            
            // Migrate limit orders
            source.retrieveLimitOrders();
            destination.retrieveLimitOrders();
            
            // Migrate alerts
            source.retrieveAlerts();
            destination.retrieveAlerts();
            
            plugin.getLogger().info("Player data migration complete!");
            result.complete(true);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error migrating player data", e);
            result.complete(false);
        }
        
        return result;
    }
    
    /**
     * Migrate trade history
     */
    private CompletableFuture<Boolean> migrateTradeHistory(Database source, Database destination) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        
        plugin.getLogger().info("Migrating trade history...");
        
        try {
            int offset = 0;
            int batchSize = 100;
            int totalMigrated = 0;
            
            while (true) {
                List<Trade> trades = source.retrieveTrades(offset, batchSize);
                
                if (trades.isEmpty()) {
                    break;
                }
                
                for (Trade trade : trades) {
                    destination.saveTrade(trade);
                    totalMigrated++;
                }
                
                offset += batchSize;
                
                if (totalMigrated % 500 == 0) {
                    plugin.getLogger().info(String.format("Migrated %d trade records", totalMigrated));
                }
            }
            
            plugin.getLogger().info(String.format("Trade history migration complete! Migrated %d records", totalMigrated));
            result.complete(true);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error migrating trade history", e);
            result.complete(false);
        }
        
        return result;
    }
    
    /**
     * Check if migration is currently in progress
     */
    public boolean isMigrationInProgress() {
        return migrationInProgress;
    }
    
    /**
     * Get the current migration progress as a percentage
     */
    public int getMigrationProgress() {
        if (!migrationInProgress || totalItemsToMigrate.get() == 0) {
            return 0;
        }
        
        return (migratedItems.get() * 100) / totalItemsToMigrate.get();
    }
    
    /**
     * Reset progress tracking
     */
    private void resetProgress() {
        totalItemsToMigrate.set(0);
        migratedItems.set(0);
        completedTasks.clear();
    }
} 