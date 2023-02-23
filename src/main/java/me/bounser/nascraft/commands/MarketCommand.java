package me.bounser.nascraft.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.MarketManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MarketCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED + "This command can only be used by players!");
            return false;
        }
        Player p = (Player) sender;

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED  + "Invalid use of command. /market <Buy/Sell> <Material> <Quantity>");
            return false;
        }
        switch (args[0]){
            case "buy":
                MarketManager.getInstance().getItem(args[1]).buyItem(Integer.parseInt(args[2]), p, args[1], 1);
                break;
            case "sell":
                MarketManager.getInstance().getItem(args[1]).sellItem(Integer.parseInt(args[2]), p, args[1], 1);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Wrong command.");
        }
        return false;
    }
}
