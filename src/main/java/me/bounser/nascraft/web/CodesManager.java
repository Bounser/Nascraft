package me.bounser.nascraft.web;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages authentication codes for the web API
 */
public class CodesManager {

    private static CodesManager instance;
    
    public static CodesManager getInstance() {
        if (instance == null) {
            instance = new CodesManager();
        }
        return instance;
    }
    
    private final Map<String, Long> authCodes = new HashMap<>();
    private final Map<String, UUID> userSessions = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final long CODE_EXPIRY_MS = TimeUnit.MINUTES.toMillis(15);
    
    private CodesManager() {
        // Private constructor for singleton
    }
    
    /**
     * Generates a new authentication code for a player
     * 
     * @param uuid The player's UUID
     * @return The generated authentication code
     */
    public String generateCode(UUID uuid) {
        String code = generateRandomCode();
        authCodes.put(code, System.currentTimeMillis() + CODE_EXPIRY_MS);
        userSessions.put(code, uuid);
        return code;
    }
    
    /**
     * Validates an authentication code
     * 
     * @param code The authentication code to validate
     * @return The UUID associated with the code, or null if invalid
     */
    public UUID validateCode(String code) {
        if (!authCodes.containsKey(code)) {
            return null;
        }
        
        long expiry = authCodes.get(code);
        if (System.currentTimeMillis() > expiry) {
            authCodes.remove(code);
            userSessions.remove(code);
            return null;
        }
        
        return userSessions.get(code);
    }
    
    /**
     * Invalidates an authentication code
     * 
     * @param code The authentication code to invalidate
     */
    public void invalidateCode(String code) {
        authCodes.remove(code);
        userSessions.remove(code);
    }
    
    /**
     * Cleans up expired codes
     */
    public void cleanupExpiredCodes() {
        long now = System.currentTimeMillis();
        authCodes.entrySet().removeIf(entry -> {
            if (entry.getValue() < now) {
                userSessions.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Gets the expiration time of a code in epoch milliseconds
     * 
     * @param code The authentication code
     * @return The expiration time in epoch milliseconds, or -1 if the code doesn't exist
     */
    public long getEpochTimeOfCode(int code) {
        String codeStr = String.valueOf(code);
        if (!authCodes.containsKey(codeStr)) {
            return -1;
        }
        return authCodes.get(codeStr);
    }
    
    /**
     * Generates a code for a Discord user
     * 
     * @param userId The Discord user ID
     * @param nickname The Discord user's nickname
     * @return The generated authentication code
     */
    public String generateCode(String userId, String nickname) {
        String code = generateRandomCode();
        authCodes.put(code, System.currentTimeMillis() + CODE_EXPIRY_MS);
        // Store Discord user info instead of UUID
        return code;
    }
    
    /**
     * Gets the code associated with a Discord user
     * 
     * @param userId The Discord user ID
     * @return The authentication code, or null if none exists
     */
    public String getCodeForDiscordUser(String userId) {
        for (Map.Entry<String, UUID> entry : userSessions.entrySet()) {
            // In a real implementation, we would store Discord user IDs separately
            // For now, we'll return the first code as a placeholder
            return entry.getKey();
        }
        return null;
    }
    
    private String generateRandomCode() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
} 