package me.bounser.nascraft.commands;

import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.managers.resources.Trend;
import me.bounser.nascraft.market.managers.PricesManager;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.Data;
import me.leoko.advancedgui.manager.GuiWallManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
            case "force":
                if (Config.getInstance().isForceAllowed())
                switch (args[1]){
                    case "bull1": PricesManager.getInstance().setMarketStatus(Trend.BULL1); break;
                    case "bull2": PricesManager.getInstance().setMarketStatus(Trend.BULL2); break;
                    case "bull3": PricesManager.getInstance().setMarketStatus(Trend.BULL3); break;
                    case "bullrun": PricesManager.getInstance().setMarketStatus(Trend.BULLRUN); break;

                    case "bear1": PricesManager.getInstance().setMarketStatus(Trend.BEAR1); break;
                    case "bear2": PricesManager.getInstance().setMarketStatus(Trend.BEAR2); break;
                    case "bear3": PricesManager.getInstance().setMarketStatus(Trend.BEAR3); break;
                    case "crash": PricesManager.getInstance().setMarketStatus(Trend.CRASH); break;

                    case "flat": PricesManager.getInstance().setMarketStatus(Trend.FLAT); break;

                    default: sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Market status not recognized.");
                } else {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "<force> command is disabled in the config.");
                }
                break;
            case "save":
                Data.getInstance().savePrices();
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Data saved.");
                break;
            case "info":
                for (Item item : MarketManager.getInstance().getAllItems()) {
                    sender.sendMessage(ChatColor.GRAY + "Mat: " + item.getMaterial() + " price: " + item.getPrice() + " stock: " + item.getStock());
                }
                break;
            case "status":
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "General status: " + ChatColor.BLUE + PricesManager.getInstance().getMarketStatus().toString());
                break;
            case "locate":
                sender.sendMessage(String.valueOf(GuiWallManager.getInstance().getActiveInstances((Player) sender).get(0).getInteraction((Player) sender).getComponentTree().locate(args[1]).getState((Player) sender, GuiWallManager.getInstance().getActiveInstances((Player) sender).get(0).getCursor((Player) sender))));
                break;
            default: sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Argument not recognized. Available arguments: force | save | info | status");
        }
        return false;
    }

}
