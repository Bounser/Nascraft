package me.bounser.nascraft.sellwand;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.managers.MoneyManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WandListener implements Listener {

    private final HashMap<Wand, HashMap<Player, Instant>> onCooldown = new HashMap<>();

    public WandListener() {
        for (Wand wand : WandsManager.getInstance().getWands().values())
            onCooldown.put(wand, new HashMap<>());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onContainerClick(PlayerInteractEvent event) {

        if (event.isCancelled()) return;

        event.useInteractedBlock();

        if (!event.getClickedBlock().getType().equals(Material.CHEST) &&
            !event.getClickedBlock().getType().equals(Material.BARREL) &&
            !event.getClickedBlock().getType().equals(Material.TRAPPED_CHEST)) return;

        if (event.getItem() == null) return;

        ItemMeta meta = event.getItem().getItemMeta();

        if (meta == null) return;

        NamespacedKey keyType = new NamespacedKey(Nascraft.getInstance(), "wand-type");

        if (meta.getPersistentDataContainer().has(keyType)) {

            Wand wand = WandsManager.getInstance().getWands().get(meta.getPersistentDataContainer().get(keyType, PersistentDataType.STRING));

            if (wand == null) return;

            event.setCancelled(true);

            if (wand.getPermission() != null) {
                if (!event.getPlayer().hasPermission(wand.getPermission())) {
                    Lang.get().message(Message.NO_PERMISSION);
                    return;
                }
            }

            if (onCooldown.get(wand).containsKey(event.getPlayer())) {

                Duration cooldown = Duration.between(onCooldown.get(wand).get(event.getPlayer()), Instant.now());

                int seconds = (int) (wand.getCooldown() - cooldown.getSeconds());

                if (seconds > 60) {
                    Lang.get().message(event.getPlayer(), Message.SELLWAND_COOLDOWN, "[TIME]",
                            Lang.get().message(Message.SELLWAND_MINUTES)
                                    .replace("[MINUTES]", String.valueOf(seconds/60))
                                    .replace("[SECONDS]", String.valueOf(seconds%60)));
                } else {
                    Lang.get().message(event.getPlayer(), Message.SELLWAND_COOLDOWN, "[TIME]",
                            Lang.get().message(Message.SELLWAND_SECONDS)
                                    .replace("[SECONDS]", String.valueOf(seconds)));
                }

                return;
            }

            Inventory inventory = getInventory(event.getClickedBlock());

            float expectedWorth = 0;

            HashMap<Item, Float> valuableItems = new HashMap<>();

            // ESTIMATE
            if (wand.getEstimateAction() != null && event.getAction().equals(wand.getEstimateAction())) {

                for (ItemStack itemStack : inventory.getContents()) {

                    if (itemStack == null) continue;

                    Item item = MarketManager.getInstance().getItem(itemStack);

                    if (item == null) continue;

                    Item parent = item.isParent() ? item : item.getParent();

                    if (valuableItems.containsKey(parent)) {
                        valuableItems.put(parent, valuableItems.get(parent) + itemStack.getAmount() * item.getMultiplier());
                    } else {
                        valuableItems.put(parent, itemStack.getAmount() * item.getMultiplier());
                    }
                }

                for (Item item : valuableItems.keySet())
                    expectedWorth += item.getPrice().getProjectedCost(valuableItems.get(item), item.getPrice().getSellTaxMultiplier());

                expectedWorth *= wand.getMultiplier();

                Lang.get().message(event.getPlayer(), Message.SELLWAND_ESTIMATED_VALUE, Formatter.format(expectedWorth, Style.ROUND_BASIC), "0", "0");

                return;
            }

            ////////////////////////////////////////////////////////////////

            // SELL
            if (wand.getSellAction() != null &&  event.getAction().equals(wand.getSellAction())) {

                NamespacedKey keyUses = new NamespacedKey(Nascraft.getInstance(), "wand-uses");

                int uses = -1;

                if (meta.getPersistentDataContainer().has(keyUses)) {

                    uses = meta.getPersistentDataContainer().get(keyUses, PersistentDataType.INTEGER);

                    if (uses == 0) {
                        Lang.get().message(event.getPlayer(), Message.SELLWAND_RAN_OUT);
                        return;
                    }
                }

                NamespacedKey keyMaxProfit = new NamespacedKey(Nascraft.getInstance(), "wand-max-profit");

                float maxProfitLeft = -1;

                if (meta.getPersistentDataContainer().has(keyMaxProfit)) {

                    maxProfitLeft = meta.getPersistentDataContainer().get(keyMaxProfit, PersistentDataType.FLOAT);

                    if (maxProfitLeft <= 0 && maxProfitLeft != -1) {
                        Lang.get().message(event.getPlayer(), Message.SELLWAND_TOO_MUCH);
                        return;
                    }
                }

                float expected = getWorthOfInventory(inventory);

                if (maxProfitLeft != -1 && maxProfitLeft < expected*wand.getMultiplier()) {
                    Lang.get().message(event.getPlayer(), Message.SELLWAND_TOO_MUCH);
                    return;
                }

                float totalWorth = 0;

                for (ItemStack itemStack : inventory.getContents()) {

                    if (MarketManager.getInstance().isAValidItem(itemStack)) {

                        Item item = MarketManager.getInstance().getItem(itemStack);

                        totalWorth += item.sell(itemStack.getAmount(), event.getPlayer().getUniqueId(), false);

                        itemStack.setAmount(0);
                    }
                }

                if (totalWorth == 0) {
                    Lang.get().message(event.getPlayer(), Message.SELLWAND_NOTHING_TO_SELL);
                    return;
                }

                if (uses != -1) {
                    uses--;
                    meta.getPersistentDataContainer().set(keyUses, PersistentDataType.INTEGER, uses);
                }

                if (maxProfitLeft != -1) {
                    maxProfitLeft -= totalWorth;
                    if (maxProfitLeft < 0) {
                        Lang.get().message(event.getPlayer(), Message.SELLWAND_TOO_MUCH);
                        return;
                    }
                    meta.getPersistentDataContainer().set(keyMaxProfit, PersistentDataType.FLOAT, maxProfitLeft);
                }

                meta.setLore(wand.getLore(uses, maxProfitLeft));

                event.getItem().setItemMeta(meta);

                if (wand.getMultiplier() != 1) {

                    float result = totalWorth * wand.getMultiplier() - totalWorth;

                    if (result > 0) {
                        MoneyManager.getInstance().deposit(event.getPlayer(), result, 0);
                    } else {
                        MoneyManager.getInstance().withdraw(event.getPlayer(), Math.abs(result), 0);
                    }

                    Lang.get().message(event.getPlayer(), Message.SELLWAND_SOLD_WITH_MULTIPLIER, "[INITIAL-WORTH]", Formatter.format(totalWorth, Style.ROUND_BASIC), "[MULTIPLIER]", String.valueOf(wand.getMultiplier()), "[WORTH]", Formatter.format(totalWorth * wand.getMultiplier(), Style.ROUND_BASIC));
                } else {
                    Lang.get().message(event.getPlayer(), Message.SELLWAND_SOLD, Formatter.format(totalWorth, Style.ROUND_BASIC), "0", "0");
                }

                if (wand.getCooldown() > 0) {
                    HashMap<Player, Instant> players = onCooldown.get(wand);
                    players.put(event.getPlayer(), Instant.now());
                    onCooldown.put(wand, players);

                    Bukkit.getScheduler().runTaskLaterAsynchronously(Nascraft.getInstance(), () -> {
                        HashMap<Player, Instant> players1 = onCooldown.get(wand);
                        players1.remove(event.getPlayer());
                        onCooldown.put(wand, players1);
                    }, 20L * wand.getCooldown());
                }
            }
        }
    }

    public Inventory getInventory(Block block) {

        Inventory inventory = null;

        switch (block.getType()) {

            case CHEST:
            case TRAPPED_CHEST:

                Chest chest = (Chest) block.getState();
                inventory = chest.getInventory();
                break;

            case BARREL:

                Barrel barrel = (Barrel) block.getState();
                inventory = barrel.getInventory();
                break;
        }
        return inventory;
    }

    public float getWorthOfInventory(Inventory inventory) {

        HashMap<Item, Float> content = new HashMap<>();

        for (ItemStack itemStack : inventory.getContents()) {

            if (itemStack == null) continue;

            Item item = MarketManager.getInstance().getItem(itemStack);

            if (item == null) continue;

            Item parent = item.isParent() ? item : item.getParent();

            if (content.containsKey(parent)) {
                content.put(parent, content.get(parent) + itemStack.getAmount() * item.getMultiplier());
            } else {
                content.put(parent, itemStack.getAmount() * item.getMultiplier());
            }
        }

        float worth = 0;

        for (Item item : content.keySet())
            worth += item.getPrice().getProjectedCost(content.get(item) * item.getMultiplier(), item.getPrice().getSellTaxMultiplier());

        return worth;
    }
}
