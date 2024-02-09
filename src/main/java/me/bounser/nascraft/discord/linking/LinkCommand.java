package me.bounser.nascraft.discord.linking;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.DiscordBot;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LinkCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nascraft.linkable")) {
            Lang.get().message(player, Message.NO_PERMISSION);
            return false;
        }

        if (LinkManager.getInstance().getUserDiscordID(player.getUniqueId()) != null) {
            Lang.get().message(player, Message.LINK_ALREADY_LINKED);
            return false;
        }

        if (args.length != 1) {
            Lang.get().message(player, Message.LINK_WRONG_USE);
            return false;
        }

        int code;

        try {
            code = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            Lang.get().message(player, Message.LINK_WRONG_FORMAT);
            return false;
        }

        if (!LinkManager.getInstance().codeExists(code)) {
            Lang.get().message(player, Message.LINK_NO_PROCESS_FOUND);
            return false;
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

        return false;
    }
}
