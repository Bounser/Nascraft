package me.bounser.nascraft.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MarketCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(sender instanceof Player) {

            Player player = (Player) sender;

            if (!player.hasPermission("nascraft.market") && Config.getInstance().getMarketPermissionRequirement()) {
                Lang.get().message(player, Message.NO_PERMISSION);
                return false;
            }

            if (args.length != 3) {
                player.sendMessage(ChatColor.RED  + "Invalid use of command. /market <Buy/Sell> <Material> <Quantity>");
                return false;
            }

            if (Integer.parseInt(args[2]) > 64) {
                player.sendMessage(ChatColor.RED + "Quantity can't be higher than 64!");
                return false;
            }

            if (MarketManager.getInstance().getItem(args[1]) == null) {
                player.sendMessage(ChatColor.RED + "That identifier isn't valid!");
                return false;
            }
            Item item = MarketManager.getInstance().getItem(args[1]);
            switch (args[0]){
                case "buy":
                    item.buyItem(Integer.parseInt(args[2]), player.getUniqueId(), true, item.getItemStack().getType());
                    break;
                case "sell":
                    item.sellItem(Integer.parseInt(args[2]), player.getUniqueId(), true, item.getItemStack().getType());
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "Wrong command.");
            }

        } else {
            if (args.length != 4) {
                Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Invalid use of command. (CONSOLE) /market <Buy/Sell> <Material> <Quantity> <Player>");
                return false;
            }

            Player player = Bukkit.getPlayer(args[3]);

            if (player == null) {
                Nascraft.getInstance().getLogger().info(ChatColor.RED + "Invalid player");
                return false;
            }
            Item item = MarketManager.getInstance().getItem(args[1]);
            switch (args[0]){
                case "buy":
                    item.buyItem(Integer.parseInt(args[2]), player.getUniqueId(), true, item.getItemStack().getType());
                    break;
                case "sell":
                    item.sellItem(Integer.parseInt(args[2]), player.getUniqueId(), true, item.getItemStack().getType());
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Wrong command.");
            }
        }
        return false;
    }

}
