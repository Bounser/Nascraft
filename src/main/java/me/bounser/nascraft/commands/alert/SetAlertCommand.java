package me.bounser.nascraft.commands.alert;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.Command;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SetAlertCommand extends Command {

    public SetAlertCommand() {
        super(
                "setalert",
                new String[]{Config.getInstance().getCommandAlias("setalerts")},
                "Set up new alerts",
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

        if (args.length != 2) {
            Lang.get().message(player, Message.ALERT_INVALID_USE); return;
        }

        String userID = LinkManager.getInstance().getUserDiscordID(player.getUniqueId());
        if (userID == null) {
            Lang.get().message(player, Message.ALERT_NOT_LINKED); return;
        }

        if (DiscordAlerts.getInstance().getAlerts().get(userID) != null &&
                DiscordAlerts.getInstance().getAlerts().get(userID).keySet().contains(args[1])) {

            Lang.get().message(player, Message.ALERT_IN_WATCHLIST); return;
        }

        String floatRegex = "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";

        if (!args[1].matches(floatRegex)) {
            Lang.get().message(player, Message.ALERT_INVALID_PRICE); return;        }

        Item item = MarketManager.getInstance().getItem(args[0]);

        if (item == null) {
            Lang.get().message(player, Message.ALERT_NOT_RECOGNIZED); return;
        }

        DiscordAlerts.getInstance().setAlert(userID, args[0], Float.parseFloat(args[1]));

        Lang.get().message(player, Message.ALERT_SETUP);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
