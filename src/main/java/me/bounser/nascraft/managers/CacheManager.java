package me.bounser.nascraft.managers;

import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.portfolio.Portfolio;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Thread-safe cache manager for frequently accessed data.
 * Uses read-write locks for efficient concurrent access.
 */
public class CacheManager {
    
    private static CacheManager instance;
    
    // Item price cache with ID-to-price mapping
    private final ConcurrentHashMap<String, Float> itemPriceCache = new ConcurrentHashMap<>();
    
    // Portfolio cache with UUID-to-Portfolio mapping
    private final ConcurrentHashMap<UUID, CachedValue<Portfolio>> portfolioCache = new ConcurrentHashMap<>();
    
    // Market data cache
    private final ConcurrentHashMap<String, CachedValue<Object>> marketDataCache = new ConcurrentHashMap<>();
    
    // Locks for bulk operations
    private final ReadWriteLock itemPriceLock = new ReentrantReadWriteLock();
    private final ReadWriteLock portfolioLock = new ReentrantReadWriteLock();
    private final ReadWriteLock marketDataLock = new ReentrantReadWriteLock();
    
    // Cache TTLs in milliseconds
    private static final long PRICE_CACHE_TTL = Duration.ofMinutes(1).toMillis();
    private static final long PORTFOLIO_CACHE_TTL = Duration.ofSeconds(30).toMillis();
    private static final long MARKET_DATA_CACHE_TTL = Duration.ofMinutes(5).toMillis();
    
    public static CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }
    
    /**
     * Get an item's price from cache or load it using the provided function
     * 
     * @param item The item to get the price for
     * @param loader Function to load the price if not in cache
     * @return The item's price
     */
    public float getItemPrice(Item item, Function<Item, Float> loader) {
        String identifier = item.getIdentifier();
        Float cachedPrice = itemPriceCache.get(identifier);
        
        if (cachedPrice != null) {
            return cachedPrice;
        }
        
        // Cache miss, load from source
        float price = loader.apply(item);
        itemPriceCache.put(identifier, price);
        return price;
    }
    
    /**
     * Update an item's price in the cache
     * 
     * @param identifier The item identifier
     * @param price The new price
     */
    public void updateItemPrice(String identifier, float price) {
        itemPriceCache.put(identifier, price);
    }
    
    /**
     * Update multiple item prices atomically
     * 
     * @param prices Map of item identifiers to prices
     */
    public void bulkUpdateItemPrices(Map<String, Float> prices) {
        itemPriceLock.writeLock().lock();
        try {
            itemPriceCache.putAll(prices);
        } finally {
            itemPriceLock.writeLock().unlock();
        }
    }
    
    /**
     * Clear the item price cache
     */
    public void clearItemPriceCache() {
        itemPriceLock.writeLock().lock();
        try {
            itemPriceCache.clear();
        } finally {
            itemPriceLock.writeLock().unlock();
        }
    }
    
    /**
     * Get a portfolio from cache or load it using the provided function
     * 
     * @param uuid The player UUID
     * @param loader Function to load the portfolio if not in cache
     * @return The player's portfolio
     */
    public Portfolio getPortfolio(UUID uuid, Function<UUID, Portfolio> loader) {
        CachedValue<Portfolio> cached = portfolioCache.get(uuid);
        
        if (cached != null && !cached.isExpired()) {
            return cached.getValue();
        }
        
        // Cache miss or expired, load from source
        Portfolio portfolio = loader.apply(uuid);
        portfolioCache.put(uuid, new CachedValue<>(portfolio, PORTFOLIO_CACHE_TTL));
        return portfolio;
    }
    
    /**
     * Update a portfolio in the cache
     * 
     * @param uuid The player UUID
     * @param portfolio The portfolio
     */
    public void updatePortfolio(UUID uuid, Portfolio portfolio) {
        portfolioCache.put(uuid, new CachedValue<>(portfolio, PORTFOLIO_CACHE_TTL));
    }
    
    /**
     * Remove a portfolio from the cache
     * 
     * @param uuid The player UUID
     */
    public void invalidatePortfolio(UUID uuid) {
        portfolioCache.remove(uuid);
    }
    
    /**
     * Get market data from cache or load it
     * 
     * @param key The market data key
     * @param loader Function to load the data if not in cache
     * @return The market data
     */
    public <T> T getMarketData(String key, Function<String, T> loader, Class<T> type) {
        CachedValue<Object> cached = marketDataCache.get(key);
        
        if (cached != null && !cached.isExpired()) {
            return type.cast(cached.getValue());
        }
        
        // Cache miss or expired, load from source
        T data = loader.apply(key);
        marketDataCache.put(key, new CachedValue<>(data, MARKET_DATA_CACHE_TTL));
        return data;
    }
    
    /**
     * Update market data in the cache
     * 
     * @param key The market data key
     * @param data The market data
     * @param ttlOverride Optional TTL override in milliseconds
     */
    public void updateMarketData(String key, Object data, Long ttlOverride) {
        long ttl = ttlOverride != null ? ttlOverride : MARKET_DATA_CACHE_TTL;
        marketDataCache.put(key, new CachedValue<>(data, ttl));
    }
    
    /**
     * Update market data in the cache with default TTL
     * 
     * @param key The market data key
     * @param data The market data
     */
    public void updateMarketData(String key, Object data) {
        updateMarketData(key, data, null);
    }
    
    /**
     * Remove market data from the cache
     * 
     * @param key The market data key
     */
    public void invalidateMarketData(String key) {
        marketDataCache.remove(key);
    }
    
    /**
     * Remove all market data matching a prefix
     * 
     * @param prefix The key prefix
     */
    public void invalidateMarketDataByPrefix(String prefix) {
        marketDataLock.writeLock().lock();
        try {
            marketDataCache.keySet().removeIf(key -> key.startsWith(prefix));
        } finally {
            marketDataLock.writeLock().unlock();
        }
    }
    
    /**
     * Get statistics about the cache
     * 
     * @return Map of cache statistics
     */
    public Map<String, Integer> getCacheStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("itemPriceCount", itemPriceCache.size());
        stats.put("portfolioCount", portfolioCache.size());
        stats.put("marketDataCount", marketDataCache.size());
        return stats;
    }
    
    /**
     * Helper class to store cached values with expiration
     */
    private static class CachedValue<T> {
        private final T value;
        private final long expiryTime;
        
        public CachedValue(T value, long ttl) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttl;
        }
        
        public T getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
} 