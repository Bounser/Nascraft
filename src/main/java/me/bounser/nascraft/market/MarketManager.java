package me.bounser.nascraft.market;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.discord.images.ImagesManager;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.managers.GraphManager;
import me.bounser.nascraft.managers.TasksManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.config.Config;
import org.bukkit.inventory.ItemStack;

import javax.xml.crypto.Data;
import java.awt.image.BufferedImage;
import java.util.*;

public class MarketManager {

    private final List<Item> items = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();

    private boolean active = true;

    private List<Float> marketChanges1h;
    private List<Float> marketChanges24h;

    private float lastChange;

    private int operationsLastHour = 0;

    private static MarketManager instance = null;

    public static MarketManager getInstance() { return instance == null ? new MarketManager() : instance; }

    private MarketManager() {
        instance = this;
        setupItems();
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
                Nascraft.getInstance().getLogger().warning("Error with the itemStack item: " + identifier);
                Nascraft.getInstance().getLogger().warning("Make sure the material is correct and exists in your version.");
                continue;
            }

            Category category = config.getCategoryFromMaterial(identifier);

            if (category == null) {
                Nascraft.getInstance().getLogger().warning("No category found for item: " + identifier);
                continue;
            }

            BufferedImage image = ImagesManager.getInstance().getImage(identifier);

            if (image == null) {
                Nascraft.getInstance().getLogger().warning("No image found for item: " + identifier);
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
            category.addItem(item);

            for (Item child : config.getChilds(identifier)) {
                item.addChildItem(child);
                items.add(child);
            }
        }

        if (categories.size() < 4) {
            Nascraft.getInstance().getLogger().severe("You need to have at least 4 categories! Disabling plugin...");
            Nascraft.getInstance().getPluginLoader().disablePlugin(Nascraft.getInstance());
        }

        for (Item item : items)
            if (item.getCategory() == null && item.isParent()) Nascraft.getInstance().getLogger().warning("Item: " + item.getIdentifier() + " is not assigned to any category.");

        marketChanges1h = new ArrayList<>(Collections.nCopies(60, 0f));
        marketChanges24h = new ArrayList<>(Collections.nCopies(24, 0f));

        TasksManager.getInstance();
        GraphManager.getInstance();
    }

    public void reload() {
        items.clear();
        categories.clear();

        setupItems();
    }

    public Item getItem(ItemStack itemStack) {
        for (Item item : items) if (itemStack.isSimilar(item.getItemStack())) return item;
        return null;
    }

    public Item getItem(String identifier) {
        for (Item item : items) if (item.getIdentifier().equalsIgnoreCase(identifier)) return item;
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
            if (item.getItemStack().isSimilar(itemStack)) return true;

        return false;
    }

    public boolean isAValidParentItem(ItemStack itemStack) {

        for (Item item : getAllParentItems())
            if (item.getItemStack().isSimilar(itemStack)) return true;

        return false;
    }

    public List<Item> getTopGainers(int quantity) {

        List<Item> items = new ArrayList<>(MarketManager.getInstance().getAllParentItems());

        List<Item> topGainers = new ArrayList<>();

        for (int i = 1; i <= quantity ; i++) {

            Item imax = items.get(0);
            for (Item item : items) {

                float variation = -100 + 100 * (item.getPrice().getValue() / item.getPrice().getValueAnHourAgo());

                if (variation != 0) {
                    if (variation > -100 + 100 * (imax.getPrice().getValue() / imax.getPrice().getValueAnHourAgo())) {
                        imax = item;
                    }
                }
            }
            items.remove(imax);

            topGainers.add(imax);
        }
        return topGainers;
    }

    public List<Item> getTopDippers(int quantity) {

        List<Item> items = new ArrayList<>(MarketManager.getInstance().getAllParentItems());

        List<Item> topDippers = new ArrayList<>();

        for (int i = 1; i <= quantity ; i++) {

            Item imax = items.get(0);
            for (Item item : items) {

                float variation = RoundUtils.round(-100 + 100 * (item.getPrice().getValue() / item.getPrice().getValueAnHourAgo()));

                if (variation != 0) {
                    if (variation < -100 + 100 * (imax.getPrice().getValue() / imax.getPrice().getValueAnHourAgo())) {
                        imax = item;
                    }
                }
            }
            items.remove(imax);

            topDippers.add(imax);
        }
        return topDippers;
    }

    public List<Item> getMostTraded(int quantity) {

        List<Item> items = new ArrayList<>(MarketManager.getInstance().getAllParentItems());

        List<Item> mostTraded = new ArrayList<>();

        for (int i = 1; i <= quantity ; i++) {

            Item imax = items.get(0);
            for (Item item : items) {

                if (item.getOperations() >= 1) {
                    if (item.getOperations() > imax.getOperations()) {
                        imax = item;
                    }
                }
            }
            items.remove(imax);

            mostTraded.add(imax);
        }
        return mostTraded;
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

    public void addCategory(Category category) { categories.remove(category); }

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
            if (Config.getInstance().includeInCPI(item)) {
                index += item.getPrice().getValue()/item.getPrice().getInitialValue();
                numOfItems++;
            }
        }

        return (index/numOfItems)*100;
    }

}
