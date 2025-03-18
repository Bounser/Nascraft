package me.bounser.nascraft.managers;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.managers.currencies.Currency;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class MoneyManager {

    private static MoneyManager instance;

    private Economy economy;
    public static MoneyManager getInstance() { return instance == null ? instance = new MoneyManager() : instance; }

    private MoneyManager() {
        economy = Nascraft.getEconomy();
    }

    public void withdraw(OfflinePlayer player, Currency currency, double amount, double taxRate) {

        switch (currency.getCurrencyType()) {

            case VAULT:
                economy.withdrawPlayer(player, amount);

                if (taxRate == 0)
                    DatabaseManager.get().getDatabase().addTransaction(amount, 0);
                else
                    DatabaseManager.get().getDatabase().addTransaction(amount, amount * taxRate);

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

    public void simpleWithdraw(OfflinePlayer player, Currency currency, double amount) {

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

    public void deposit(OfflinePlayer player, Currency currency, double amount, double taxRate) {

        switch (currency.getCurrencyType()) {

            case VAULT:
                economy.depositPlayer(player, amount);

                if (taxRate == 0)
                    DatabaseManager.get().getDatabase().addTransaction(-amount, Math.abs(amount - amount / taxRate));
                else
                    DatabaseManager.get().getDatabase().addTransaction(-amount, 0);

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

    public void simpleDeposit(OfflinePlayer player, Currency currency, double amount) {

        switch (currency.getCurrencyType()) {

            case VAULT:
                economy.depositPlayer(player, amount);

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

    public boolean hasEnoughMoney(OfflinePlayer player, Currency currency, double quantity) {

        switch (currency.getCurrencyType()) {

            case VAULT:
                return economy.getBalance(player) >= quantity;

            case CUSTOM:
                double balance = Double.parseDouble(PlaceholderAPI.setPlaceholders(player, currency.getBalancePlaceholder()));
                return (balance >= quantity);

            default:
                return false;
        }
    }

    public double getBalance(OfflinePlayer player, Currency currency) {

        switch (currency.getCurrencyType()) {

            case VAULT:
                return economy.getBalance(player);

            case CUSTOM:
                return Double.parseDouble(PlaceholderAPI.setPlaceholders(player, currency.getBalancePlaceholder()));

            default:
                return 0;
        }
    }
}