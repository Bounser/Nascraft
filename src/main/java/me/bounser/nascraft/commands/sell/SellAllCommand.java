package me.bounser.nascraft.commands.sell;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.Command;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.managers.currencies.Currency;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class SellAllCommand extends Command {

    public SellAllCommand() {
        super(
                "sellall",
                new String[]{Config.getInstance().getCommandAlias("sellall")},
                "Sell items directly to the market",
                "nascraft.sellall"
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("nascraft.sellall")) {
            Lang.get().message(player, Message.NO_PERMISSION); return;
        }

        if (args.length == 0) {

            sellEverything(player, false);

        } else if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) {

            sellEverything(player, true);

        } else {

            if (MarketManager.getInstance().getItem(args[0]) == null) {
                Lang.get().message(player, Message.SELLALL_ERROR_WRONG_MATERIAL);
                return;
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
                    return;
                }

                String formattedValue =  Formatter.format(item.getCurrency(), item.getPrice().getProjectedCost(items.get(item)*item.getMultiplier(), item.getPrice().getSellTaxMultiplier()), Style.ROUND_BASIC);

                TextComponent component = (TextComponent) MiniMessage.miniMessage().deserialize(
                        Lang.get().message(Message.CLICK_TO_CONFIRM)
                );

                Component hoverText = MiniMessage.miniMessage().deserialize(
                        Lang.get().message(Message.SELLALL_ESTIMATED_VALUE, formattedValue, "0", "0") +
                                Lang.get().message(Message.LIST_SEGMENT, formattedValue, String.valueOf(items.get(item)), item.getName())
                );

                component = component.hoverEvent(HoverEvent.showText(hoverText))
                        .clickEvent(ClickEvent.runCommand("/" + Config.getInstance().getCommandAlias("sellall") + " " + item.getIdentifier() + " confirm"));

                Lang.get().getAudience().player(player).sendMessage(component);

            } else {
                Lang.get().message(player, Message.SELLALL_ERROR_WITHOUT_ITEM, "0", "0", item.getName());
            }
        }
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

        if (confirmed) {

            HashMap<Currency, Double> value = new HashMap<>();
            int amount = 0;

            for (Item item : content.keySet()) {
                amount += content.get(item);

                if (value.containsKey(item.getCurrency())) {
                    value.put(item.getCurrency(), value.get(item.getCurrency()) + item.sell(content.get(item), player.getUniqueId(), true));
                } else {
                    value.put(item.getCurrency(), item.sell(content.get(item), player.getUniqueId(), true));
                }
            }

            String values = "";

            for (Currency currency : value.keySet()) {
                values += Formatter.format(currency, value.get(currency), Style.ROUND_BASIC) + " ";
            }

            Lang.get().message(player, Message.SELLALL_TOTAL, values, String.valueOf(amount), "");

        } else {

            String text = "";

            List<String> currencies = new ArrayList<>();

            for (Item item : content.keySet()) {
                if (!currencies.contains(item.getCurrency().getCurrencyIdentifier())) currencies.add(item.getCurrency().getCurrencyIdentifier());
            }

            HashMap<String, List<Item>> itemsPerCurrency = new HashMap<>();

            for (Item item : content.keySet())  {

                String currencyIdentifier = item.getCurrency().getCurrencyIdentifier();

                if (itemsPerCurrency.containsKey(currencyIdentifier)) {
                    List<Item> itemsOfCertainCurrency = itemsPerCurrency.get(currencyIdentifier);
                    itemsOfCertainCurrency.add(item);
                    itemsPerCurrency.put(currencyIdentifier, itemsOfCertainCurrency);
                } else {
                    List<Item> list = new ArrayList<>();
                    list.add(item);
                    itemsPerCurrency.put(currencyIdentifier, list);
                }
            }

            HashMap<String, Float> totalValuePerCurrency = new HashMap<>();

            for (String string : itemsPerCurrency.keySet()) {

                float value = 0;

                for (Item item : itemsPerCurrency.get(string)) {
                    value += item.getPrice().getProjectedCost(content.get(item) * item.getMultiplier(), item.getPrice().getSellTaxMultiplier());
                }

                totalValuePerCurrency.put(string, value);
            }

            for (Item item : content.keySet()) {
                double value = item.getPrice().getProjectedCost(content.get(item) * item.getMultiplier(), item.getPrice().getSellTaxMultiplier());
                text = text + Lang.get().message(Message.LIST_SEGMENT, Formatter.format(item.getCurrency(), value, Style.ROUND_BASIC), String.valueOf(content.get(item)), item.getName());
            }

            text = text + "\n";

            TextComponent component = (TextComponent) MiniMessage.miniMessage().deserialize(
                    Lang.get().message(Message.CLICK_TO_CONFIRM)
            );

            String perCurrencyText = "";

            for (String currency : totalValuePerCurrency.keySet()) {
                perCurrencyText += Formatter.format(CurrenciesManager.getInstance().getCurrency(currency), totalValuePerCurrency.get(currency), Style.ROUND_BASIC) + "\n";
            }

            String finalText = Lang.get().message(Message.SELLALL_ESTIMATED_VALUE).replace("[WORTH]", perCurrencyText) + text;

            Component hoverText = MiniMessage.miniMessage().deserialize(finalText);

            component = component.hoverEvent(HoverEvent.showText(hoverText))
                    .clickEvent(ClickEvent.runCommand("/" + Config.getInstance().getCommandAlias("sellall") + " confirm"));

            Lang.get().getAudience().player(player).sendMessage(component);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) return Arrays.asList("");

        Player player = ((Player) sender);

        List<Item> items = new ArrayList<>();

        for (ItemStack itemStack : player.getInventory().getContents()) {

            if (itemStack == null || itemStack.getType().equals(Material.AIR)) continue;

            Item item = MarketManager.getInstance().getItem(itemStack);

            if (item != null && !items.contains(item))
                items.add(item);

        }

        List<String> options = new ArrayList<>();

        items.forEach(item -> options.add(item.getIdentifier()));

        return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>());
    }
}