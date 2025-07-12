package me.bounser.nascraft.web.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class DebtDTO {
    
    private UUID playerUUID;
    private String playerName;
    private double amount;
    private double interestRate;
    private LocalDateTime dueDate;
    private LocalDateTime borrowDate;
    private boolean isPaid;
    
    public DebtDTO(UUID playerUUID, String playerName, double amount, double interestRate, 
                 LocalDateTime dueDate, LocalDateTime borrowDate, boolean isPaid) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.amount = amount;
        this.interestRate = interestRate;
        this.dueDate = dueDate;
        this.borrowDate = borrowDate;
        this.isPaid = isPaid;
    }
    
    /**
     * Backward compatibility constructor for older code
     * @param principal The principal amount of the debt
     * @param interest The interest amount
     * @param dailyRate The daily interest rate
     * @param collateral The collateral value
     * @param dueDate The due date as a timestamp
     * @param borrowDate The borrow date as a timestamp
     * @param status The status of the debt (paid or not)
     */
    public DebtDTO(double principal, double interest, double dailyRate, double collateral, 
                 double dueDate, double borrowDate, String status) {
        this.playerUUID = new UUID(0, 0); // Default UUID
        this.playerName = "Unknown"; // Default player name
        this.amount = principal;
        this.interestRate = dailyRate;
        this.dueDate = LocalDateTime.ofInstant(Instant.ofEpochMilli((long)dueDate), ZoneId.systemDefault());
        this.borrowDate = LocalDateTime.ofInstant(Instant.ofEpochMilli((long)borrowDate), ZoneId.systemDefault());
        this.isPaid = "paid".equalsIgnoreCase(status);
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public double getInterestRate() {
        return interestRate;
    }
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public LocalDateTime getBorrowDate() {
        return borrowDate;
    }
    
    public boolean isPaid() {
        return isPaid;
    }
} 