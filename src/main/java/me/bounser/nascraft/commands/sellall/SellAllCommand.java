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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;


public class SellAllCommand implements CommandExecutor {


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
            return false;
        }

        Player player = (Player) sender;

        Config lang = Config.getInstance();

        if (!player.hasPermission("nascraft.sellall")) {
            player.sendMessage(Config.getInstance().getPermissionText());
            return false;
        }

        if (args.length < 1) {
            player.sendMessage(lang.getSellallErrorWrongMaterialText());
            return false;
        } else if (args[0].equalsIgnoreCase("everything")) {

            if(args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
                sellEverything((Player) sender, true);
            } else {
                sellEverything((Player) sender, false);
            }

        } else {

            if(!MarketManager.getInstance().getAllMaterials().contains(args[0].toLowerCase())) {
                player.sendMessage(lang.getSellallErrorWrongMaterialText());
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
                    nascraftItem.sellItem(items.get(nascraftItem), player, 1);
                    return false;
                }

                String text = ChatColor.GRAY + "Estimated value: \n\n> " + ChatColor.GOLD + nascraftItem.getName() +" x "+ items.get(nascraftItem) +" = "+ RoundUtils.round(nascraftItem.getPrice().getSellPrice()*items.get(nascraftItem)) + Config.getInstance().getCurrency() + "\n  ";

                TextComponent message = new TextComponent("\n " + ChatColor.GOLD + "[ Click here to confirm ]\n");
                message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nsellall " + nascraftItem.getMaterial() + " confirm"));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(text).create()));

                player.spigot().sendMessage(message);

            } else {

                player.sendMessage(lang.getSellallErrorWithoutItemText(nascraftItem.getName()));

            }
        }
        return false;
    }

    public void sellEverything(Player player, boolean confirmed) {

        if (player.getInventory().getContents() == null) {
            return;
        }

        HashMap<Item, Integer> items = new HashMap<>();

        List<String> materials = MarketManager.getInstance().getAllMaterials();

        for (ItemStack itemStack : player.getInventory()) {

            if (itemStack != null) {
                Item item = MarketManager.getInstance().getItem(itemStack.getType().toString());

                if (materials.contains(itemStack.getType().toString().toLowerCase())) {

                    if (items.get(item) == null) {

                        items.put(item, itemStack.getAmount());

                    } else {

                        items.put(item, items.get(item) + itemStack.getAmount());
                    }
                }
            }
        }

        if (items.isEmpty()) {
            player.sendMessage(Config.getInstance().getSellallEverythingErrorText());
            return;
        }

        float totalValue = 0;

        for (Item item : items.keySet()) {
            totalValue += item.getPrice().getSellPrice()*items.get(item);
        }

        if (confirmed) {

            for (Item item : items.keySet()) {
                player.getInventory().remove(Material.valueOf(item.getMaterial().toUpperCase()));
            }

            for (Item item : items.keySet()) {

                item.ghostSellItem(items.get(item));
            }
            player.sendMessage(Config.getInstance().getSellallEverythingText(String.valueOf(items.values().size()), String.valueOf(totalValue)));
            Nascraft.getEconomy().depositPlayer(player, totalValue);

        } else {
            String text = Config.getInstance().getSellallEverythingEstimatedText(String.valueOf(totalValue)) + "\n" ;

            for (Item item : items.keySet()) {
                text = text + ChatColor.GRAY + "\n> " + ChatColor.GOLD + item.getName() + " x " + items.get(item) + " = " + RoundUtils.round(item.getPrice().getSellPrice()*items.get(item)) + Config.getInstance().getCurrency();
            }

            text = text + "\n";

            TextComponent message = new TextComponent(Config.getInstance().getClickToConfirmText());
            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nsellall everything confirm"));
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(text).create()));

            player.spigot().sendMessage(message);
        }
    }

}
