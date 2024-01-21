package me.bounser.nascraft.commands.admin.marketeditor.edit.category;

import me.bounser.nascraft.commands.admin.marketeditor.overview.MarketEditor;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.resources.Category;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CategoryEditor {

    private List<Category> categories;

    private final HashMap<Player, Integer> playerOffset = new HashMap<>();

    private Player player;


    public CategoryEditor(Player player) {

        categories = new ArrayList<>(MarketManager.getInstance().getCategories());
        this.player = player;

        open();
    }

    public void open() {

        Inventory inventory = Bukkit.createInventory(player, 45, "§8§lEditing Categories");

        insertPanes(inventory);
        insertArrows(inventory);
        insertCategories(inventory);

        player.openInventory(inventory);

    }

    public void insertPanes(Inventory inventory) {

        ItemStack blackFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta metaBlack = blackFiller.getItemMeta();
        metaBlack.setDisplayName(" ");
        blackFiller.setItemMeta(metaBlack);

        for(int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 37, 38, 39, 41, 42, 43}) {
            inventory.setItem(i, blackFiller);
        }

        ItemStack closeButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = closeButton.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "§lCANCEL");
        closeButton.setItemMeta(meta);

        inventory.setItem(8, closeButton);

        ItemStack confirmButton = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta metaConfirm = confirmButton.getItemMeta();
        metaConfirm.setDisplayName(ChatColor.GREEN + "§lSAVE CHANGES");
        confirmButton.setItemMeta(metaConfirm);

        inventory.setItem(40, confirmButton);
    }

    public void insertArrows(Inventory inventory) {

        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta metaArrow = arrow.getItemMeta();
        metaArrow.setDisplayName(ChatColor.LIGHT_PURPLE + "§l< LEFT");
        arrow.setItemMeta(metaArrow);

        inventory.setItem(36, arrow);

        metaArrow.setDisplayName(ChatColor.LIGHT_PURPLE + "§lRIGHT >");
        arrow.setItemMeta(metaArrow);

        inventory.setItem(44, arrow);
    }

    public List<Category> getCategories() { return categories; }

    public void insertCategories(Inventory inventory) {

        List<Category> categoriesList;

        if (playerOffset.containsKey(player)) {

            if (categories.size() > playerOffset.get(player) + 9) {
                categoriesList = categories.subList(playerOffset.get(player), playerOffset.get(player) + 9);
            } else {
                categoriesList = categories.subList(playerOffset.get(player), categories.size()-1);
            }

        } else {
            if (categories.size() > 9) {
                categoriesList = categories.subList(0, 9);
            } else {
                categoriesList = categories.subList(playerOffset.get(player), categories.size()-1);
            }
        }

        while (categoriesList.size() < 9) categoriesList.add(null);

        for (int i = 0; i < 9; i++) {

            Category category = categoriesList.get(i);

            if (category == null) {

                inventory.setItem(9+i, new ItemStack(Material.AIR));
                inventory.setItem(18+i, new ItemStack(Material.AIR));
                inventory.setItem(27+i, new ItemStack(Material.AIR));

            } else {

                inventory.setItem(9+i, getItemStackOfOption(
                        "Display name: " + category.getDisplayName(),
                        "Click to change the display name.",
                        Material.NAME_TAG
                ));

                inventory.setItem(18+i, getItemStackOfOption(
                        category.getDisplayName(),
                        "Identifier: " + category.getIdentifier(),
                        category.getMaterial()
                ));

                inventory.setItem(27+i, getItemStackOfOption(
                        "§c§lDELETE CATEGORY",
                        "Click to delete.",
                        Material.RED_CONCRETE
                ));

            }

        }

    }

    public ItemStack getItemStackOfOption(String displayName, String lore, Material material) {
        ItemStack paper = new ItemStack(material);
        ItemMeta meta = paper.getItemMeta();
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + lore
        ));
        meta.setDisplayName(ChatColor.GOLD + displayName);
        paper.setItemMeta(meta);
        return paper;
    }

    public void removeCategory(Category category) {

        if (categories == null || category == null) new MarketEditor(player);

        MarketManager.getInstance().removeCategory(category);

        FileConfiguration categories = Config.getInstance().getCategoriesFileConfiguration();

        categories.set("categories." + category.getIdentifier(), null);

        try {
            categories.save(Config.getInstance().getCategoriesFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new MarketEditor(player);
    }

    public int getOffset() {
        if (playerOffset.containsKey(player)) return playerOffset.get(player);
        playerOffset.put(player, 0);
        return 0;
    }

    public void increaseOffset() {
        if (playerOffset.containsKey(player) && playerOffset.get(player) < categories.size()) {
            playerOffset.put(player, playerOffset.get(player)+1);
        }
    }

    public void decreaseOffset() {
        if (playerOffset.containsKey(player) && playerOffset.get(player) > 0) {
            playerOffset.put(player, playerOffset.get(player)-1);
        }
    }
}
