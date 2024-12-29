package me.bounser.nascraft.commands.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.Command;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.DiscordBot;
import me.bounser.nascraft.discord.linking.LinkManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DiscordCommand extends Command {

    public DiscordCommand() {
        super(
                "discord",
                new String[]{Config.getInstance().getCommandAlias("discord")},
                "Discord options",
                "nascraft.discord"
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console."); return;
        }

        Player player = ((Player) sender).getPlayer();

        if (!player.hasPermission("nascraft.discord")) {
            Lang.get().message((Player) sender, Message.NO_PERMISSION); return;
        }

        if (LinkManager.getInstance().getUserDiscordID(player.getUniqueId()) == null) {
            Lang.get().message(player, Message.DISCORDCMD_NOT_LINKED);
            return;
        }

        if (args.length == 0) {
            DiscordBot.getInstance().getJDA().retrieveUserById(LinkManager.getInstance().getUserDiscordID(player.getUniqueId()))
                    .queue(user -> Lang.get().message(player, Message.DISCORDCMD_LINKED, "[USER]", user.getName()));
            return;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
