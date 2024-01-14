package me.bounser.nascraft.market;

import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.managers.GraphManager;
import me.bounser.nascraft.managers.TasksManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.config.Config;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MarketManager {

    private final List<Item> items = new ArrayList<>();
    private final List<Material> materials = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();

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

        for (String stringMaterial : Config.getInstance().getAllMaterials()) {

            Material material = Material.valueOf(stringMaterial.toUpperCase());

            Category category = config.getCategoryFromMaterial(material);

            Item item = new Item(material, config.getAlias(material), category);
            items.add(item);
            category.addItem(item);
            materials.add(material);
        }

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

    public Item getItem(Material material) {
        for (Item item : items) { if (item.getMaterial() == material) { return item; } }
        return null;
    }

    public Item getItem(String material) {
        for (Item item : items) { if (item.getMaterial().toString().equalsIgnoreCase(material.replace(" ", "_"))) { return item; } }
        return null;
    }

    public List<Category> getCategories() { return categories; }

    public List<Item> getAllItems() { return items; }

    public List<Item> getAllItemsInAlphabeticalOrder() {

        List<Item> sorted = new ArrayList<>(items);

        sorted.sort(Comparator.comparing(Item::getName));

        return sorted;
    }

    public List<Material> getAllMaterials() { return materials; }

    public void stop() { active = false; }
    public void resume() { active = true; }

    public boolean getActive() { return active; }

    public boolean isValidItem(ItemStack itemStack) {

        if (itemStack == null) return false;

        if (!getAllMaterials().contains(itemStack.getType())) return false;

        ItemMeta meta = itemStack.getItemMeta();

        return !meta.hasDisplayName() && !meta.hasEnchants() && !meta.hasLore() && !meta.hasAttributeModifiers() && !meta.hasCustomModelData();
    }

    public List<Item> getTopGainers(int quantity) {

        List<Item> items = new ArrayList<>(MarketManager.getInstance().getAllItems());

        List<Item> topGainers = new ArrayList<>();

        for (int i = 1; i <= quantity ; i++) {

            Item imax = items.get(0);
            for (Item item : items) {

                float variation = -100 + 100 * (item.getPrice().getValue() / item.getPrices(TimeSpan.HOUR).get(0));

                if (variation != 0) {
                    if (variation > -100 + 100 * (imax.getPrice().getValue() / imax.getPrices(TimeSpan.HOUR).get(0))) {
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

        List<Item> items = new ArrayList<>(MarketManager.getInstance().getAllItems());

        List<Item> topDippers = new ArrayList<>();

        for (int i = 1; i <= quantity ; i++) {

            Item imax = items.get(0);
            for (Item item : items) {

                float variation = RoundUtils.round(-100 + 100 * (item.getPrice().getValue() / item.getPrices(TimeSpan.HOUR).get(0)));

                if (variation != 0) {
                    if (variation < -100 + 100 * (imax.getPrice().getValue() / imax.getPrices(TimeSpan.HOUR).get(0))) {
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

        List<Item> items = new ArrayList<>(MarketManager.getInstance().getAllItems());

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
        List<Float> benchmark = getBenchmark1h(1);
        return -100f + 100f*benchmark.get(benchmark.size()-1);
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

}
