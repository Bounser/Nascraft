package me.bounser.nascraft.market;

import de.tr7zw.changeme.nbtapi.NBT;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.managers.ImagesManager;
import me.bounser.nascraft.managers.GraphManager;
import me.bounser.nascraft.managers.TasksManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Logger;

public class MarketManager {

    private static final Logger LOGGER = Logger.getLogger("Nascraft");

    private final List<Item> items = new ArrayList<>();
    private final HashMap<String, Item> identifiers = new HashMap<>();
    private List<Category> categories = new ArrayList<>();

    private boolean active = true;

    private List<Float> marketChanges1h;
    private List<Float> marketChanges24h;

    private float lastChange;

    private int operationsLastHour = 0;

    private List<String> ignoredKeys = new ArrayList<>();

    ZoneOffset offset = ZonedDateTime.now(ZoneId.systemDefault()).getOffset();

    private static MarketManager instance = null;

    public static MarketManager getInstance() { return instance == null ? new MarketManager() : instance; }

    private MarketManager() {
        instance = this;
        setupItems();
        ignoredKeys = Config.getInstance().getIgnoredKeys();

        active = !Config.getInstance().isMarketClosed();
    }

    public void setupItems() {

        Config config = Config.getInstance();

        for (String categoryName : Config.getInstance().getCategories()) {
            Category category = new Category(categoryName);
            categories.add(category);
        }

        for (String identifier : Config.getInstance().getAllMaterials()) {

            ItemStack itemStack = config.getItemStackOfItem(identifier);

            if (itemStack == null) {
                LOGGER.warning("Error with the itemStack item: " + identifier);
                LOGGER.warning("Make sure the material is correct and exists in your version.");
                continue;
            }

            Category category = config.getCategoryFromMaterial(identifier);

            if (category == null) {
                LOGGER.warning("No category found for item: " + identifier);
                continue;
            }

            BufferedImage image = ImagesManager.getInstance().getImage(identifier);

            if (image == null) {
                LOGGER.warning("No image found for item: " + identifier);
                continue;
            }

            Item item = new Item(
                    itemStack,
                    identifier,
                    config.getAlias(identifier),
                    category,
                    image
            );

            DatabaseManager.get().getDatabase().retrieveItem(item);

            items.add(item);
            identifiers.put(identifier, item);
            category.addItem(item);

            for (Item child : config.getChilds(identifier)) {
                item.addChildItem(child);
                items.add(child);
            }
        }

        LOGGER.info("Loaded " + categories.size() + " categories.");

        Plugin AGUI = null;
        try {
            if (Bukkit.getServer() != null) AGUI = Bukkit.getPluginManager().getPlugin("AdvancedGUI");
        } catch (Throwable ignored) { /* no server in test context */ }
        if (categories.size() < 4 && (AGUI != null)) {
            LOGGER.severe("You need to have at least 4 categories! Disabling plugin...");
            Nascraft instance = Nascraft.getInstance();
            if (instance != null) instance.getPluginLoader().disablePlugin(instance);
        }

        for (Item item : items)
            if (item.getCategory() == null && item.isParent()) LOGGER.warning("Item: " + item.getIdentifier() + " is not assigned to any category.");

        marketChanges1h = new ArrayList<>(Collections.nCopies(60, 0f));
        marketChanges24h = new ArrayList<>(Collections.nCopies(24, 0f));

        try {
            if (Bukkit.getServer() != null) {
                TasksManager.getInstance();
                GraphManager.getInstance();
            }
        } catch (Throwable ignored) { /* no server in test context */ }
    }

    public void reload() {
        items.clear();
        categories.clear();

        setupItems();
    }

    public Item getItem(ItemStack itemStack) {
        for (Item item : items) if (isSimilarEnough(itemStack, item.getItemStack())) return item;
        return null;
    }

    public Item getItem(String identifier) {
        if (identifiers.containsKey(identifier)) return identifiers.get(identifier);
        return null;
    }

    public List<Category> getCategories() { return categories; }

    public List<Item> getAllItems() { return items; }

    public List<Item> getAllParentItemsInAlphabeticalOrder() {

        List<Item> sorted = new ArrayList<>(getAllParentItems());

        sorted.sort(Comparator.comparing(Item::getName));

        return sorted;
    }

    public List<String> getAllItemsAndChildsIdentifiers() {

        List<String> identifiers = new ArrayList<>();

        for (Item item : getAllItems()) {
            identifiers.add(item.getIdentifier());
        }

        return identifiers;
    }

    public List<Item> getAllParentItems() {

        List<Item> parents = new ArrayList<>();

        for (Item item : items) {
            if (item.isParent()) parents.add(item);
        }

        return parents;
    }

    public void stop() { active = false; }
    public void resume() { active = true; }

    public boolean getActive() { return active; }

    public boolean isAValidItem(ItemStack itemStack) {

        for (Item item : items)
            if (isSimilarEnough(item.getItemStack(), itemStack)) return true;

        return false;
    }

    public boolean isAValidParentItem(ItemStack itemStack) {

        for (Item item : getAllParentItems())
            if (isSimilarEnough(item.getItemStack(), itemStack)) return true;

        return false;
    }

    /**
     * Strategy seam: how to strip a single NBT key from an ItemStack.
     * Production uses NBT.modify (relocated NBT-API). Tests inject a mock so the
     * inline mock-maker doesn't need to instrument the (uninstrumentable) NBT class.
     */
    @FunctionalInterface
    public interface KeyStripper {
        void strip(ItemStack stack, String key);
    }

    private KeyStripper keyStripper = (stack, key) -> NBT.modify(stack, (java.util.function.Consumer<de.tr7zw.changeme.nbtapi.iface.ReadWriteItemNBT>) nbt -> nbt.removeKey(key));

    /** Test seam — package-private. */
    void setKeyStripper(KeyStripper stripper) { this.keyStripper = stripper; }

    public boolean isSimilarEnough(ItemStack itemStack1, ItemStack itemStack2) {

        if (itemStack1 == null || itemStack2 == null) return false;

        if (!itemStack1.getType().equals(itemStack2.getType())) return false;

        ItemStack itemStackWithoutFlags1 = itemStack1.clone();
        ItemStack itemStackWithoutFlags2 = itemStack2.clone();

        for (String ignoredKey : ignoredKeys) {
            keyStripper.strip(itemStackWithoutFlags1, ignoredKey);
            keyStripper.strip(itemStackWithoutFlags2, ignoredKey);
        }

        return itemStackWithoutFlags1.isSimilar(itemStackWithoutFlags2);
    }

    private List<Item> rankParentItems(int quantity, Comparator<Item> comparator) {
        if (quantity <= 0) return new ArrayList<>();

        List<Item> ranked = new ArrayList<>(getAllParentItems());
        ranked.sort(comparator);

        return new ArrayList<>(ranked.subList(0, Math.min(quantity, ranked.size())));
    }

    public List<Item> getTopGainers(int quantity) {
        return rankParentItems(
                quantity,
                Comparator.comparingDouble((Item item) -> item.getPrice().getValueChangeLastHour()).reversed()
        );
    }

    public List<Item> getTopDippers(int quantity) {
        return rankParentItems(
                quantity,
                Comparator.comparingDouble(item -> item.getPrice().getValueChangeLastHour())
        );
    }

    public List<Item> getMostMoved(int quantity) {
        return rankParentItems(
                quantity,
                Comparator.comparingDouble((Item item) -> Math.abs(item.getPrice().getValueChangeLastHour())).reversed()
        );
    }

    public List<Item> getMostTraded(int quantity) {
        return rankParentItems(
                quantity,
                Comparator.comparingInt(Item::getOperations).reversed()
        );
    }

    public int getPositionByVolume(Item item) {

        List<Item> items = new ArrayList<>(getAllItems());

        items.sort(Comparator.comparingDouble(Item::getVolume));

        return items.size()-getIndexOf(item, items);
    }

    public int getIndexOf(Item item, List<Item> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == item) {
                return i;
            }
        }
        return -1;
    }

    public void updateMarketChange1h(float change) {
        lastChange = change;

        marketChanges1h.add(change);
        marketChanges1h.remove(0);
    }

    public List<Float> getBenchmark1h(float base) {

        List<Float> benchmark = new ArrayList<>();

        float value = base;

        for (float change : marketChanges1h) {
            value += value * change/100;
            benchmark.add(value);
        }

        return benchmark;
    }

    public float getChange1h(){

        float change = 0;

        for (Item item : getAllParentItems())
            change += item.getPrice().getValue()/item.getPrice().getValueAnHourAgo()-1;

        return change*100;
    }

    public float getLastChange() { return lastChange; }

    public int[] getBenchmarkX(int xSize, int offset) { return Plot.getXPositions(xSize, offset, false, 60); }

    public int[] getBenchmarkY(int ySize, int offset) {
        return Plot.getYPositions(ySize, offset, false, getBenchmark1h(100));
    }

    public int getOperationsLastHour() { return operationsLastHour; }

    public void addOperation() { operationsLastHour++; }

    public void setOperationsLastHour(int operations) { operationsLastHour = operations; }

    public void removeItem(Item item) { items.remove(item); }

    public void addItem(Item item) { items.add(item); }

    public void removeCategory(Category category) { categories.remove(category); }

    public void addCategory(Category category) { categories.add(category); }

    public void setCategories(List<Category> categories) { this.categories = categories; }

    public Category getCategoryFromIdentifier(String identifier) {

        for (Category category : categories)
            if (category.getIdentifier().equals(identifier)) return category;

        return null;
    }

    public float getConsumerPriceIndex() {

        float index = 0;
        int numOfItems = 0;

        for (Item item : getAllParentItems()) {
            if (!item.getCurrency().equals(CurrenciesManager.getInstance().getDefaultCurrency())) continue;

            if (Config.getInstance().includeInCPI(item)) {
                index += (float) (item.getPrice().getValue()/item.getPrice().getInitialValue());
                numOfItems++;
            }
        }

        return (index/numOfItems)*100;
    }

}
