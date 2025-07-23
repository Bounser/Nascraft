package me.bounser.nascraft.web.dto;

import java.util.UUID;

public class PlayerStatsDTO {
    
    private UUID playerUUID;
    private String playerName;
    private double totalSpent;
    private double totalEarned;
    private int itemsBought;
    private int itemsSold;
    private double profit;
    
    public PlayerStatsDTO(UUID playerUUID, String playerName, double totalSpent, double totalEarned, int itemsBought, int itemsSold) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.totalSpent = totalSpent;
        this.totalEarned = totalEarned;
        this.itemsBought = itemsBought;
        this.itemsSold = itemsSold;
        this.profit = totalEarned - totalSpent;
    }
    
    /**
     * Backward compatibility constructor for older code
     * @param playerIdLong The player ID as a long (will be converted to UUID)
     * @param totalSpent The total amount spent
     * @param totalEarned The total amount earned
     * @param profit The profit (totalEarned - totalSpent)
     */
    public PlayerStatsDTO(long playerIdLong, double totalSpent, double totalEarned, double profit) {
        // Convert long to UUID (this is a simplistic approach)
        this.playerUUID = new UUID(0, playerIdLong);
        this.playerName = "Player-" + playerIdLong; // Default player name
        this.totalSpent = totalSpent;
        this.totalEarned = totalEarned;
        // Estimate bought/sold items based on average price of 10.0
        this.itemsBought = (int)(totalSpent / 10.0);
        this.itemsSold = (int)(totalEarned / 10.0);
        this.profit = profit;
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public double getTotalSpent() {
        return totalSpent;
    }
    
    public double getTotalEarned() {
        return totalEarned;
    }
    
    public int getItemsBought() {
        return itemsBought;
    }
    
    public int getItemsSold() {
        return itemsSold;
    }
    
    public double getProfit() {
        return profit;
    }
} 