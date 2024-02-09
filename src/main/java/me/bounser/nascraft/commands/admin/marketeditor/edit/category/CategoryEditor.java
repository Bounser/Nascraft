package me.bounser.nascraft.commands.admin.marketeditor.edit.category;

import me.bounser.nascraft.commands.admin.marketeditor.overview.MarketEditorManager;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CategoryEditor {

    private Category previousCategory;

    private String identifier;
    private String displayName;
    private Material material;


    private Player player;


    public CategoryEditor(Player player, Category category) {

        previousCategory = category;

        identifier = category.getIdentifier();

        displayName = category.getDisplayName();
        material = category.getMaterial();

        this.player = player;

        open();
    }

    public void open() {

        Inventory inventory = Bukkit.createInventory(player, 27, "§8§lEdit Category");

        insertPanes(inventory);
        insertCategoryOptions(inventory);

        player.openInventory(inventory);
    }

    public void insertPanes(Inventory inventory) {

        ItemStack blackFiller = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta metaBlack = blackFiller.getItemMeta();
        metaBlack.setDisplayName(" ");
        blackFiller.setItemMeta(metaBlack);

        ItemStack grayFiller = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta metaGray = grayFiller.getItemMeta();
        metaGray.setDisplayName(" ");
        grayFiller.setItemMeta(metaGray);

        for(int i : new int[]{3, 4, 5, 12, 15, 22, 21, 23, 6, 7, 8, 16, 24, 25, 26}) {
            inventory.setItem(i, blackFiller);
        }

        for(int i : new int[]{0, 1, 2, 18, 19, 20}) {
            inventory.setItem(i, grayFiller);
        }

        ItemStack closeButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = closeButton.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "§lCANCEL");
        closeButton.setItemMeta(meta);

        inventory.setItem(11, closeButton);

        ItemStack confirmButton = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta metaConfirm = confirmButton.getItemMeta();
        metaConfirm.setDisplayName(ChatColor.GREEN + "§lSAVE CHANGES");
        confirmButton.setItemMeta(metaConfirm);

        inventory.setItem(9, confirmButton);

        ItemStack deletePanel = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta metaDelete = deletePanel.getItemMeta();
        metaDelete.setDisplayName(ChatColor.RED + "§lDELETE CATEGORY");
        deletePanel.setItemMeta(metaDelete);

        inventory.setItem(17, deletePanel);
    }


    public void insertCategoryOptions(Inventory inventory) {

        inventory.setItem(10, getItemStackOfOption(
                identifier,
                Arrays.asList(
                        ChatColor.GRAY + "Display named: " + ChatColor.GOLD + displayName,
                        ChatColor.GRAY + "Material: " + ChatColor.GOLD + material.name()),
                material
        ));

        inventory.setItem(9, getItemStackOfOption(
                ChatColor.GREEN + "§lSAVE CHANGES",
                Collections.singletonList(""),
                Material.LIME_STAINED_GLASS_PANE
        ));

        inventory.setItem(11, getItemStackOfOption(
                ChatColor.RED + "§lCANCEL",
                Collections.singletonList(""),
                Material.RED_STAINED_GLASS_PANE
        ));

        inventory.setItem(17, getItemStackOfOption(
                ChatColor.RED + "§lDELETE CATEGORY",
                Collections.singletonList(""),
                Material.RED_STAINED_GLASS_PANE
        ));

        inventory.setItem(13, getItemStackOfOption(
                ChatColor.GRAY + "Category display name",
                Arrays.asList(ChatColor.GOLD + displayName, "", ChatColor.GRAY + "Click to change"),
                Material.PAPER
        ));

        inventory.setItem(14, getItemStackOfOption(
                ChatColor.GRAY + "Category material",
                Arrays.asList(ChatColor.GOLD + material.toString().toLowerCase(), "", ChatColor.GRAY + "Click with the new material."),
                Material.PAPER
        ));
    }

    public ItemStack getItemStackOfOption(String displayName, List<String> lore, Material material) {
        ItemStack paper = new ItemStack(material);
        ItemMeta meta = paper.getItemMeta();
        meta.setLore(lore);
        meta.setDisplayName(ChatColor.GOLD + displayName);
        paper.setItemMeta(meta);
        return paper;
    }

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public void setMaterial(Material material) { this.material = material; }

    public void save() {

        List<Category> categories = MarketManager.getInstance().getCategories();

        Category newCategory = new Category(identifier);

        newCategory.setDisplayName(displayName);
        newCategory.setDisplayMaterial(material);
        newCategory.setItems(previousCategory.getItems());

        categories.set(categories.indexOf(previousCategory), newCategory);

        MarketManager.getInstance().setCategories(categories);

        FileConfiguration categoriesFile = Config.getInstance().getCategoriesFileConfiguration();

        categoriesFile.set("categories." + identifier + ".display-name", displayName);

        if (!material.equals(Material.STONE))
            categoriesFile.set("categories." + identifier + ".display-material", material.toString().toLowerCase());

        try { categoriesFile.save(Config.getInstance().getCategoriesFile()); }
        catch (IOException e) { throw new RuntimeException(e); }

        player.sendMessage(ChatColor.LIGHT_PURPLE + "Changes in categories saved.");
        MarketEditorManager.getInstance().getMarketEditorFromPlayer(player).open();
    }

    public void removeCategory() {

        List<Category> categories = MarketManager.getInstance().getCategories();
        categories.remove(previousCategory);
        MarketManager.getInstance().setCategories(categories);

        FileConfiguration categoriesFile = Config.getInstance().getCategoriesFileConfiguration();

        categoriesFile.set("categories." + identifier, null);

        try { categoriesFile.save(Config.getInstance().getCategoriesFile()); }
        catch (IOException e) { throw new RuntimeException(e); }

        player.sendMessage(ChatColor.LIGHT_PURPLE + "Category deleted.");
        if (MarketEditorManager.getInstance().getMarketEditorFromPlayer(player) == null) {
            MarketEditorManager.getInstance().startEditing(player);
        } else {
            MarketEditorManager.getInstance().clearEditing(player);
            MarketEditorManager.getInstance().startEditing(player);
        }
    }
}