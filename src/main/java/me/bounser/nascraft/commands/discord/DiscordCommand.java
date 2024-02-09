package me.bounser.nascraft.commands.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.DiscordBot;
import me.bounser.nascraft.discord.linking.LinkManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;


public class DiscordCommand implements CommandExecutor {


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console."); return false;
        }

        Player player = ((Player) sender).getPlayer();

        if (!player.hasPermission("nascraft.discord")) {
            Lang.get().message((Player) sender, Message.NO_PERMISSION); return false;
        }

        if (LinkManager.getInstance().getUserDiscordID(player.getUniqueId()) == null) {
            Lang.get().message(player, Message.DISCORDCMD_NOT_LINKED);
            return false;
        }

        if (args.length == 0) {
            DiscordBot.getInstance().getJDA().retrieveUserById(LinkManager.getInstance().getUserDiscordID(player.getUniqueId()))
                    .queue(user -> Lang.get().message(player, Message.DISCORDCMD_LINKED, "[USER]", user.getName()));

            return false;
        }

        switch (args[0].toLowerCase()) {

            case "inv":
            case "inventory":

                Inventory inventory = Bukkit.createInventory(player, 45, Lang.get().message(Message.DISINV_TITLE));
                player.openInventory(inventory);

                DiscordInventoryInGame.getInstance().updateDiscordInventory(player);
                return false;

        }
        return false;
    }
}
