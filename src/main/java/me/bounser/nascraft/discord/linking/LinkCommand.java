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
            player.sendMessage(ChatColor.GRAY + "Already linked!");
            return false;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.GRAY + "Wrong use of the command!");
            return false;
        }

        int code;

        try {
            code = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.GRAY + "Wrong code format!");
            return false;
        }

        if (!LinkManager.getInstance().codeExists(code)) {
            player.sendMessage(ChatColor.GRAY + "No linking process found with that code.");
            return false;
        }

        DiscordBot.getInstance().getJDA().retrieveUserById(LinkManager.getInstance().getUserFromCode(code))
                .queue(user -> {
                    if (LinkManager.getInstance().redeemCode(code, ((Player) sender).getUniqueId(), player.getName())) {
                        player.sendMessage(ChatColor.GRAY + "Linked successfully with user " + user.getName());
                    }
                    user.openPrivateChannel().queue(privateChannel -> {
                        privateChannel.sendMessage(":link: Your discords account has been successfully linked to the minecraft user: ``" + player.getName() + "``").queue();
                    });
                });

        return false;
    }
}
