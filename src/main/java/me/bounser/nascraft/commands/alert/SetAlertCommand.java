package me.bounser.nascraft.commands.alert;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetAlertCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console."); return false;
        }

        Player player = ((Player) sender).getPlayer();

        if (!player.hasPermission("nascraft.alert")) {
            Lang.get().message((Player) sender, Message.NO_PERMISSION); return false;
        }

        if (args.length != 2) {
            Lang.get().message(player, Message.ALERT_INVALID_USE); return false;
        }

        String userID = LinkManager.getInstance().getUserDiscordID(player.getUniqueId());
        if (userID == null) {
            Lang.get().message(player, Message.ALERT_NOT_LINKED); return false;
        }

        if (DiscordAlerts.getInstance().getAlerts().get(userID) != null &&
                DiscordAlerts.getInstance().getAlerts().get(userID).keySet().contains(args[1])) {

            Lang.get().message(player, Message.ALERT_IN_WATCHLIST); return false;
        }

        String floatRegex = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";

        if (!args[1].matches(floatRegex)) {
            Lang.get().message(player, Message.ALERT_INVALID_PRICE); return false;        }

        Item item = MarketManager.getInstance().getItem(args[0]);

        if (item == null) {
            Lang.get().message(player, Message.ALERT_NOT_RECOGNIZED); return false;
        }

        DiscordAlerts.getInstance().setAlert(userID, args[0], Float.parseFloat(args[1]));

        Lang.get().message(player, Message.ALERT_SETUP); return false;
    }
}
