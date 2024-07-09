package me.bounser.nascraft.commands.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.discord.inventories.DiscordInventories;
import me.bounser.nascraft.discord.inventories.DiscordInventory;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.managers.MoneyManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class DiscordInventoryInGame implements Listener {

    private static DiscordInventoryInGame instance = null;

    public static DiscordInventoryInGame getInstance() { return instance == null ? new DiscordInventoryInGame() : instance; }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {

        if (!event.getWhoClicked().hasMetadata("NascraftDiscordInventory")) return;

        if (event.getClickedInventory() == null || event.getView().getTopInventory().getSize() != 45) { return; }

        if (event.getClickedInventory().getSize() == 45 || event.isShiftClick() || event.isRightClick()) { event.setCancelled(true); }

        if (!event.getAction().equals(InventoryAction.PICKUP_ALL) && !event.getAction().equals(InventoryAction.PLACE_ALL)) { event.setCancelled(true); return; }

        DiscordInventory discordInventory = DiscordInventories.getInstance().getInventory(event.getWhoClicked().getUniqueId());

        if (event.getClickedInventory().getSize() == 45 && event.getCurrentItem() != null && event.getRawSlot() > 8+discordInventory.getCapacity() && event.getRawSlot() < 40) {
            if (MoneyManager.getInstance().hasEnoughMoney((OfflinePlayer) event.getWhoClicked(), discordInventory.getNextSlotPrice())) {
                Nascraft.getEconomy().withdrawPlayer((OfflinePlayer) event.getView().getPlayer(), discordInventory.getNextSlotPrice());
                discordInventory.increaseCapacity();
            } else {
                Lang.get().message((Player) event.getWhoClicked(), Message.DISINV_CANT_AFFORD_EXPANSION);
            }
            return;
        }

        if (event.getClickedInventory().getSize() == 45 &&
                event.getRawSlot() > 8 &&
                event.getRawSlot() < 40 &&
                (event.getCurrentItem() != null && event.getCursor().getType().equals(Material.AIR))) {

            List<Item> items = new ArrayList<>(discordInventory.getContent().keySet());

            Item item = items.get(event.getRawSlot()-9);

            if (item == null) return;

            int quantity = discordInventory.getContent().get(item);

            if (quantity <= event.getCurrentItem().getType().getMaxStackSize()) {
                discordInventory.removeItem(item, quantity);
                event.getWhoClicked().setItemOnCursor(item.getItemStack(quantity));
            } else {
                discordInventory.removeItem(item, event.getCurrentItem().getType().getMaxStackSize());
                event.getWhoClicked().setItemOnCursor(item.getItemStack(event.getCurrentItem().getType().getMaxStackSize()));
            }
            return;
        }

        if (event.getClickedInventory().getSize() == 45 && (event.getCursor() != null) && MarketManager.getInstance().isAValidItem(event.getCursor())) {

            Item item = MarketManager.getInstance().getItem(event.getCursor());

            if (item == null) { return; }

            int amount = event.getCursor().getAmount();

            event.getWhoClicked().setItemOnCursor(null);
            discordInventory.addItem(item, amount);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getSize() == 45 && event.getView().getTitle().equals(Lang.get().message(Message.DISINV_TITLE))) { event.setCancelled(true); }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer().hasMetadata("NascraftDiscordInventory")) {
            event.getPlayer().removeMetadata("NascraftDiscordInventory", Nascraft.getInstance());
        }
    }

    public void updateDiscordInventory(Player player) {

        if (!player.hasMetadata("NascraftDiscordInventory")) return;

        Inventory inventory = player.getOpenInventory().getTopInventory();

        inventory.clear();

        insertFillers(inventory);
        insertDiscordHead(inventory, player.getUniqueId());
        insertLockedSpaces(inventory, player.getUniqueId());
        insertDiscordInventoryContent(inventory, player.getUniqueId());
    }

    public void insertFillers(Inventory inventory) {

        ItemStack filler = new ItemStack(Config.getInstance().getDiscordInvFillersMaterial());
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for(int i : new int[]{0, 1, 2, 3, 5, 6, 7, 8, 40, 41, 42, 43, 44}) {
            inventory.setItem(i, filler);
        }
    }

    public void insertDiscordHead(Inventory inventory, UUID uuid) {

        String TEXTURE = Config.getInstance().getDiscordInvInfoTexture();

        PlayerProfile profile = getProfile(TEXTURE);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.DISINV_INFO_TITLE));
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(title));

        List<String> lore = new ArrayList<>();
        for (String line : Lang.get().message(Message.DISINV_INFO_LORE, "0", Formatter.format(DiscordInventories.getInstance().getInventory(uuid).getInventoryValue(), Style.ROUND_BASIC), "0").split("\\n")) {
            Component loreComponent = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
        }

        meta.setLore(lore);
        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);

        inventory.setItem(Config.getInstance().getDiscordInvInfoSlot(), head);
    }

    public void insertLockedSpaces(Inventory inventory, UUID uuid) {

        ItemStack filler = new ItemStack(Config.getInstance().getDiscordInvLockedMaterial());
        ItemMeta meta = filler.getItemMeta();

        Component lockName = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.DISINV_LOCKED_TITLE));
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(lockName));

        List<String> lore = new ArrayList<>();
        for (String line : Lang.get().message(Message.DISINV_LOCKED_LORE, "0", Formatter.format(DiscordInventories.getInstance().getInventory(uuid).getNextSlotPrice(), Style.ROUND_BASIC), "0").split("\\n")) {
            Component loreComponent = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
        }

        meta.setLore(lore);
        filler.setItemMeta(meta);

        for(int i = 9 + DatabaseManager.get().getDatabase().retrieveCapacity(uuid); i < 40 ; i++) {
            inventory.setItem(i, filler);
        }
    }

    public void insertDiscordInventoryContent(Inventory inventory, UUID uuid) {

        HashMap<Item, Integer> content = DiscordInventories.getInstance().getInventory(uuid).getContent();

        int i = 9;
        for (Item item : content.keySet()) {
            if (item == null) {
                i++; continue;
            }
            ItemStack itemStack = item.getItemStack();
            ItemMeta meta = itemStack.getItemMeta();

            List<String> finalLore = new ArrayList<>();

            if (meta.hasLore()) {

                List<String> lore = meta.getLore();

                lore.add("");
                Component loreComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.DISINV_AMOUNT, "0", String.valueOf(content.get(item)), "0"));
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));

                finalLore = lore;

            } else {

                for (String line : Lang.get().message(Message.DISINV_AMOUNT, "0", String.valueOf(content.get(item)), "0").split("\\n")) {
                    Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                    finalLore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
                }
            }

            meta.setLore(finalLore);

            itemStack.setItemMeta(meta);
            inventory.setItem(i, itemStack);
            i++;
        }
    }

    private static PlayerProfile getProfile(String texture) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();
        URL urlObject;
        try {
            urlObject = new URL("https://textures.minecraft.net/texture/" + texture);
        } catch (MalformedURLException exception) {
            throw new RuntimeException("Invalid URL", exception);
        }
        textures.setSkin(urlObject);
        profile.setTextures(textures);
        return profile;
    }

}