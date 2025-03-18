package me.bounser.nascraft.commands.admin.nascraft;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.Database;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class NascraftLogListener implements Listener {

    @EventHandler
    public void onInventoryClickEvent(InventoryClickEvent event) {

        if (event.getWhoClicked().hasMetadata("NascraftLogInventory")) {

            event.setCancelled(true);

            if (event.getRawSlot() != 0 && event.getRawSlot() != 8)
                return;

            if (event.getCurrentItem().getType().equals(Material.ARROW)) {

                String mode = event.getWhoClicked().getMetadata("NascraftLogInventory").get(0).asString();;

                int page = event.getWhoClicked().getMetadata("NascraftLogInventoryPage").get(0).asInt();

                switch (event.getRawSlot()) {

                    case 0:

                        event.getWhoClicked().setMetadata("NascraftLogInventoryPage", new FixedMetadataValue(Nascraft.getInstance(), page - 1));

                        if (mode.equals("global")) {
                            updateTradePage(event.getInventory(), page - 1, null, null);
                        } else if (mode.startsWith("item-")) {
                            updateTradePage(event.getInventory(), page - 1, MarketManager.getInstance().getItem(mode.substring(5)), null);
                        } else if (mode.startsWith("uuid-")){
                            updateTradePage(event.getInventory(), page - 1, null, UUID.fromString(mode.substring(5)));
                        }

                        break;

                    case 8:

                        event.getWhoClicked().setMetadata("NascraftLogInventoryPage", new FixedMetadataValue(Nascraft.getInstance(), page + 1));

                        if (mode.equals("global")) {
                            updateTradePage(event.getInventory(), page +1, null, null);
                        } else if (mode.startsWith("item-")) {
                            updateTradePage(event.getInventory(), page +1, MarketManager.getInstance().getItem(mode.substring(5)), null);
                        } else if (mode.startsWith("uuid-")){
                            updateTradePage(event.getInventory(), page +1, null, UUID.fromString(mode.substring(5)));
                        }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryCloseEvent(InventoryCloseEvent event) {
        if (event.getPlayer().hasMetadata("NascraftLogInventory")) {
            event.getPlayer().removeMetadata("NascraftLogInventory", Nascraft.getInstance());
            event.getPlayer().removeMetadata("NascraftLogInventoryPage", Nascraft.getInstance());
        }
    }

    public static void createTradePage(Player player, Item item, UUID uuid) {

        Inventory logsGUI = Bukkit.createInventory(null, 54, "Log");

        if (uuid == null && item == null) {
            ItemStack itemStack = new ItemStack(Material.CHEST);

            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Global trades");
            itemStack.setItemMeta(meta);

            logsGUI.setItem(4, itemStack);

        } else if (uuid != null) {
            ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);

            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(ChatColor.BLUE + "Trades of: " + DatabaseManager.get().getDatabase().getNameByUUID(uuid) + " (" + uuid + ")");
            itemStack.setItemMeta(meta);

            logsGUI.setItem(4, itemStack);

        } else {
            ItemStack itemStack = item.getItemStack();

            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(ChatColor.BLUE + "Trades with: " + item.getFormattedName());
            itemStack.setItemMeta(meta);

            logsGUI.setItem(4, itemStack);
        }

        for (int i : Arrays.asList(0, 1, 2, 3, 5, 6, 7, 8))
            logsGUI.setItem(i, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));

        updateTradePage(logsGUI, 0, item, uuid);

        player.openInventory(logsGUI);
    }

    public static void updateTradePage(Inventory logsGUI, int page, Item item, UUID uuid) {

        List<Trade> trades;

        if (uuid == null && item == null) {
            trades = DatabaseManager.get().getDatabase().retrieveTrades(page * 45, 46);
        } else if (uuid != null) {
            trades = DatabaseManager.get().getDatabase().retrieveTrades(uuid, page * 45, 46);
        } else {
            trades = DatabaseManager.get().getDatabase().retrieveTrades(item, page * 45, 46);
        }

        if (page > 0) {

            ItemStack itemStack = new ItemStack(Material.ARROW);

            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(ChatColor.BLUE + "Previous Page");
            itemStack.setItemMeta(meta);

            logsGUI.setItem(0, itemStack);
        } else {
            logsGUI.setItem(0, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }

        if (trades.size() == 46) {

            ItemStack itemStack = new ItemStack(Material.ARROW);

            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(ChatColor.BLUE + "Next Page");
            itemStack.setItemMeta(meta);

            logsGUI.setItem(8, itemStack);
        } else {
            logsGUI.setItem(8, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        }

        for (int i = 0; i <= 44; i++) {

            if (trades.size() >= i+1) {

                Trade trade = trades.get(i);

                if (trade.getItem() == null) {
                    logsGUI.setItem(i+9, new ItemStack(Material.AIR));
                    continue;
                }

                ItemStack itemStack = trade.getItem().getItemStack(Math.min(trade.getAmount(), 64));

                ItemMeta meta = itemStack.getItemMeta();

                if (meta == null) continue;

                if (trade.isBuy()) {
                    meta.setDisplayName(ChatColor.GREEN + "BUY: " + trade.getItem().getFormattedName());
                } else {
                    meta.setDisplayName(ChatColor.RED + "SELL: " + trade.getItem().getFormattedName());
                }

                List<String> lore = new ArrayList<>();

                lore.add(ChatColor.BLUE + "Player: " + DatabaseManager.get().getDatabase().getNameByUUID(trade.getUuid()) +  " (" + trade.getUuid() + ")");
                lore.add("");

                if (trade.getAmount() > 64) {
                    lore.add(ChatColor.BLUE + "Quantity: " + trade.getAmount());
                    lore.add("");
                }

                String price = ChatColor.BLUE + (trade.isBuy() ? "Price paid: " : "Price received: ");

                if (trade.getAmount() == 1) price += BukkitComponentSerializer.legacy().serialize(MiniMessage.miniMessage().deserialize(Formatter.format(trade.getItem().getCurrency(), trade.getValue(), Style.ROUND_BASIC)));
                else price += ChatColor.GREEN + BukkitComponentSerializer.legacy().serialize(MiniMessage.miniMessage().deserialize(Formatter.format(trade.getItem().getCurrency(), trade.getValue(), Style.ROUND_BASIC))) + ChatColor.BLUE + " → " + BukkitComponentSerializer.legacy().serialize(MiniMessage.miniMessage().deserialize(Formatter.format(trade.getItem().getCurrency(), trade.getValue()/trade.getAmount(), Style.ROUND_BASIC))) + ChatColor.BLUE + " each";

                lore.add(price);

                lore.add("");
                lore.add(ChatColor.BLUE + getFormattedTime(trade.getDate()));

                meta.setLore(lore);

                itemStack.setItemMeta(meta);

                logsGUI.setItem(i+9, itemStack);

            } else {
                logsGUI.setItem(i+9, new ItemStack(Material.AIR));
            }
        }

        for (HumanEntity viewer : logsGUI.getViewers()) {
            viewer.openInventory(logsGUI);

            viewer.setMetadata("NascraftLogInventoryPage", new FixedMetadataValue(Nascraft.getInstance(), page));

            if (item == null && uuid == null) {
                viewer.setMetadata("NascraftLogInventory", new FixedMetadataValue(Nascraft.getInstance(), "global"));
            } else if (item != null) {
                viewer.setMetadata("NascraftLogInventory", new FixedMetadataValue(Nascraft.getInstance(), "item-" + item.getIdentifier()));
            } else if (uuid != null){
                viewer.setMetadata("NascraftLogInventory", new FixedMetadataValue(Nascraft.getInstance(), "uuid-" + uuid));
            }
        }
    }

    public static String getFormattedTime(LocalDateTime date) {

        LocalDateTime now = LocalDateTime.now();

        Duration duration = Duration.between(date, now);

        long days = duration.toDays();
        duration = duration.minusDays(days);

        long hours = duration.toHours();
        duration = duration.minusHours(hours);

        long minutes = duration.toMinutes();
        duration = duration.minusMinutes(minutes);

        long seconds = duration.getSeconds();

        StringBuilder result = new StringBuilder();

        if (days > 0) {
            result.append(days).append(" day").append(days != 1 ? "s" : "").append(", ");
        }
        if (hours > 0) {
            result.append(hours).append(" hour").append(hours != 1 ? "s" : "").append(", ");
        }
        if (minutes > 0) {
            result.append(minutes).append(" min").append(minutes != 1 ? "s" : "").append(", ");
        }
        if (seconds > 0) {
            result.append(seconds).append(" second").append(seconds != 1 ? "s" : "").append(", ");
        }

        if (result.length() > 0) {
            result.setLength(result.length() - 2);
            result.append(" ago");
        } else {
            result.append("just now");
        }

        return result.toString();
    }
}
