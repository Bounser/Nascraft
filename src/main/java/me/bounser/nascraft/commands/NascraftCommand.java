package me.bounser.nascraft.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.Data;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.database.JsonManager;
import me.leoko.advancedgui.manager.GuiWallManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class NascraftCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("nascraft.admin") && sender instanceof Player) {
            sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " +ChatColor.RED + "You are not allowed to use this command!");
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " +ChatColor.RED + "Wrong syntax. Available arguments: force | save | info | status");
            return false;
        }

        switch(args[0]){
            case "save":

                try {
                    Data.getInstance().updateData();
                } catch (SQLException e) {
                    Nascraft.getInstance().getLogger().warning("Error trying to save the data!");
                    e.printStackTrace();
                }
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Data saved.");
                break;
            case "info":
                for (Item item : MarketManager.getInstance().getAllItems()) {
                    sender.sendMessage(ChatColor.GRAY + "Mat: " + item.getMaterial() + " value: " + item.getPrice().getValue() + " stock: " + item.getPrice().getStock());
                }
                break;
            case "locate":
                sender.sendMessage(String.valueOf(GuiWallManager.getInstance().getActiveInstances((Player) sender).get(0).getInteraction((Player) sender).getComponentTree().locate(args[1]).getState((Player) sender, GuiWallManager.getInstance().getActiveInstances((Player) sender).get(0).getCursor((Player) sender))));
                break;

            case "close":
                if(MarketManager.getInstance().getState()) {
                    MarketManager.getInstance().stop();
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Shop stopped. Resume it with /nascraft resume.");
                } else {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Shop is already stopped!");
                }
                break;
            case "resume":
                if(!MarketManager.getInstance().getState()) {
                    MarketManager.getInstance().resume();
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Shop resumed.");
                } else {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Shop is already active!");
                }
                break;

            default: sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Argument not recognized. Available arguments: save | info | stop | resume");
        }
        return false;
    }

}
