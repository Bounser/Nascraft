package me.bounser.nascraft.commands.sellall;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.RoundUtils;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;


public class SellAllCommand implements CommandExecutor {


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nascraft.sellall")) {
            player.sendMessage(ChatColor.RED + "Permission required.");
            return false;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Material not recognized. Usage: /sellall [material]");
            return false;
        /*} else if (args[0].equalsIgnoreCase("everything")) {

            TODO: implementation.

        */
        } else {

            if(!MarketManager.getInstance().getAllMaterials().contains(args[0].toLowerCase())) {
                player.sendMessage(ChatColor.RED + "Material not recognized. Usage: /sellall [material]");
                return false;
            }

            PlayerInventory inventory = player.getInventory();

            HashMap<Item, Integer> items = new HashMap<>();
            Item nascraftItem = MarketManager.getInstance().getItem(args[0].toLowerCase());

            for(ItemStack item : inventory) {

                if(item != null && item.getType().toString().equals(args[0].toUpperCase())) {

                    if(items.get(nascraftItem) != null) {

                        items.put(nascraftItem, items.get(nascraftItem) + item.getAmount());

                    } else {

                        items.put(nascraftItem, item.getAmount());

                    }
                }
            }

            if(items.get(nascraftItem) != null) {

                if(args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
                    nascraftItem.sellItem(items.get(nascraftItem), player, nascraftItem.getMaterial(), 1);
                    return false;
                }

                String text = ChatColor.GRAY + "Estimated value: \n\n> " + ChatColor.GOLD + nascraftItem.getName() +" x "+ items.get(nascraftItem) +" = "+ RoundUtils.round(nascraftItem.getPrice().getSellPrice()*items.get(nascraftItem)) + Config.getInstance().getCurrency() + "\n  ";

                TextComponent message = new TextComponent("\n " + ChatColor.GOLD + "[ Click here to confirm ]\n");
                message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nsellall " + nascraftItem.getMaterial() + " confirm"));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(text).create()));

                player.spigot().sendMessage(message);

            } else {

                player.sendMessage(ChatColor.RED + "You don't have any " + nascraftItem.getName() + " to sell.");

            }
        }
        return false;
    }
}
