package me.bounser.nascraft.commands.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.SQLite;
import me.bounser.nascraft.discord.inventories.DiscordInventories;
import me.bounser.nascraft.discord.inventories.DiscordInventory;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class DiscordInventoryInGame implements Listener {

    private static DiscordInventoryInGame instance = null;

    public static DiscordInventoryInGame getInstance() { return instance == null ? new DiscordInventoryInGame() : instance; }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {

        if (event.getView().getTopInventory().getSize() != 45 || !event.getView().getTitle().equals("Discord Inventory")) { return; }

        if (event.getClickedInventory().getSize() == 45 || event.isShiftClick() || event.isRightClick()) { event.setCancelled(true); }

        if (!event.getAction().equals(InventoryAction.PICKUP_ALL) && !event.getAction().equals(InventoryAction.PLACE_ALL)) { event.setCancelled(true); return; }

        DiscordInventory discordInventory = DiscordInventories.getInstance().getInventory(event.getWhoClicked().getUniqueId());

        if (event.getClickedInventory().getSize() == 45 && event.getCurrentItem() != null && event.getCurrentItem().getItemMeta().getDisplayName().equals(ChatColor.RED + "LOCKED")) {
            Nascraft.getEconomy().withdrawPlayer((OfflinePlayer) event.getView().getPlayer(), discordInventory.getNextSlotPrice());
            discordInventory.increaseCapacity();
            return;

        } else if (event.getClickedInventory().getSize() == 45 && event.getRawSlot() > 8 && event.getRawSlot() < 40 &&
                (event.getCurrentItem() != null && (event.getCurrentItem().getItemMeta().hasLore() && event.getCurrentItem().getItemMeta().getLore().get(0).contains("Amount:") && event.getCursor().getType().equals(Material.AIR)))) {

            Material material = event.getCurrentItem().getType();
            Item item = MarketManager.getInstance().getItem(material.toString());
            int quantity = discordInventory.getContent().get(item);

            if (quantity <= 64) {
                discordInventory.removeItem(item, quantity);
                event.getWhoClicked().setItemOnCursor(new ItemStack(material, quantity));
            } else {
                discordInventory.removeItem(item, 64);
                event.getWhoClicked().setItemOnCursor(new ItemStack(material, 64));
            }
            return;
        }

        if (event.getClickedInventory().getSize() == 45 && (event.getCursor() != null || event.getCursor().getType().equals(event.getCurrentItem().getType()))) {

            Item item = MarketManager.getInstance().getItem(event.getCursor().getType().toString());
            int amount = event.getCursor().getAmount();

            if (item == null) { return; }

            event.getWhoClicked().setItemOnCursor(null);
            discordInventory.addItem(item, amount);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getSize() == 45 && event.getView().getTitle().equals("Discord Inventory")) { event.setCancelled(true); }
    }

    public void updateDiscordInventory(Player player) {

        if (!player.getOpenInventory().getTitle().equals("Discord Inventory") || player.getOpenInventory().getTopInventory().getSize() != 45) return;

        Inventory inventory = player.getOpenInventory().getTopInventory();

        inventory.clear();

        insertFillers(inventory);
        insertDiscordHead(inventory, player.getUniqueId());
        insertLockedSpaces(inventory, player.getUniqueId());
        insertDiscordInventoryContent(inventory, player.getUniqueId());
    }

    public void insertFillers(Inventory inventory) {

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for(int i : new int[]{0, 1, 2, 3, 5, 6, 7, 8, 40, 41, 42, 43, 44}) {
            inventory.setItem(i, filler);
        }
    }

    public void insertDiscordHead(Inventory inventory, UUID uuid) {

        String TEXTURE = "b722098ae79c7abf002fe9684c773ea71db8919bb2ef2053ea0c0684c5a1ce4f";

        PlayerProfile profile = getProfile(TEXTURE);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Discord Inventory");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Value: " + ChatColor.GOLD + Formatter.format(DiscordInventories.getInstance().getInventory(uuid).getInventoryValue(), Style.ROUND_TO_TWO)));
        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);

        inventory.setItem(4, head);
    }

    public void insertLockedSpaces(Inventory inventory, UUID uuid) {

        ItemStack filler = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "LOCKED");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Click to buy next slot for: ", ChatColor.GOLD + Formatter.format(DiscordInventories.getInstance().getInventory(uuid).getNextSlotPrice(), Style.ROUND_TO_TWO)));
        filler.setItemMeta(meta);

        for(int i = 9 + SQLite.getInstance().retrieveCapacity(uuid); i < 40 ; i++) {
            inventory.setItem(i, filler);
        }
    }

    public void insertDiscordInventoryContent(Inventory inventory, UUID uuid) {

        HashMap<Item, Integer> content = DiscordInventories.getInstance().getInventory(uuid).getContent();

        int i = 9;
        for (Item item : content.keySet()) {
            ItemStack material = new ItemStack(item.getMaterial());
            ItemMeta meta = material.getItemMeta();
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Amount: " + content.get(item)));
            material.setItemMeta(meta);
            inventory.setItem(i, material);
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