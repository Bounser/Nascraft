package me.bounser.nascraft.commands.sellwand;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.sellwand.Wand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class GetSellWandCommand implements CommandExecutor {

    private HashMap<String, Wand> wands;

    public GetSellWandCommand() {

        for (Wand wand : Config.getInstance().getWands())
            wands.put(wand.getName(), wand);

    }

    public HashMap<String, Wand> getWands() { return wands; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {

            if (args.length != 2) {
                Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Invalid use of command. /getsellwand [sellwand] [player]");
                return false;
            } else {

                Player player = Bukkit.getPlayer(args[1]);

                if (player == null) {
                    Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Invalid player.");
                    return false;
                }

                Wand wand = wands.get(args[0]);

                if (wand == null) {
                    Nascraft.getInstance().getLogger().info(ChatColor.RED + "No wand recognized with that ID!");
                    return false;
                }

                player.getInventory().addItem(wand.getItemStackOfNewWand());

            }
            return false;
        }

        if (args.length != 1) {

            if (sender.hasPermission("nascraft.admin")) {

                Wand wand = wands.get(args[0]);

                if (wand == null) {
                    sender.sendMessage(ChatColor.RED + "No wand recognized with that ID!");
                    return false;
                }

                ((Player) sender).getInventory().addItem(wand.getItemStackOfNewWand());

            } else {
                Lang.get().message((Player) sender, Message.NO_PERMISSION);
                return false;
            }
        }
        return false;
    }
}
