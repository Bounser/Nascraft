package me.bounser.nascraft.web.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class DetailedTransactionDTO {
    
    private String itemId;
    private String itemName;
    private String itemMaterial;
    private UUID playerUUID;
    private String playerName;
    private double price;
    private int quantity;
    private double total;
    private LocalDateTime timestamp;
    private String transactionType;
    private String category;
    
    public DetailedTransactionDTO(String itemId, String itemName, String itemMaterial,
                         UUID playerUUID, String playerName, double price, int quantity, 
                         LocalDateTime timestamp, String transactionType, String category) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemMaterial = itemMaterial;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.price = price;
        this.quantity = quantity;
        this.total = price * quantity;
        this.timestamp = timestamp;
        this.transactionType = transactionType;
        this.category = category;
    }
    
    /**
     * Backward compatibility constructor for older code
     * @param itemId The item identifier
     * @param category The category of the item
     * @param quantity The quantity of the transaction
     * @param timestamp The timestamp of the transaction
     */
    public DetailedTransactionDTO(String itemId, String category, int quantity, long timestamp) {
        this.itemId = itemId;
        this.itemName = itemId; // Using itemId as name for backward compatibility
        this.itemMaterial = "UNKNOWN"; // Default material
        this.playerUUID = new UUID(0, 0); // Default UUID
        this.playerName = "Unknown"; // Default player name
        this.price = 0.0; // Default price
        this.quantity = quantity;
        this.total = 0.0; // Will be set later when price is available
        this.timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        this.transactionType = quantity > 0 ? "buy" : "sell";
        this.category = category;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public String getItemMaterial() {
        return itemMaterial;
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public double getPrice() {
        return price;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public double getTotal() {
        return total;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public String getCategory() {
        return category;
    }
} 