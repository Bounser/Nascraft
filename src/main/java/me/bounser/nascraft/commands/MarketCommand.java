package me.bounser.nascraft.commands;

import me.bounser.nascraft.market.MarketManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MarketCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(sender instanceof Player) {

            if (args.length != 3) {
                sender.sendMessage(ChatColor.RED  + "Invalid use of command. /market <Buy/Sell> <Material> <Quantity>");
                return false;
            }

            if (Integer.parseInt(args[2]) > 64) {
                sender.sendMessage(ChatColor.RED + "Quantity can't be higher than 64!");
                return false;
            }

            try {
                Material.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "That material isn't valid!");
                return false;
            }

            if(MarketManager.getInstance().getItem(args[1]) == null) {
                sender.sendMessage(ChatColor.RED + "That material isn't valid!");
                return false;
            }

            switch (args[0]){
                case "buy":
                    MarketManager.getInstance().getItem(args[1]).buyItem(Integer.parseInt(args[2]), (Player) sender, args[1], 1);
                    break;
                case "sell":
                    MarketManager.getInstance().getItem(args[1]).sellItem(Integer.parseInt(args[2]), (Player) sender, args[1], 1);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Wrong command.");
            }
        } else {
            if (args.length != 4) {
                sender.sendMessage(ChatColor.RED  + "Invalid use of command. (CONSOLE) /market <Buy/Sell> <Material> <Quantity> <Player>");
                return false;
            }
            switch (args[0]){
                case "buy":
                    MarketManager.getInstance().getItem(args[1]).buyItem(Integer.parseInt(args[2]), Bukkit.getPlayer(args[3]), args[1], 1);
                    break;
                case "sell":
                    MarketManager.getInstance().getItem(args[1]).sellItem(Integer.parseInt(args[2]), Bukkit.getPlayer(args[3]), args[1], 1);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Wrong command.");
            }
        }
        return false;
    }
}
