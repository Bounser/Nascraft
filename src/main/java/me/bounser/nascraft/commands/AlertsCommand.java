package me.bounser.nascraft.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.playerinfo.PlayerInfoManager;
import me.bounser.nascraft.discord.DiscordAlerts;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AlertsCommand implements CommandExecutor {

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

        if (args.length > 0) {
            player.sendMessage(ChatColor.RED + "Invalid use of command. /alerts"); return false;
        }

        String userID = PlayerInfoManager.getInstance().getPlayerReport(player.getUniqueId()).getUserDiscordID();

        if (userID == null) {
            player.sendMessage(ChatColor.RED + "Account not linked"); return false;
        }

        if (DiscordAlerts.getInstance().getAlerts().get(userID) == null || DiscordAlerts.getInstance().getAlerts().get(userID).size() == 0) {
            player.sendMessage(ChatColor.RED + "No don't have any alert setup."); return false;
        }

        String alerts = ChatColor.LIGHT_PURPLE + "Active alerts:\n ";

        for (Item item : DiscordAlerts.getInstance().getAlerts().get(userID).keySet())
            alerts = alerts + ChatColor.LIGHT_PURPLE + "\n> " + ChatColor.GRAY + item.getName() + " at price: " + Math.abs(DiscordAlerts.getInstance().getAlerts().get(userID).get(item)) + Config.getInstance().getCurrency();

        player.sendMessage(alerts);

        return false;
    }
}
