package me.bounser.nascraft.commands.sellwand;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.sellwand.Wand;
import me.bounser.nascraft.sellwand.WandsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.units.qual.N;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


public class GetSellWandCommand implements CommandExecutor {


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

                Wand wand = WandsManager.getInstance().getWands().get(args[0]);

                if (wand == null) {
                    Nascraft.getInstance().getLogger().info(ChatColor.RED + "No wand recognized with that ID!");
                    return false;
                }

                ItemStack wandItemStack = wand.getItemStackOfNewWand();

                NamespacedKey namespacedKey = new NamespacedKey(Nascraft.getInstance(), "identifier");
                ItemMeta meta = wandItemStack.getItemMeta();
                meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, UUID.randomUUID().toString());
                wandItemStack.setItemMeta(meta);

                player.getInventory().addItem(wandItemStack);

            }
            return false;
        }

        if (!sender.hasPermission("nascraft.admin")) {
            Lang.get().message((Player) sender, Message.NO_PERMISSION);
            return false;
        }

        if (args.length == 1) {

            Wand wand = WandsManager.getInstance().getWands().get(args[0]);

            if (wand == null) {
                sender.sendMessage(ChatColor.RED + "No wand recognized with that ID!");
                return false;
            }

            ItemStack wandItemStack = wand.getItemStackOfNewWand();

            NamespacedKey namespacedKey = new NamespacedKey(Nascraft.getInstance(), "identifier");
            ItemMeta meta = wandItemStack.getItemMeta();
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            wandItemStack.setItemMeta(meta);

            ((Player) sender).getInventory().addItem(wandItemStack);

        } else {
            sender.sendMessage(ChatColor.RED + "Invalid use of command. /getsellwand [Sell Wand ID]");
        }
        return false;
    }
}
