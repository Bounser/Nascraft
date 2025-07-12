package me.bounser.nascraft.web.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class TransactionDTO {
    
    private String itemId;
    private String itemName;
    private UUID playerUUID;
    private String playerName;
    private double price;
    private int quantity;
    private double total;
    private LocalDateTime timestamp;
    private String transactionType;
    
    public TransactionDTO(String itemId, String itemName, UUID playerUUID, String playerName,
                         double price, int quantity, LocalDateTime timestamp, String transactionType) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.price = price;
        this.quantity = quantity;
        this.total = price * quantity;
        this.timestamp = timestamp;
        this.transactionType = transactionType;
    }
    
    /**
     * Backward compatibility constructor for older code
     * @param itemId The item identifier
     * @param quantity The quantity of the transaction
     * @param price The price per item
     * @param timestamp The timestamp of the transaction
     */
    public TransactionDTO(String itemId, int quantity, double price, long timestamp) {
        this.itemId = itemId;
        this.itemName = itemId; // Using itemId as name for backward compatibility
        this.playerUUID = new UUID(0, 0); // Default UUID
        this.playerName = "Unknown"; // Default player name
        this.price = price;
        this.quantity = quantity;
        this.total = price * quantity;
        this.timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        this.transactionType = quantity > 0 ? "buy" : "sell";
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public String getItemName() {
        return itemName;
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
} 