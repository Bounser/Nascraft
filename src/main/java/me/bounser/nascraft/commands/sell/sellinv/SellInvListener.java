package me.bounser.nascraft.commands.sell.sellinv;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
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

        if (event.getClickedInventory().getType() == InventoryType.PLAYER) {

            ItemStack itemClicked = event.getCurrentItem();

            if (itemClicked == null) return;

            if (playerItems.get(player) != null && playerItems.get(player).size() >= 27) {
                Lang.get().message(player, Message.SELL_FULL);
                return;
            }

            if (MarketManager.getInstance().isAValidItem(itemClicked)) {

                List<ItemStack> items = playerItems.get(player);

                if (items == null) items = new ArrayList<>();

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

                    if (playerItems.isEmpty()) return;

                    float realValue = 0;

                    for (ItemStack itemStack : playerItems.get(player)) {

                        Item item = MarketManager.getInstance().getItem(itemStack);

                        realValue += item.sell(itemStack.getAmount(), player.getUniqueId(), false);
                    }

                    playerItems.remove(player);
                    renderInv(event.getClickedInventory(), player);

                    Lang.get().message(player, Message.SELL_ACTION_MESSAGE, Formatter.format(realValue, Style.ROUND_BASIC), "", "");
                    
                    break;

                default:

                    if (8 >= event.getRawSlot() || 36 <= event.getRawSlot()) return;

                    List<ItemStack> items = playerItems.get(player);

                    ItemStack item = event.getCurrentItem();

                    ItemMeta meta = item.getItemMeta();

                    if (meta != null && meta.hasLore()) {
                        if (meta.getLore().size() > 2) meta.setLore(meta.getLore().subList(0, meta.getLore().size()-2));
                        else meta.setLore(null);
                    }

                    item.setItemMeta(meta);

                    items.remove(item);
                    playerItems.put(player, items);

                    player.getInventory().addItem(event.getCurrentItem());

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
                for (ItemStack itemStack : playerItems.get(event.getPlayer())) { event.getPlayer().getInventory().addItem(itemStack); }

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

        List<String> lore = new ArrayList<>();
        for (String line : Lang.get().message(Message.SELL_BUTTON_LORE, Formatter.format(getSellInventoryValue(player), Style.ROUND_BASIC), "", "").split("\\n")) {
            Component loreLine = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(loreLine));
        }

        meta.setLore(lore);

        sellButton.setItemMeta(meta);

        inventory.setItem(40, sellButton);
    }

    public float getSellInventoryValue(Player player) {

        if (playerItems.get(player) == null) return 0;

        HashMap<Item, Integer> content = new HashMap<>();

        for (ItemStack itemStack : playerItems.get(player)) {

            Item item = MarketManager.getInstance().getItem(itemStack);

            content.compute(item, (key, value) -> (value == null) ? itemStack.getAmount() : value + itemStack.getAmount());
        }

        float totalValue = 0;

        for (Item item : content.keySet())
            totalValue += item.sellPrice(content.get(item));

        return  RoundUtils.round(totalValue);
    }
}
