package me.bounser.nascraft.discord.linking;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class LinkCommand implements CommandExecutor {

    private Config lang = Config.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nascraft.linkable")) {
            player.sendMessage(lang.getPermissionText());
            return false;
        }

        if (!LinkManager.getInstance().getUserDiscordID(player).equals("-1")) {
            player.sendMessage(ChatColor.GRAY + "Already linked!");
            return false;
        }

        if (LinkManager.getInstance().isLinking(player)) {
            player.sendMessage(ChatColor.GRAY + "You are already in the process of linking! The code is " + LinkManager.getInstance().getCodeFromPlayer(player));
            return false;
        }

        int randomNumber = new Random().nextInt(100000) + 100;

        LinkManager.getInstance().addCode(randomNumber, player.getUniqueId());

        player.sendMessage(ChatColor.GRAY + "Your code is: " + randomNumber + "  Use /link " + randomNumber + " to link accounts!");

        return false;
    }
}
