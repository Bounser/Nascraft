package me.bounser.nascraft.commands.admin.marketeditor.edit.item;

import me.bounser.nascraft.commands.admin.marketeditor.overview.MarketEditor;
import me.bounser.nascraft.commands.admin.marketeditor.overview.MarketEditorManager;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.managers.ImagesManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.managers.currencies.Currency;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import java.util.List;

public class EditItemMenu {

    private ItemStack itemStack;

    private double initialPrice;
    private String alias;
    private float elasticity;
    private float noiseIntensity;
    private double support;
    private double resistance;

    private Category prevCategory;
    private Category category;

    private Item item;
    private Player player;

    private Currency currency;

    public EditItemMenu(Player player, ItemStack itemStack) {

        this.player = player;

        this.itemStack = itemStack.clone();
        this.itemStack.setAmount(1);

        initialPrice = 1;
        alias = (Character.toUpperCase(itemStack.getType().toString().toLowerCase().charAt(0)) + itemStack.getType().toString().toLowerCase().substring(1)).replace("_", " ");
        elasticity = 1;
        noiseIntensity = 1;
        support = 0;
        resistance = 0;
        currency = CurrenciesManager.getInstance().getDefaultCurrency();

        prevCategory = MarketManager.getInstance().getCategories().get(0);
        category = MarketManager.getInstance().getCategories().get(0);

        open();
    }

    public EditItemMenu(Player player, Item item) {

        this.player = player;
        this.item = item;

        itemStack = item.getItemStack().clone();
        initialPrice = item.getPrice().getInitialValue();
        alias = item.getName();
        elasticity = item.getPrice().getElasticity();
        noiseIntensity = item.getPrice().getNoiseIntensity();
        support = item.getPrice().getSupport();
        resistance = item.getPrice().getResistance();
        currency = item.getCurrency();

        prevCategory = item.getCategory();
        category = item.getCategory();

        open();

    }

    public void open() {

        Inventory inventory = Bukkit.createInventory(player, 27, "§8§lEditing Item");

        insertPanes(inventory);
        insertOptions(inventory);
        insertItem(itemStack.clone(), inventory);
        this.itemStack = itemStack.clone();
        player.openInventory(inventory);
    }

    public Currency getCurrency() {
        return currency;
    }

    public double getInitialPrice() {
        return initialPrice;
    }

    public double getSupport() {
        return support;
    }

    public double getResistance() {
        return resistance;
    }

    public void insertItem(ItemStack itemStack, Inventory inventory) {

        ItemStack displayItemStack = itemStack.clone();

        ItemMeta meta = displayItemStack.getItemMeta();

        if (meta.hasLore()) {
            List<String> lore = meta.getLore();

            lore.add("");
            lore.add(ChatColor.GREEN + "§lCLICK TO CHANGE");

            meta.setLore(lore);
        } else {
            meta.setLore(Arrays.asList("", ChatColor.GREEN + "§lCLICK TO CHANGE"));
        }

        displayItemStack.setItemMeta(meta);

        inventory.setItem(10, displayItemStack);
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

        for(int i : new int[]{3, 12, 21, 6, 7, 8, 16, 24, 25, 26}) {
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
        metaDelete.setDisplayName(ChatColor.RED + "§lDELETE ITEM");
        deletePanel.setItemMeta(metaDelete);

        inventory.setItem(17, deletePanel);
    }

    public void insertOptions(Inventory inventory) {

        Component priceComponent = MiniMessage.miniMessage().deserialize(Formatter.format(currency, initialPrice, Style.ROUND_BASIC));

        inventory.setItem(4,
                getItemStackOfOption(Material.GOLD_INGOT,
                        "Initial Price " + ChatColor.UNDERLINE + "(REQUIRED)",
                        Arrays.asList(ChatColor.GRAY + "Value: " + BukkitComponentSerializer.legacy().serialize(priceComponent),
                                "",
                                ChatColor.GRAY + "The initial price of the item gives a point",
                                ChatColor.GRAY + "of initial stability at neutral internal stock (0)",
                                "",
                                ChatColor.RED + "⚠ CAUTION ⚠ Changing this value will alter the",
                                ChatColor.RED + "shape of the price curve, changing the current price.",
                                "",
                                ChatColor.GREEN + "" + ChatColor.BOLD + "CLICK TO EDIT")
        ));

        inventory.setItem(5,
                getItemStackOfOption(Material.NAME_TAG,
                "Alias",
                        Arrays.asList(ChatColor.GRAY + "Alias: " + ChatColor.GREEN + alias,
                                "",
                                ChatColor.GRAY + "This is the name that will be displayed to players.",
                                "",
                                ChatColor.GREEN + "" + ChatColor.BOLD + "CLICK TO EDIT")
        ));

        inventory.setItem(6,
                getItemStackOfOption(Material.GOLD_NUGGET,
                        "Currency",
                        Arrays.asList(ChatColor.GRAY + "Currency: " + ChatColor.GREEN + currency.getCurrencyIdentifier(),
                                "",
                                ChatColor.GREEN + "" + ChatColor.BOLD + "CLICK TO SWITCH BETWEEN CURRENCIES")
                ));

        inventory.setItem(13,
                getItemStackOfOption(Material.SLIME_BALL,
                "Elasticity",
                        Arrays.asList(ChatColor.GRAY + "Value: " + ChatColor.GREEN + elasticity,
                                "",
                                ChatColor.GRAY + "This value determines the magnitude of the",
                                ChatColor.GRAY + "changes due to player transactions. Has decimal precision.",
                                ChatColor.GRAY + "A bigger value means that when a player buys or sells",
                                ChatColor.GRAY + "the price will react with a bigger change. Conversely",
                                ChatColor.GRAY + "when the value is lower this changes will be smaller.",
                                "",
                                ChatColor.RED + "⚠ CAUTION ⚠ Changing this value will alter the",
                                ChatColor.RED + "shape of the price curve, changing the current price.",
                                "",
                                ChatColor.GREEN + "" + ChatColor.BOLD + "CLICK TO EDIT")
        ));

        inventory.setItem(14,
                getItemStackOfOption(Material.COMPARATOR,
                "Noise Intensity",
                        Arrays.asList(ChatColor.GRAY + "Value: " + ChatColor.GREEN + noiseIntensity,
                                "",
                                ChatColor.GRAY + "This value determines the sensibility of the",
                                ChatColor.GRAY + "item to random changes. Has decimal precision.",
                                ChatColor.GRAY + "A bigger value means that the price will fluctuate",
                                ChatColor.GRAY + "in bigger quantities than when the value is lower.",
                                "",
                                ChatColor.RED + "⚠ CAUTION ⚠ High values can make the item",
                                ChatColor.RED + "extremely volatile.",
                                "",
                                ChatColor.GREEN + "" + ChatColor.BOLD + "CLICK TO EDIT")
        ));

        Component supportComponent = MiniMessage.miniMessage().deserialize(Formatter.format(currency, support, Style.ROUND_BASIC));

        inventory.setItem(22,
                getItemStackOfOption(Material.BEDROCK,
                "Support",
                        Arrays.asList(ChatColor.GRAY + "Value: " + (support == 0 ? ChatColor.RED + "DISABLED" : BukkitComponentSerializer.legacy().serialize(supportComponent)),
                                "",
                                ChatColor.GRAY + "If the noise is enabled, then the price of the",
                                ChatColor.GRAY + "item will slowly tend to stay " + ChatColor.UNDERLINE + "ABOVE this value.",
                                "",
                                ChatColor.GREEN + "" + ChatColor.BOLD + "CLICK TO EDIT")
        ));

        Component resistanceComponent = MiniMessage.miniMessage().deserialize(Formatter.format(currency, resistance, Style.ROUND_BASIC));

        inventory.setItem(23,
                getItemStackOfOption(Material.WHITE_WOOL,
                        "Resistance",
                        Arrays.asList(ChatColor.GRAY + "Value: " + (resistance == 0 ? ChatColor.RED + "DISABLED" : BukkitComponentSerializer.legacy().serialize(resistanceComponent)),
                                "",
                                ChatColor.GRAY + "If the noise is enabled, then the price of the",
                                ChatColor.GRAY + "item will slowly tend to stay " + ChatColor.UNDERLINE + "BELOW this value.",
                                "",
                                ChatColor.GREEN + "" + ChatColor.BOLD + "CLICK TO EDIT")
        ));

        inventory.setItem(15,
                getItemStackOfOption(Material.CHEST,
                        "Category",
                        Arrays.asList(ChatColor.GRAY + "Category: " + ChatColor.GREEN + category.getIdentifier() + ChatColor.GRAY + " - " + ChatColor.GOLD + category.getDisplayName(),
                                "",
                                ChatColor.GREEN + "" + ChatColor.BOLD + "CLICK TO CHANGE")
        ));
    }

    public static ItemStack getItemStackOfOption(Material material, String displayName, List<String> value) {
        ItemStack paper = new ItemStack(material);
        ItemMeta meta = paper.getItemMeta();
        meta.setLore(value);
        meta.setDisplayName(ChatColor.GOLD + displayName);
        paper.setItemMeta(meta);
        return paper;
    }

    public void setItemStack(ItemStack itemStack) { this.itemStack = itemStack; }
    public void setInitialPrice(float initialPrice) { this.initialPrice = initialPrice; }
    public void setAlias(String alias) { this.alias = alias; }
    public void setElasticity(float elasticity) { this.elasticity = elasticity; }
    public void setNoiseIntensity(float noiseIntensity) { this.noiseIntensity = noiseIntensity; }
    public void setSupport(float support) { this.support = support; }
    public void setResistance(float resistance) { this.resistance = resistance; }
    public void setCategory(Category category) { this.category = category; }
    public void setCurrency(Currency currency) { this.currency = currency; }

    public void save() {

        FileConfiguration items = Config.getInstance().getItemsFileConfiguration();

        String identifier;

        if (item == null) {

            int count = 0;

            for (Item item : MarketManager.getInstance().getAllItems())
                if (item.getItemStack().getType().equals(itemStack.getType())) count++;

            identifier = count == 0 ? itemStack.getType().toString().toLowerCase() : itemStack.getType().toString().toLowerCase() + count;
        }
        else identifier = item.getIdentifier();

        items.set("items." + identifier + ".alias", alias);
        items.set("items." + identifier + ".initial-price", initialPrice);
        items.set("items." + identifier + ".elasticity", elasticity);
        items.set("items." + identifier + ".noise-intensity", noiseIntensity);

        if (support != 0)
            items.set("items." + identifier + ".support", support);

        if (resistance != 0)
            items.set("items." + identifier + ".resistance", resistance);

        if (itemStack != null && itemStack.getType() != Material.AIR) {
            if (item != null) item.setItemStack(itemStack);
            items.set("items." + identifier + ".item-stack", itemStack);
        }

        if (!currency.equals(CurrenciesManager.getInstance().getDefaultCurrency())) {
            items.set("items." + identifier + ".currency", currency.getCurrencyIdentifier());
        }

        FileConfiguration categories = Config.getInstance().getCategoriesFileConfiguration();

        List<String> itemsOfPrevCategory = categories.getStringList("categories." + prevCategory.getIdentifier() + ".items");
        itemsOfPrevCategory.remove(identifier);
        categories.set("categories." + prevCategory.getIdentifier() + ".items", itemsOfPrevCategory);

        List<String> itemsOfNewCategory = categories.getStringList("categories." + category.getIdentifier() + ".items");
        itemsOfNewCategory.add(identifier);
        categories.set("categories." + category.getIdentifier() + ".items", itemsOfNewCategory);

        try {
            categories.save(Config.getInstance().getCategoriesFile());
            items.save(Config.getInstance().getItemsFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (item != null) {
            item.setCategory(category);
            item.setCurrency(currency);
            item.changeProperties(
                    initialPrice,
                    alias,
                    elasticity,
                    noiseIntensity,
                    support,
                    resistance
            );

            if (!prevCategory.getIdentifier().equals(category.getIdentifier())) {
                category.addItem(item);
                prevCategory.removeItem(item);
            }

            player.sendMessage(ChatColor.LIGHT_PURPLE + "Property changes saved!");
        } else {
            Item item = new Item(itemStack, identifier, alias, category, ImagesManager.getInstance().getImage(identifier));
            item.setCurrency(currency);
            category.addItem(item);
            MarketManager.getInstance().addItem(item);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "New item saved!");
        }

        MarketEditorManager.getInstance().getMarketEditorFromPlayer(player).open();
    }

    public void removeItem() {

        if (item == null) new MarketEditor(player);

        MarketManager.getInstance().removeItem(item);
        prevCategory.removeItem(item);

        FileConfiguration items = Config.getInstance().getItemsFileConfiguration();

        items.set("items." + item.getIdentifier(), null);

        FileConfiguration categories = Config.getInstance().getCategoriesFileConfiguration();

        List<String> itemsOfPrevCategory = categories.getStringList("categories." + prevCategory.getIdentifier() + ".items");
        itemsOfPrevCategory.remove(item.getIdentifier());
        categories.set("categories." + prevCategory.getIdentifier() + ".items", itemsOfPrevCategory);

        try {
            categories.save(Config.getInstance().getCategoriesFile());
            items.save(Config.getInstance().getItemsFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        MarketEditorManager.getInstance().getMarketEditorFromPlayer(player).open();
    }

}
