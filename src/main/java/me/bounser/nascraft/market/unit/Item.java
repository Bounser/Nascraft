package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.api.events.BuyTradableEvent;
import me.bounser.nascraft.api.events.SellTradableEvent;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.managers.MoneyManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.unit.plot.GraphData;
import me.bounser.nascraft.market.unit.plot.PlotData;
import me.bounser.nascraft.market.unit.stats.ItemStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.awt.image.BufferedImage;
import java.util.*;

public class Item implements Tradable {

    private ItemStack itemStack;
    private final String identifier;
    private String alias;
    private final BufferedImage icon;
    private Category category;

    private final Price price;

    private int operations;

    private int volume;

    private float collectedTaxes;

    // 30 min
    private final GraphData gdMinutes;
    // 1 day
    private final GraphData gdHours;

    private final PlotData plotData;

    // 60 (0-59) values representing the prices in the last 60 minutes.
    private List<Float> pricesHour;
    // 24 (0-23) values representing the prices in all 24 hours of the day.
    private List<Float> pricesDay;

    private ItemStats itemStats;

    private final HashMap<Child, Float> childs;

    public Item(ItemStack itemStack, String identifier, String alias, Category category, HashMap<Child, Float> childs, BufferedImage image){

        itemStack.setAmount(1);

        this.itemStack = itemStack;
        this.identifier = identifier;
        this.alias = alias;

        this.price = new Price(
                this,
                Config.getInstance().getInitialPrice(identifier),
                Config.getInstance().getElasticity(identifier),
                Config.getInstance().getSupport(identifier),
                Config.getInstance().getResistance(identifier),
                Config.getInstance().getNoiseIntensity(identifier));

        this.icon = image;

        DatabaseManager.get().getDatabase().retrieveItem(this);

        float lastPrice = DatabaseManager.get().getDatabase().retrieveLastPrice(this);
        pricesHour = new ArrayList<>(Collections.nCopies(60, lastPrice));
        pricesDay = new ArrayList<>(Collections.nCopies(48, lastPrice));

        this.category = category;
        operations = 0;
        this.childs = childs;

        gdMinutes = new GraphData(TimeSpan.HOUR, pricesHour);
        gdHours = new GraphData(TimeSpan.DAY, pricesDay);

        plotData = new PlotData(this);

        itemStats = new ItemStats(this);
    }

    public String getName() { return alias; }

    public void setPrice(TimeSpan timeSpan, List<Float> prices) {
        switch (timeSpan) {
            case HOUR: pricesHour = prices; break;
            case DAY: pricesDay = prices; break;
        }
    }

    public void addValueToDay(float value) {
        pricesDay.remove(0);
        pricesDay.add(value);
    }

    public void addValueToHour(float value) {
        pricesHour.remove(0);
        pricesHour.add(value);
    }

    @Override
    public float buyPrice(int amount) {
        return price.getProjectedCost(-amount, price.getBuyTaxMultiplier());
    }

    @Override
    public float sellPrice(int amount) {
        return price.getProjectedCost(amount, price.getSellTaxMultiplier());
    }

    @Override
    public void buy(int amount, UUID uuid, boolean feedback) {

        Player player = Bukkit.getPlayer(uuid);
        Player offlinePlayer = Bukkit.getPlayer(uuid);

        BuyTradableEvent event = new BuyTradableEvent(player, this, amount);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;

        if(!MarketManager.getInstance().getActive()) { Lang.get().message(player, Message.SHOP_CLOSED); return; }

        if (!checkBalance(player, feedback, amount)) return;
        if (!checkInventory(player, feedback, amount)) return;

        int maxSize = Math.round((itemStack.getType().getMaxStackSize())/(price.getElasticity()*4));
        int orderSize = amount / maxSize;
        int excess = amount % maxSize;

        float totalCost = 0;

        ItemStack operationItemStack = itemStack.clone();

        for (int i = 0 ; i < orderSize ; i++) {

            totalCost += price.getBuyPrice()*maxSize;

            price.changeStock(-maxSize);

            operationItemStack.setAmount(maxSize);

            if (player != null  && feedback) player.getInventory().addItem(operationItemStack);
        }

        if (excess > 0) {

            totalCost += price.getBuyPrice()*excess;

            price.changeStock(-excess);

            operationItemStack.setAmount(excess);

            if (player != null  && feedback) player.getInventory().addItem(operationItemStack);
        }

        MoneyManager.getInstance().withdraw(offlinePlayer, totalCost);

        totalCost = RoundUtils.round(totalCost);

        if (player != null && feedback) Lang.get().message(player, Message.BUY_MESSAGE, Formatter.format(totalCost, Style.ROUND_BASIC), String.valueOf(amount), alias);

        updateInternalValues(amount,
                amount,
                0,
                price.getValue()*price.getBuyTaxMultiplier());

        DatabaseManager.get().getDatabase().saveTrade(uuid, this, amount, totalCost, true, false);
        MarketManager.getInstance().addOperation();
    }


    public boolean checkBalance(Player player, boolean feedback, int amount) {
        if (!MoneyManager.getInstance().hasEnoughMoney(player, (float) (price.getProjectedCost(amount, price.getBuyTaxMultiplier())*1.2))) {
            if (player != null && feedback) Lang.get().message(player, Message.NOT_ENOUGH_MONEY);
            return false;
        }
        return true;
    }

    public boolean checkInventory(Player player, boolean feedback, int amount) {

        if (player == null) return true;

        if (player.getInventory().firstEmpty() == -1) {

            int untilFull = 0;

            for (ItemStack is : player.getInventory()) {
                if(is != null && is.isSimilar(itemStack)) {
                    untilFull += itemStack.getType().getMaxStackSize() - is.getAmount();
                }
            }
            if (untilFull < amount) {
                if (feedback) Lang.get().message(player, Message.NOT_ENOUGH_SPACE);
                return false;
            }

        } else {
            int slotsUsed = 0;

            for (ItemStack content : player.getInventory().getStorageContents())
                if (content != null && !content.getType().equals(Material.AIR)) slotsUsed++;

            if ((36 - slotsUsed) < (amount/itemStack.getType().getMaxStackSize())) {
                if (feedback) Lang.get().message(player, Message.NOT_ENOUGH_SPACE);
                return false;
            }
        }

        return true;
    }

    @Override
    public float sell(int amount, UUID uuid, boolean feedback) {

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        Player player = Bukkit.getPlayer(uuid);

        SellTradableEvent event = new SellTradableEvent(player, this, amount);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return -1;

        if (!MarketManager.getInstance().getActive()) {
            if (player != null && feedback) Lang.get().message(player, Message.SHOP_CLOSED);
            return -1;
        }

        ItemStack operationItemStack = itemStack.clone();

        operationItemStack.setAmount(1);

        if (player != null && feedback && !player.getInventory().containsAtLeast(operationItemStack, amount)) {
            Lang.get().message(player, Message.NOT_ENOUGH_ITEMS);
            return -1;
        }

        int maxSize = itemStack.getType().getMaxStackSize();
        int orderSize = amount / maxSize;
        int excess = amount % maxSize;

        float totalWorth = 0;

        for (int i = 0 ; i < orderSize ; i++) {

            totalWorth += price.getSellPrice()*maxSize;

            price.changeStock(maxSize);

            operationItemStack.setAmount(maxSize);

            if (player != null && feedback) player.getInventory().removeItem(operationItemStack);
        }

        if (excess > 0) {

            totalWorth += price.getSellPrice()*excess;

            price.changeStock(excess);

            operationItemStack.setAmount(excess);

            if (player != null && feedback) player.getInventory().removeItem(operationItemStack);
        }

        updateInternalValues(amount,
                amount,
                0,
                price.getValue()*price.getSellTaxMultiplier());

        MoneyManager.getInstance().deposit(offlinePlayer, totalWorth);

        totalWorth = RoundUtils.round(totalWorth);

        if (player != null && feedback) Lang.get().message(player, Message.SELL_MESSAGE, Formatter.format(totalWorth, Style.ROUND_BASIC), String.valueOf(amount), alias);

        DatabaseManager.get().getDatabase().saveTrade(uuid, this, amount, totalWorth, false, false);
        MarketManager.getInstance().addOperation();

        return totalWorth;
    }

    public void ghostBuyItem(int amount) {
        updateInternalValues(-amount, amount, -amount,price.getValue()*price.getBuyTaxMultiplier());
        MarketManager.getInstance().addOperation();
    }

    public void ghostSellItem(int amount) {
        updateInternalValues(amount, amount, amount, price.getValue()*price.getSellTaxMultiplier());
        MarketManager.getInstance().addOperation();
    }

    private void updateInternalValues(int operations, int volume, int stockChange, float taxes) {
        this.operations += operations;
        this.volume += volume;
        this.price.changeStock(stockChange);
        this.collectedTaxes += taxes;
    }

    public String getIdentifier() { return identifier; }

    public List<Material> getParentAndChildsMaterials() {
        List materials = new ArrayList();
        materials.add(itemStack.getType());
        materials.add(childs.keySet());

        return materials;
    }

    public Price getPrice() { return price; }

    public List<Float> getPrices(TimeSpan timeSpan) {
        switch (timeSpan) {
            case HOUR: return pricesHour;
            case DAY: return pricesDay;
            default: return null;
        }
    }

    public HashMap<Child, Float> getChilds() { return childs; }

    public int getOperations() { return operations; }

    public void lowerOperations() {
        if (operations > 10) {
            operations -= Math.round((float) operations/60f);
            operations -= 3;
        } else if (operations > 1){
            operations -= 1;
        }
    }

    public List<GraphData> getGraphData() { return Arrays.asList(gdMinutes, gdHours); }

    public GraphData getGraphData(TimeSpan timeSpan) {
        switch (timeSpan) {
            case HOUR: return gdMinutes;
            case DAY: return gdHours;
            default: return null;
        }
    }

    public PlotData getPlotData() { return plotData; }

    public int getVolume() { return volume; }

    public float getLow(TimeSpan timeSpan) { return Collections.min(getPrices(timeSpan)); }

    public float getHigh(TimeSpan timeSpan) { return Collections.max(getPrices(timeSpan)); }

    public float getCollectedTaxes() { return collectedTaxes; }

    public void setCollectedTaxes(float newCollectedTaxes) { collectedTaxes = newCollectedTaxes; }

    public void addVolume(int volume) { this.volume += volume; }

    public void restartVolume() { volume = 0; }

    public ItemStats getItemStats() { return itemStats; }

    public Category getCategory() { return category; }

    public void setCategory(Category category) { this.category = category; }

    public void changeProperties(float initialPrice, String alias, float elasticity, float noiseSensibility, float support, float resistance) {

        price.setInitialValue(initialPrice)
                .setElasticity(elasticity)
                .setNoiseIntensity(noiseSensibility)
                .setSupport(support)
                .setResistance(resistance);

        this.alias = alias;
    }

    public ItemStack getItemStack() { return itemStack.clone(); }

    public ItemStack getItemStack(int quantity) {
        ItemStack clonedItemStack = itemStack.clone();
        clonedItemStack.setAmount(quantity);
        return clonedItemStack;
    }

    public void setItemStack(ItemStack itemStack) { this.itemStack = itemStack; }


    public BufferedImage getIcon() { return icon; }

}
