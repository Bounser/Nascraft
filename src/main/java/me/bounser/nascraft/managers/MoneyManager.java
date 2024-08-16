package me.bounser.nascraft.managers;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.DatabaseManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

public class MoneyManager {

    public static MoneyManager instance;

    private Economy economy;

    public static MoneyManager getInstance() { return instance == null ? instance = new MoneyManager() : instance; }

    private MoneyManager() { economy = Nascraft.getEconomy(); }

    public void withdraw(OfflinePlayer player, float quantity, float taxRate) {
        economy.withdrawPlayer(player, quantity);
        DatabaseManager.get().getDatabase().addTransaction(quantity, Math.abs(quantity-quantity/taxRate));
    }

    public void deposit(OfflinePlayer player, float quantity, float taxRate) {
        economy.depositPlayer(player, quantity);
        DatabaseManager.get().getDatabase().addTransaction(-quantity, Math.abs(quantity-quantity/taxRate));
    }

    public boolean hasEnoughMoney(OfflinePlayer player, float quantity) { return economy.getBalance(player) >= quantity; }

}
