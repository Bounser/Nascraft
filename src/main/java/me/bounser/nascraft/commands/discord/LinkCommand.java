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

import java.util.Arrays;
import java.util.List;

public class LinkCommand extends Command {

    public LinkCommand() {
        super(
                "link",
                new String[]{Config.getInstance().getCommandAlias("link")},
                "Link a minecraft account with discord",
                "nascraft.linkable"
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nascraft.linkable")) {
            Lang.get().message(player, Message.NO_PERMISSION);
            return;
        }

        if (LinkManager.getInstance().getUserDiscordID(player.getUniqueId()) != null) {
            Lang.get().message(player, Message.LINK_ALREADY_LINKED);
            return;
        }

        if (args.length != 1) {
            Lang.get().message(player, Message.LINK_WRONG_USE);
            return;
        }

        int code;

        try {
            code = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            Lang.get().message(player, Message.LINK_WRONG_FORMAT);
            return;
        }

        if (!LinkManager.getInstance().codeExists(code)) {
            Lang.get().message(player, Message.LINK_NO_PROCESS_FOUND);
            return;
        }

        DiscordBot.getInstance().getJDA().retrieveUserById(LinkManager.getInstance().getUserFromCode(code))
                .queue(user -> {
                    if (LinkManager.getInstance().redeemCode(code, ((Player) sender).getUniqueId(), player.getName())) {
                        Lang.get().message(player, Message.LINK_SUCCESS, "[USER]", user.getName());
                    }
                    user.openPrivateChannel().queue(privateChannel -> {
                        privateChannel.sendMessage(Lang.get().message(Message.LINK_DIRECT_MESSAGE, "[USER]", player.getName())).queue();
                    });
                });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? Arrays.asList("code") : null;
    }
}
