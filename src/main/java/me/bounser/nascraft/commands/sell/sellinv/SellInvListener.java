package me.bounser.nascraft.commands.sell.sellinv;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.managers.InventoryManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.managers.currencies.Currency;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;


public class SellInvListener implements Listener {

    private final HashMap<Player, List<ItemStack>> playerItems = new HashMap<>();

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {

        Player player = (Player) event.getWhoClicked();

        if (!player.hasMetadata("NascraftSell")) return;

        event.setCancelled(true);

        if (event.getClickedInventory() != null && event.getClickedInventory().getType().equals(InventoryType.PLAYER)) {

            if (!MarketManager.getInstance().getActive()) {
                Lang.get().message(player, Message.SHOP_CLOSED);
                return;
            }

            ItemStack itemClicked = event.getCurrentItem();

            if (itemClicked == null) return;

            if (playerItems.get(player) != null && playerItems.get(player).size() >= 27) {
                Lang.get().message(player, Message.SELL_FULL);
                return;
            }

            Item item = MarketManager.getInstance().getItem(itemClicked);

            if (item != null) {

                int totalChange = itemClicked.getAmount();

                ItemStack itemItemStack = item.getItemStack();

                if (playerItems.containsKey(player) && !playerItems.get(player).isEmpty())
                    for (ItemStack itemStack : playerItems.get(player))
                        if (MarketManager.getInstance().isSimilarEnough(itemItemStack, itemStack))
                            totalChange += itemStack.getAmount();


                if (!item.getPrice().canStockChange(totalChange, false) && item.isPriceRestricted()) {
                    Lang.get().message(player, Message.BOTTOM_LIMIT_REACHED);
                    return;
                }

                List<ItemStack> items = playerItems.get(player);

                if (items == null) items = new ArrayList<>();

                float totalAmount = itemClicked.getAmount();

                if (items.contains(item.getItemStack())) {

                    for (ItemStack itemStack : items)
                        if (itemStack.isSimilar(item.getItemStack()))
                            totalAmount += itemStack.getAmount();

                    if (!item.getPrice().canStockChange(totalAmount, false) && item.isPriceRestricted()) {
                        Lang.get().message(player, Message.BOTTOM_LIMIT_REACHED);
                        return;
                    }
                }

                if (event.isShiftClick()) {

                    for (int i = 0; i < event.getClickedInventory().getSize(); i++) {
                        ItemStack itemStack = event.getClickedInventory().getItem(i);

                        if (itemStack != null && itemStack.isSimilar(itemClicked)) {

                            items.add(itemStack);
                            event.getClickedInventory().setItem(i, new ItemStack(Material.AIR));

                        }
                        if (items.size() >= 27) break;
                    }

                    playerItems.put(player, items);

                } else {
                    items.add(event.getCurrentItem());

                    playerItems.put(player, items);
                    event.getClickedInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));
                }

                renderInv(event.getView().getTopInventory(), player);
                return;
            }

            Lang.get().message(player, Message.SELL_ITEM_NOT_ALLOWED);

        } else {

            switch (event.getRawSlot()) {

                case 8:

                    event.getWhoClicked().closeInventory(); break;

                case 40:

                    if (!MarketManager.getInstance().getActive()) {
                        Lang.get().message(player, Message.SHOP_CLOSED);
                        return;
                    }

                    if (playerItems.isEmpty()) return;

                    HashMap<Currency, Double> result = new HashMap<>();

                    List<ItemStack> newPlayerItems = new ArrayList<>();

                    for (ItemStack itemStack : playerItems.get(player)) {

                        Item item = MarketManager.getInstance().getItem(itemStack);

                        if (item.getPrice().canStockChange(itemStack.getAmount(), false) || !item.isPriceRestricted()) {
                            double value = item.sell(itemStack.getAmount(), player.getUniqueId(), false);

                            if (result.containsKey(item.getCurrency())) {
                                double tempValue = result.get(item.getCurrency());
                                tempValue += value;
                                result.put(item.getCurrency(), tempValue);
                            } else {
                                result.put(item.getCurrency(), value);
                            }

                        } else {
                            newPlayerItems.add(itemStack);
                        }
                    }

                    if (newPlayerItems.isEmpty()) playerItems.remove(player);
                    else playerItems.put(player, newPlayerItems);

                    renderInv(event.getClickedInventory(), player);

                    String report = "";

                    int i = 0;

                    for (Currency currency : CurrenciesManager.getInstance().getCurrencies()) {
                        if (result.get(currency) == null) continue;
                        i++;
                        report += Formatter.format(currency, result.get(currency), Style.ROUND_BASIC) + (i == result.size() ? "" : ",");
                    }

                    Lang.get().message(player, Message.SELL_ACTION_MESSAGE, report, "", "");

                    break;

                default:

                    if (8 >= event.getRawSlot() || 36 <= event.getRawSlot()) return;

                    List<ItemStack> items = playerItems.get(player);

                    ItemStack item = event.getCurrentItem();

                    if (item == null) return;

                    ItemMeta meta = item.getItemMeta();

                    if (meta != null && meta.hasLore()) {
                        if (meta.getLore().size() > 2) meta.setLore(meta.getLore().subList(0, meta.getLore().size()-2));
                        else meta.setLore(null);
                    }

                    item.setItemMeta(meta);

                    items.remove(item);
                    playerItems.put(player, items);

                    InventoryManager.addItemsToInventory(player, event.getCurrentItem(), event.getCurrentItem().getAmount());

                    renderInv(event.getView().getTopInventory(), player);

                    break;
            }
        }
    }

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent event) {

        Player player = (Player) event.getPlayer();

        if (player.hasMetadata("NascraftSell")) {
            player.removeMetadata("NascraftSell", Nascraft.getInstance());

            if (playerItems.containsKey(player)) {
                for (ItemStack itemStack : playerItems.get(event.getPlayer()))
                    InventoryManager.addItemsToInventory(player, itemStack, itemStack.getAmount());

                playerItems.remove(event.getPlayer());
            }
        }
    }

    public void renderInv(Inventory inventory, Player player) {

        updateSellButton(inventory, player);

        if (playerItems.get(player) == null || playerItems.get(player).isEmpty()) {
            for (int i = 9 ; i <= 35 ; i++) {
                    inventory.setItem(i, new ItemStack(Material.AIR));
            }
            return;
        }

        List<Material> materials = new ArrayList<>();

        for (ItemStack itemstack : playerItems.get(player)) { materials.add(itemstack.getType()); }

        Collections.sort(materials);

        List<ItemStack> items = new ArrayList<>(playerItems.get(player));

        for (int i = 9 ; i <= 35 ; i++) {
            if (materials.size() != 0 && materials.get(0) != null) {

                ItemStack item = null;

                for (ItemStack itemStack : items) {
                    if (itemStack != null && itemStack.getType().equals(materials.get(0))) {
                        item = itemStack;
                    }
                }
                items.remove(item);

                inventory.setItem(i, getDisplayClonedItem(item));
                materials.remove(0);

            } else {
                inventory.setItem(i, new ItemStack(Material.AIR));
            }
        }
    }

    public ItemStack getDisplayClonedItem(ItemStack itemStack) {

        ItemStack clonedItem = itemStack.clone();

        ItemMeta meta = clonedItem.getItemMeta();

        Component remove = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.SELL_REMOVE_ITEM));
        String removeLore = BukkitComponentSerializer.legacy().serialize(remove);

        if (meta.hasLore()) {

            List<String> lore = meta.getLore();

            lore.add("");
            lore.add(removeLore);

            meta.setLore(lore);

        } else {
            meta.setLore(Arrays.asList("", removeLore));
        }

        clonedItem.setItemMeta(meta);

        return clonedItem;
    }

    public void updateSellButton(Inventory inventory, Player player) {

        ItemStack sellButton = inventory.getItem(40);

        ItemMeta meta = sellButton.getItemMeta();

        String result = "";

        HashMap<Currency, Double> invResult = getSellInventoryValue(player);

        for (Currency currency : invResult.keySet()) {
            if (invResult.get(currency) > 0)
                result += Formatter.format(currency, invResult.get(currency), Style.ROUND_BASIC) + "\n";
        }

        List<String> lore = new ArrayList<>();
        for (String line : Lang.get().message(Message.SELL_BUTTON_LORE, "[WORTH-LIST]", result).split("\\n")) {
            Component loreLine = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(loreLine));
        }

        meta.setLore(lore);

        sellButton.setItemMeta(meta);

        inventory.setItem(40, sellButton);
    }

    public HashMap<Currency, Double> getSellInventoryValue(Player player) {

        HashMap<Currency, Double> valuePerCurrency = new HashMap<>();

        for (Currency currency : CurrenciesManager.getInstance().getCurrencies())
            valuePerCurrency.put(currency, 0d);

        if (playerItems.get(player) == null) return valuePerCurrency;

        HashMap<Item, Integer> content = new HashMap<>();

        for (ItemStack itemStack : playerItems.get(player)) {

            Item item = MarketManager.getInstance().getItem(itemStack);

            content.compute(item, (key, value) -> (value == null) ? itemStack.getAmount() : value + itemStack.getAmount());
        }

        for (Item item : content.keySet()) {
            double internalValue = valuePerCurrency.get(item.getCurrency());
            internalValue += item.sellPrice(content.get(item));
            valuePerCurrency.put(item.getCurrency(), internalValue);
        }

        return valuePerCurrency;
    }
}
