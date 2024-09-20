package me.bounser.nascraft.managers;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.managers.currencies.Currency;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

public class MoneyManager {

    public static MoneyManager instance;

    private Economy economy;
    private FileConfiguration config;
    public static MoneyManager getInstance() { return instance == null ? instance = new MoneyManager() : instance; }

    private MoneyManager() {
        economy = Nascraft.getEconomy();
        config = Nascraft.getInstance().getConfig();
    }

    public void withdraw(OfflinePlayer player, Currency currency, float amount, float taxRate) {

        switch (currency.getCurrencyType()) {

            case VAULT:
                economy.withdrawPlayer(player, amount);
                DatabaseManager.get().getDatabase().addTransaction(amount, Math.abs(amount - amount / taxRate));
                break;

            case CUSTOM:

                String command = currency.getWithdrawCommand()
                        .replace("[USER-NAME]", player.getName());
                if (amount == (int) amount) {
                    command = command.replace("[AMOUNT]", String.valueOf((int) amount));
                } else {
                    command = command.replace("[AMOUNT]", String.valueOf((int) amount));
                }

                String finalCommand = command;
                Bukkit.getScheduler().runTask(Nascraft.getInstance(), () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                });
        }
    }

    public void simpleWithdraw(OfflinePlayer player, Currency currency, float amount) {

        switch (currency.getCurrencyType()) {

            case VAULT:
                economy.withdrawPlayer(player, amount);
                break;

            case CUSTOM:

                String command = currency.getWithdrawCommand()
                        .replace("[USER-NAME]", player.getName());
                if (amount == (int) amount) {
                    command = command.replace("[AMOUNT]", String.valueOf((int) amount));
                } else {
                    command = command.replace("[AMOUNT]", String.valueOf((int) amount));
                }

                String finalCommand = command;
                Bukkit.getScheduler().runTask(Nascraft.getInstance(), () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                });
        }
    }

    public void deposit(OfflinePlayer player, Currency currency, float amount, float taxRate) {

        switch (currency.getCurrencyType()) {

            case VAULT:
                economy.depositPlayer(player, amount);
                DatabaseManager.get().getDatabase().addTransaction(-amount, Math.abs(amount - amount / taxRate));
                break;

            case CUSTOM:
                String command = currency.getDepositCommand()
                        .replace("[USER-NAME]", player.getName());
                if (amount == (int) amount) {
                    command = command.replace("[AMOUNT]", String.valueOf((int) amount));
                } else {
                    command = command.replace("[AMOUNT]", String.valueOf((int) amount));
                }

                String finalCommand = command;
                Bukkit.getScheduler().runTask(Nascraft.getInstance(), () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                });
        }
    }

    public boolean hasEnoughMoney(OfflinePlayer player, Currency currency, float quantity) {

        switch (currency.getCurrencyType()) {

            case VAULT:
                return economy.getBalance(player) >= quantity;

            case CUSTOM:
                float balance = Float.parseFloat(PlaceholderAPI.setPlaceholders(player, currency.getBalancePlaceholder()));
                return (balance >= quantity);

            default:
                return false;
        }
    }

    public float getBalance(OfflinePlayer player, Currency currency) {

        switch (currency.getCurrencyType()) {

            case VAULT:
                return (float) economy.getBalance(player);

            case CUSTOM:
                return Float.parseFloat(PlaceholderAPI.setPlaceholders(player, currency.getBalancePlaceholder()));

            default:
                return 0;
        }
    }
}