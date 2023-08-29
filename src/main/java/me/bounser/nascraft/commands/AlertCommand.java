package me.bounser.nascraft.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.playerinfo.PlayerInfoManager;
import me.bounser.nascraft.discord.DiscordAlerts;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AlertCommand implements CommandExecutor {

    private Config lang = Config.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console."); return false;
        }

        Player player = ((Player) sender).getPlayer();

        if (!player.hasPermission("nascraft.alert")) {
            player.sendMessage(lang.getPermissionText()); return false;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Invalid use of command. /alert [material] [price]"); return false;
        }

        String userID = PlayerInfoManager.getInstance().getPlayerReport(player.getUniqueId()).getUserDiscordID();

        if (userID == null) {
            player.sendMessage(ChatColor.RED + "Account not linked."); return false;
        }

        if (DiscordAlerts.getInstance().getAlerts().get(userID) != null &&
                DiscordAlerts.getInstance().getAlerts().get(userID).keySet().contains(args[1])) {

            player.sendMessage(ChatColor.RED + "Item already on your watchlist."); return false;
        }

        String floatRegex = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";

        if (!args[1].matches(floatRegex)) {
            player.sendMessage(ChatColor.RED + "Invalid use of command. Price must be a number. (Ex 120.90)"); return false;
        }

        Item item = MarketManager.getInstance().getItem(args[0]);

        if (item == null) {
            player.sendMessage(ChatColor.RED + "Item not recognized."); return false;
        }

        DiscordAlerts.getInstance().setAlert(userID, args[0], Float.parseFloat(args[1]));

        player.sendMessage(ChatColor.GRAY + "Alert setup correctly! You'll receive a DM when the price is reached.");

        return false;
    }
}
