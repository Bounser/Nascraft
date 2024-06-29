package me.bounser.nascraft.commands.sell.sellall;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
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


public class SellAllCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nascraft.sellall")) {
            Lang.get().message(player, Message.NO_PERMISSION); return false;
        }

        if (args.length < 1) {
            Lang.get().message(player, Message.SELLALL_ERROR_WRONG_MATERIAL);
            return false;
        } else if (args[0].equalsIgnoreCase("everything")) {

            sellEverything(player, args.length == 2 && args[1].equalsIgnoreCase("confirm"));

        } else {

            if (MarketManager.getInstance().getItem(args[0]) == null) {
                Lang.get().message(player, Message.SELLALL_ERROR_WRONG_MATERIAL);
                return false;
            }

            PlayerInventory inventory = player.getInventory();

            HashMap<Item, Integer> items = new HashMap<>();
            Item item = MarketManager.getInstance().getItem(args[0]);

            for(ItemStack itemStack : inventory) {

                if(itemStack != null && itemStack.getType().toString().equalsIgnoreCase(args[0])) {

                    if(MarketManager.getInstance().isAValidItem(itemStack)) {

                        if(items.get(item) != null) {

                            items.put(item, items.get(item) + itemStack.getAmount());

                        } else {

                            items.put(item, itemStack.getAmount());

                        }
                    }
                }
            }

            if(items.get(item) != null) {

                if(args.length == 2 && args[1].equalsIgnoreCase("confirm")) {
                    item.sell(items.get(item), player.getUniqueId(), true);
                    return false;
                }

                String formattedValue =  Formatter.format(item.getPrice().getProjectedCost(items.get(item)*item.getMultiplier(), item.getPrice().getSellTaxMultiplier()), Style.ROUND_BASIC);

                TextComponent component = (TextComponent) MiniMessage.miniMessage().deserialize(
                        Lang.get().message(Message.CLICK_TO_CONFIRM)
                );

                Component hoverText = MiniMessage.miniMessage().deserialize(
                        Lang.get().message(Message.SELLALL_ESTIMATED_VALUE, formattedValue, "0", "0") +
                                Lang.get().message(Message.LIST_SEGMENT, formattedValue, String.valueOf(items.get(item)), item.getName())
                );

                component = component.hoverEvent(HoverEvent.showText(hoverText))
                        .clickEvent(ClickEvent.runCommand("/nsellall " + item.getIdentifier() + " confirm"));

                Lang.get().getAudience().player(player).sendMessage(component);

            } else {
                Lang.get().message(player, Message.SELLALL_ERROR_WITHOUT_ITEM, "0", "0", item.getName());
            }
        }
        return false;
    }

    public void sellEverything(Player player, boolean confirmed) {

        if (player.getInventory().getContents() == null) return;

        HashMap<Item, Float> items = new HashMap<>();

        for (ItemStack itemStack : player.getInventory()) {

            if (itemStack != null && !itemStack.getType().equals(Material.AIR)) {
                Item item = MarketManager.getInstance().getItem(itemStack);

                if (item == null) continue;

                Item parent = item.isParent() ? item : item.getParent();

                if (items.containsKey(parent)) {
                    items.put(parent, items.get(parent) + itemStack.getAmount() * item.getMultiplier());
                } else {
                    items.put(parent, itemStack.getAmount() * item.getMultiplier());
                }
            }
        }

        if (items.isEmpty()) {
            Lang.get().message(player, Message.SELLALL_EVERYTHING_ERROR);
            return;
        }

        HashMap<Item, Integer> content = new HashMap<>();

        for (ItemStack itemStack : player.getInventory().getContents()) {

            if (itemStack == null) continue;

            Item item = MarketManager.getInstance().getItem(itemStack);

            if (item == null) continue;

            if (content.containsKey(item)) {
                content.put(item, content.get(item) + itemStack.getAmount());
            } else {
                content.put(item, itemStack.getAmount());
            }
        }

        float totalValue = 0;

        if (confirmed) {

            for (Item item : content.keySet()) {
                item.sell(content.get(item), player.getUniqueId(), true);
            }

        } else {

            String text = "";

            for (Item item : content.keySet()) {
                float value = item.getPrice().getProjectedCost(content.get(item) * item.getMultiplier(), item.getPrice().getSellTaxMultiplier());
                totalValue += value;
                text = text + Lang.get().message(Message.LIST_SEGMENT, Formatter.format(value, Style.ROUND_BASIC), String.valueOf(content.get(item)), item.getName());
            }

            text = text + "\n";

            TextComponent component = (TextComponent) MiniMessage.miniMessage().deserialize(
                    Lang.get().message(Message.CLICK_TO_CONFIRM)
            );

            Component hoverText = MiniMessage.miniMessage().deserialize(
                    Lang.get().message(Message.SELLALL_ESTIMATED_VALUE, Formatter.format(totalValue, Style.ROUND_BASIC), "0", "0") +
                            text
            );

            component = component.hoverEvent(HoverEvent.showText(hoverText))
                    .clickEvent(ClickEvent.runCommand("/nsellall everything confirm"));

            Lang.get().getAudience().player(player).sendMessage(component);
        }
    }
}
