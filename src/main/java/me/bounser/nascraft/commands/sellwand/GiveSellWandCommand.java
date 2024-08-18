package me.bounser.nascraft.commands.sellwand;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.Command;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.sellwand.Wand;
import me.bounser.nascraft.sellwand.WandsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GiveSellWandCommand extends Command {

    public GiveSellWandCommand() {
        super(
                "givesellwand",
                new String[]{Config.getInstance().getCommandAlias("givesellwand")},
                "Give a sellwand.",
                "nascraft.admin"
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {

            if (args.length != 2) {
                Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Invalid use of command. /givesellwand [sellwand] [player]");
                return;
            } else {

                Player player = Bukkit.getPlayer(args[1]);

                if (player == null) {
                    Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Invalid player.");
                    return;
                }

                Wand wand = WandsManager.getInstance().getWands().get(args[0]);

                if (wand == null) {
                    Nascraft.getInstance().getLogger().info(ChatColor.RED + "No wand recognized with that ID!");
                    return;
                }

                ItemStack wandItemStack = wand.getItemStackOfNewWand();

                NamespacedKey namespacedKey = new NamespacedKey(Nascraft.getInstance(), "identifier");
                ItemMeta meta = wandItemStack.getItemMeta();
                meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, UUID.randomUUID().toString());
                wandItemStack.setItemMeta(meta);

                player.getInventory().addItem(wandItemStack);

            }
            return;
        }

        if (!sender.hasPermission("nascraft.admin")) {
            Lang.get().message((Player) sender, Message.NO_PERMISSION);
            return;
        }

        if (args.length == 1) {

            Wand wand = WandsManager.getInstance().getWands().get(args[0]);

            if (wand == null) {
                sender.sendMessage(ChatColor.RED + "No wand recognized with that ID!");
                return;
            }

            ItemStack wandItemStack = wand.getItemStackOfNewWand();

            NamespacedKey namespacedKey = new NamespacedKey(Nascraft.getInstance(), "identifier");
            ItemMeta meta = wandItemStack.getItemMeta();
            meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            wandItemStack.setItemMeta(meta);

            ((Player) sender).getInventory().addItem(wandItemStack);

        } else {
            sender.sendMessage(ChatColor.RED + "Invalid use of command. /givesellwand [Sell Wand ID]");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return StringUtil.copyPartialMatches(args[0], WandsManager.getInstance().getWands().keySet(), new ArrayList<>());
    }
}
