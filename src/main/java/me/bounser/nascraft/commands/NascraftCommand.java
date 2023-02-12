package me.bounser.nascraft.commands;

import me.bounser.nascraft.market.Item;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.MarketStatus;
import me.bounser.nascraft.market.PricesManager;
import me.bounser.nascraft.tools.Data;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NascraftCommand implements CommandExecutor {


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!sender.hasPermission("nascraft.admin") && sender instanceof Player) {
            sender.sendMessage(ChatColor.RED + "You are not allowed to use this command!");
            return false;
        }
        switch(args[0]){

            case "force":
                switch (args[1]){
                    case "bull1": PricesManager.getInstance().setMarketStatus(MarketStatus.BULL1); break;
                    case "bull2": PricesManager.getInstance().setMarketStatus(MarketStatus.BULL2); break;
                    case "bull3": PricesManager.getInstance().setMarketStatus(MarketStatus.BULL3); break;
                    case "bullrun": PricesManager.getInstance().setMarketStatus(MarketStatus.BULLRUN); break;

                    case "bear1": PricesManager.getInstance().setMarketStatus(MarketStatus.BEAR1); break;
                    case "bear2": PricesManager.getInstance().setMarketStatus(MarketStatus.BEAR2); break;
                    case "bear3": PricesManager.getInstance().setMarketStatus(MarketStatus.BEAR3); break;
                    case "crash": PricesManager.getInstance().setMarketStatus(MarketStatus.CRASH); break;

                    case "flat": PricesManager.getInstance().setMarketStatus(MarketStatus.FLAT); break;

                    default: sender.sendMessage(ChatColor.RED + "Market status not recognized.");
                }

            case "save":
                Data.getInstance().savePrices();
                break;

            case "info":
                for(Item item : MarketManager.getInstance().getAllItems()) {
                    sender.sendMessage(ChatColor.GRAY + "Mat: " + item.getMaterial() + " price: " + item.getPrice() + " stock: " + item.getStock());
                }

            default: if(sender instanceof Player) sender.sendMessage(ChatColor.RED + "Argument not recognized.");
        }

        return false;
    }

}
