package me.bounser.nascraft.managers;


import me.bounser.nascraft.Nascraft;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

public class MoneyManager {

    public static MoneyManager instance;

    private Economy economy;

    public static MoneyManager getInstance() { return instance == null ? instance = new MoneyManager() : instance; }

    private MoneyManager() { economy = Nascraft.getEconomy(); }

    public void withdraw(OfflinePlayer player, float quantity) { economy.withdrawPlayer(player, quantity); }

    public void deposit(OfflinePlayer player, float quantity) { economy.depositPlayer(player, quantity); }

    public boolean hasEnoughMoney(OfflinePlayer player, float quantity) { return economy.getBalance(player) >= quantity; }

}
