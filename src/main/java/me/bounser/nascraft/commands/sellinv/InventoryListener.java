package me.bounser.nascraft.commands.sellinv;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.RoundUtils;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
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


public class InventoryListener implements Listener {

    private HashMap<Player, List<ItemStack>> playerItems = new HashMap<>();

    private Config lang = Config.getInstance();

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {

        if (event.getView().getTopInventory().getSize() != 45 && !event.getView().getTitle().equals(lang.getSellTitle()) || event.getCurrentItem() == null) { return; }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory().getType() == InventoryType.PLAYER) {

            ItemStack itemClicked = event.getCurrentItem();

            if (itemClicked == null) return;

            if (playerItems.get(player) != null && playerItems.get(player).size() >= 26) {
                player.sendMessage(lang.getSellFullText());
                return;
            }

            if (MarketManager.getInstance().getAllMaterials().contains(itemClicked.getType().toString().toLowerCase())){
                for (String material: MarketManager.getInstance().getAllMaterials()) {
                    if (material.equalsIgnoreCase(itemClicked.getType().toString())) {

                        List<ItemStack> items = playerItems.get(player);

                        if (items == null) items = new ArrayList<>();

                        ItemMeta itemMeta = event.getCurrentItem().getItemMeta();

                        if(itemMeta.hasDisplayName() || itemMeta.hasEnchants() || itemMeta.hasLore() || itemMeta.hasAttributeModifiers() || itemMeta.hasCustomModelData()) {
                            player.sendMessage(lang.getSellItemNotAllowedText());
                            return;
                        }

                        items.add(event.getCurrentItem());

                        playerItems.put(player, items);
                        event.getClickedInventory().setItem(event.getSlot(), new ItemStack(Material.AIR));

                        renderInv(event.getView().getTopInventory(), player);
                    }
                }
                return;
            }

            player.sendMessage(lang.getSellItemNotAllowedText());

        } else {

            switch (event.getRawSlot()) {

                case 8:

                    event.getWhoClicked().closeInventory(); break;

                case 40:

                    if (playerItems.isEmpty()) return;

                    float realValue = 0;

                    for (ItemStack itemStack : playerItems.get(player)) {

                        Item item = MarketManager.getInstance().getItem(itemStack.getType().toString());

                        realValue += RoundUtils.round(item.getPrice().getSellPrice()*itemStack.getAmount());

                        item.ghostSellItem(itemStack.getAmount());
                    }

                    playerItems.remove(player);
                    renderInv(event.getClickedInventory(), player);

                    player.sendMessage(lang.getSellActionText(String.valueOf(realValue)));

                    Nascraft.getEconomy().depositPlayer(player, realValue);

                    break;

                default:

                    if (9 > event.getRawSlot() && 35 < event.getRawSlot()) return;


                    List<ItemStack> items = playerItems.get(player);

                    ItemStack item = event.getCurrentItem();

                    ItemMeta meta = item.getItemMeta();

                    meta.setDisplayName(null);

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

        if (!event.getView().getTitle().equals(lang.getSellTitle()) || playerItems.get(event.getPlayer()) == null)  return;

        for (ItemStack itemStack : playerItems.get(event.getPlayer())) { event.getPlayer().getInventory().addItem(itemStack); }

        playerItems.remove(event.getPlayer());
    }

    public void renderInv(Inventory inventory, Player player) {

        updateSellButton(inventory, player);

        if (playerItems.get(player) == null || playerItems.get(player).size() == 0) {
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

                inventory.setItem(i, getClonedItem(item));
                materials.remove(0);

            } else {
                inventory.setItem(i, new ItemStack(Material.AIR));
            }
        }
    }

    public ItemStack getClonedItem(ItemStack itemStack) {

        ItemStack clonedItem = itemStack.clone();

        ItemMeta meta = clonedItem.getItemMeta();

        meta.setDisplayName(lang.getSellRemoveItemText());

        clonedItem.setItemMeta(meta);

        return clonedItem;
    }

    public void updateSellButton(Inventory inventory, Player player) {

        ItemStack sellButton = inventory.getItem(40);

        ItemMeta meta = sellButton.getItemMeta();

        meta.setLore(Arrays.asList(lang.getSellButtonLore(String.valueOf(getSellInventoryValue(player)))));

        sellButton.setItemMeta(meta);

        inventory.setItem(40, sellButton);
    }

    public float getSellInventoryValue(Player player) {

        if (playerItems.get(player) == null) return 0;

        float totalValue = 0;

        for (ItemStack itemStack : playerItems.get(player)) {
            totalValue += MarketManager.getInstance().getItem(itemStack.getType().toString()).getPrice().getSellPrice()*itemStack.getAmount();
        }

        return  RoundUtils.round(totalValue);
    }
}
