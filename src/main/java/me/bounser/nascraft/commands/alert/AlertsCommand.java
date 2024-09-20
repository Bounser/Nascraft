package me.bounser.nascraft.commands.alert;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.Command;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AlertsCommand extends Command {

    public AlertsCommand() {
        super(
                "alerts",
                new String[]{Config.getInstance().getCommandAlias("alerts")},
                "Check your alerts",
                "nascraft.alert"
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console."); return;
        }

        Player player = ((Player) sender).getPlayer();

        if (!player.hasPermission("nascraft.alert")) {
            Lang.get().message((Player) sender, Message.NO_PERMISSION); return;
        }

        if (args.length > 0) { Lang.get().message(player, Message.ALERT_INVALID_USE); }

        String userID = LinkManager.getInstance().getUserDiscordID(player.getUniqueId());

        if (userID == null) {
            Lang.get().message(player, Message.ALERT_NOT_LINKED); return;
        }

        if (DiscordAlerts.getInstance().getAlerts().get(userID) == null || DiscordAlerts.getInstance().getAlerts().get(userID).size() == 0) {
            Lang.get().message(player, Message.ALERTS_EMPTY); return;
        }

        String alerts = Lang.get().message(Message.ALERTS_HEADER);

        for (Item item : DiscordAlerts.getInstance().getAlerts().get(userID).keySet())
            alerts = alerts + Lang.get().message(Message.ALERTS_LIST_SEGMENT, Formatter.format(item.getCurrency(), Math.abs(DiscordAlerts.getInstance().getAlerts().get(userID).get(item)), Style.ROUND_BASIC), "0", item.getName());

        Lang.get().message(player, alerts);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
