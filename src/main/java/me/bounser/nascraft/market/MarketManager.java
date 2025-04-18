package me.bounser.nascraft.market;

import de.tr7zw.changeme.nbtapi.NBT;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.chart.cpi.CPIInstant;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.managers.DebtManager;
import me.bounser.nascraft.managers.ImagesManager;
import me.bounser.nascraft.managers.GraphManager;
import me.bounser.nascraft.managers.TasksManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.unit.stats.Instant;
import me.bounser.nascraft.portfolio.Portfolio;
import me.bounser.nascraft.portfolio.PortfoliosManager;
import me.bounser.nascraft.web.dto.CategoryDTO;
import me.bounser.nascraft.web.dto.ItemDTO;
import me.bounser.nascraft.web.dto.PortfolioDTO;
import me.bounser.nascraft.web.dto.TimeSeriesDTO;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.units.qual.C;

import java.awt.image.BufferedImage;
import java.time.ZoneOffset;
import java.util.*;

public class MarketManager {

    private final List<Item> items = new ArrayList<>();
    private final HashMap<String, Item> identifiers = new HashMap<>();
    private List<Category> categories = new ArrayList<>();

    private boolean active = true;

    private List<Float> marketChanges1h;
    private List<Float> marketChanges24h;

    private float lastChange;

    private int operationsLastHour = 0;

    private List<String> ignoredKeys = new ArrayList<>();

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
            identifiers.put(identifier, item);
            category.addItem(item);

            for (Item child : config.getChilds(identifier)) {
                item.addChildItem(child);
                items.add(child);
            }
        }

        Nascraft.getInstance().getLogger().info("Loaded " + categories.size() + " categories.");

        Plugin AGUI = Bukkit.getPluginManager().getPlugin("AdvancedGUI");
        if (categories.size() < 4 && (AGUI != null)) {
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

    public boolean isSimilarEnough(ItemStack itemStack1, ItemStack itemStack2) {

        if (itemStack1 == null || itemStack2 == null) return false;

        if (!itemStack1.getType().equals(itemStack2.getType())) return false;

        ItemStack itemStackWithoutFlags1 = itemStack1.clone();
        ItemStack itemStackWithoutFlags2 = itemStack2.clone();

        for (String ignoredKey : ignoredKeys) {
            NBT.modify(itemStackWithoutFlags1, nbt -> {
                nbt.removeKey(ignoredKey);
            });
            NBT.modify(itemStackWithoutFlags1, nbt -> {
                nbt.removeKey(ignoredKey);
            });
        }

        return itemStackWithoutFlags1.isSimilar(itemStackWithoutFlags2);
    }

    public List<Item> getTopGainers(int quantity) {

        List<Item> items = new ArrayList<>(MarketManager.getInstance().getAllParentItems());

        List<Item> topGainers = new ArrayList<>();

        for (int i = 1; i <= quantity ; i++) {

            Item imax = items.get(0);
            for (Item item : items) {

                float variation = item.getPrice().getValueChangeLastHour();

                if (variation != 0) {
                    if (variation > imax.getPrice().getValueChangeLastHour()) {
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

                float variation = item.getPrice().getValueChangeLastHour();

                if (variation != 0) {
                    if (variation < imax.getPrice().getValueChangeLastHour()) {
                        imax = item;
                    }
                }
            }
            items.remove(imax);

            topDippers.add(imax);
        }
        return topDippers;
    }

    public List<Item> getMostMoved(int quantity) {

        List<Item> items = new ArrayList<>(MarketManager.getInstance().getAllParentItems());

        List<Item> mostMoved = new ArrayList<>();

        for (int i = 1; i <= quantity ; i++) {

            Item imax = items.get(0);
            for (Item item : items) {

                float variation = item.getPrice().getValueChangeLastHour();

                if (variation != 0) {
                    if (Math.abs(variation) > Math.abs(imax.getPrice().getValueChangeLastHour())) {
                        imax = item;
                    }
                }
            }
            items.remove(imax);

            mostMoved.add(imax);
        }
        return mostMoved;
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
            if (!item.getCurrency().equals(CurrenciesManager.getInstance().getDefaultCurrency())) continue;

            if (Config.getInstance().includeInCPI(item)) {
                index += (float) (item.getPrice().getValue()/item.getPrice().getInitialValue());
                numOfItems++;
            }
        }

        return (index/numOfItems)*100;
    }

    public List<ItemDTO> getAllItemData() {

        List<ItemDTO> itemsDTO = new ArrayList<>();

        for (Item item : items) {

            if (!item.isParent()) continue;

            itemsDTO.add(
                new ItemDTO(
                        item.getIdentifier(),
                        item.getName(),
                        item.getPrice().getValue(),
                        item.getPrice().getBuyPrice(),
                        item.getPrice().getSellPrice(),
                        item.getOperations(),
                        Formatter.roundToDecimals(item.getPrice().getValueChangeLastHour(), 1)
                )
            );
        }

        return  itemsDTO;
    }

    public ItemDTO getPopularItem() {

        Item item = getMostTraded(1).get(0);

        return new ItemDTO(
                item.getIdentifier(),
                item.getName(),
                item.getPrice().getValue(),
                item.getPrice().getBuyPrice(),
                item.getPrice().getSellPrice(),
                item.getOperations(),
                Formatter.roundToDecimals(item.getPrice().getValueChangeLastHour(), 1)
        );

    }

    public List<TimeSeriesDTO> getCPITimeSeries() {

        List<CPIInstant> instants = DatabaseManager.get().getDatabase().getCPIHistory();
        List<TimeSeriesDTO> timeSeries = new ArrayList<>();

        for (CPIInstant instant : instants) {

            timeSeries.add(
                    new TimeSeriesDTO(
                            instant.getLocalDateTime().toEpochSecond(ZoneOffset.UTC),
                            instant.getIndexValue()
                            )
            );
        }

        return timeSeries;
    }

    public List<TimeSeriesDTO> getItemTimeSeries(String identifier) {

        List<TimeSeriesDTO> timeSeries = new ArrayList<>();
        Set<Long> seenTimestamps = new HashSet<>();

        Item item = getItem(identifier);
        if (item == null) return null;

        List<Instant> instants = DatabaseManager.get().getDatabase().getAllPrices(item);

        for (Instant instant : instants) {
            long timestamp = instant.getLocalDateTime().toEpochSecond(ZoneOffset.UTC);
            if (!seenTimestamps.contains(timestamp) && instant.getPrice() != 0) {
                seenTimestamps.add(timestamp);
                timeSeries.add(new TimeSeriesDTO(timestamp, instant.getPrice()));
            }
        }
        return timeSeries;
    }

    public List<CategoryDTO> getCategoriesDTO() {

        List<CategoryDTO> categoriesDTO = new ArrayList<>();

        for (Category category : categories) {

            categoriesDTO.add(
                    new CategoryDTO(
                            category.getIdentifier(),
                            category.getDisplayName(),
                            category.getDayChange()
                    )
            );
        }
        return categoriesDTO;
    }

    public List<PortfolioDTO> getTopPortfolios() {

        List<PortfolioDTO> portfolioDTO = new ArrayList<>();

        HashMap<UUID, Portfolio> top = DatabaseManager.get().getDatabase().getTopWorth(5);

        for (UUID uuid : top.keySet()) {

            portfolioDTO.add(
                    new PortfolioDTO(
                            DatabaseManager.get().getDatabase().getNameByUUID(uuid),
                            top.get(uuid).getInventoryValue() - DebtManager.getInstance().getDebtOfPlayer(uuid),
                            top.get(uuid).getContent()
                    )
            );
        }
        return portfolioDTO;
    }

}
