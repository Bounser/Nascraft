package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.SQLite;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.managers.MarketManager;
import me.bounser.nascraft.managers.MoneyManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.formatter.Style;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Item {

    private final Material material;
    private final String alias;
    private final Category category;

    private final Price price;

    private int operations;

    private float volume;

    private float collectedTaxes;

    // 30 min
    private final GraphData gdMinutes;
    // 1 day
    private final GraphData gdHours;
    // 1 month
    private final GraphData gdDays;
    // 1 year
    private final GraphData gdMonth;

    private final PlotData plotData;

    // 60 (0-59) values representing the prices in the last 60 minutes.
    private List<Float> pricesHour;
    // 24 (0-23) values representing the prices in all 24 hours of the day.
    private List<Float> pricesDay;
    // 30 (0-29) values representing the prices in the last month. *
    private List<Float> pricesMonth;
    // 24 (0-23) values representing 2 prices each month. *
    private List<Float> pricesYear;

    private final HashMap<Material, Float> childs;

    public Item(Material material, String alias, Category category){
        this.material = material;
        this.alias = alias;

        this.price = new Price(
                this,
                Config.getInstance().getInitialPrice(material),
                Config.getInstance().getElasticity(material),
                Config.getInstance().getSupport(material),
                Config.getInstance().getResistance(material),
                Config.getInstance().getNoiseIntensity(material));

        SQLite.getInstance().retrievePrices(this);
        SQLite.getInstance().retrieveItem(this);

        float lastPrice = SQLite.getInstance().retrieveLastPrice(this);
        pricesHour = new ArrayList<>(Collections.nCopies(60, lastPrice));

        if (pricesDay == null) {
            pricesDay = new ArrayList<>(Collections.nCopies(48, lastPrice));
            pricesMonth = new ArrayList<>(Collections.nCopies(30, lastPrice));
            pricesYear = new ArrayList<>(Collections.nCopies(51, lastPrice));
        }

        this.category = category;
        operations = 0;
        this.childs = Config.getInstance().getChilds(this.material);

        gdMinutes = new GraphData(TimeSpan.HOUR, pricesHour);
        gdHours = new GraphData(TimeSpan.DAY, pricesDay);
        gdDays = new GraphData(TimeSpan.MONTH, pricesMonth);
        gdMonth = new GraphData(TimeSpan.YEAR, pricesYear);

        plotData = new PlotData(this);
    }

    public String getName() { return alias; }

    public void setPrice(TimeSpan timeSpan, List<Float> prices) {
        switch (timeSpan) {
            case HOUR: pricesHour = prices; break;
            case DAY: pricesDay = prices; break;
            case MONTH: pricesMonth = prices; break;
            case YEAR: pricesYear = prices; break;
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

    public void buyItem(int amount, UUID uuid, boolean feedback) {

        Player player = Bukkit.getPlayer(uuid);
        Player offlinePlayer = Bukkit.getPlayer(uuid);

        if(!MarketManager.getInstance().getActive()) { Lang.get().message(player, Message.SHOP_CLOSED); return; }

        if (!checkBalance(player, feedback, amount)) return;
        if (!checkInventory(player, feedback, amount)) return;

        int maxSize = material.getMaxStackSize();
        int orderSize = amount / maxSize;
        int excess = amount % maxSize;

        float totalCost = 0;

        for (int i = 0 ; i < orderSize ; i++) {

            totalCost += price.getBuyPrice()*maxSize*childs.get(material);

            price.changeStock(-maxSize);

            if (player != null  && feedback) player.getInventory().addItem(new ItemStack(material, maxSize));
        }

        if (excess > 0) {

            totalCost += price.getBuyPrice()*excess*childs.get(material);

            price.changeStock(-excess);

            if (player != null  && feedback) player.getInventory().addItem(new ItemStack(material, excess));
        }

        totalCost = RoundUtils.round(totalCost);

        MoneyManager.getInstance().withdraw(offlinePlayer, totalCost);

        if (player != null && feedback) Lang.get().message(player, Message.BUY_MESSAGE, Formatter.format(totalCost, Style.ROUND_BASIC), String.valueOf(amount), alias);

        updateInternalValues(amount,
                amount*price.getBuyPrice(),
                0,
                price.getValue()*price.getBuyTaxMultiplier());

        SQLite.getInstance().saveTrade(uuid, this, amount, totalCost, true, false);
        MarketManager.getInstance().addOperation();
    }

    public boolean checkBalance(Player player, boolean feedback, int amount) {
        if (!Nascraft.getEconomy().has(player, price.getBuyPrice()*amount*childs.get(material))) {
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
                if(is != null && is.getType().equals(material)) {
                    untilFull += material.getMaxStackSize() - is.getAmount();
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

            if ((36 - slotsUsed) < (amount/material.getMaxStackSize())) {
                if (feedback) Lang.get().message(player, Message.NOT_ENOUGH_SPACE);
                return false;
            }
        }

        return true;
    }

    public float sellItem(int amount, UUID uuid, boolean feedback) {

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        Player player = Bukkit.getPlayer(uuid);

        if (!MarketManager.getInstance().getActive()) {
            if (player != null && feedback) Lang.get().message(player, Message.SHOP_CLOSED);
            return -1;
        }

        if (player != null &&  feedback && !player.getInventory().containsAtLeast(new ItemStack(material), amount)) {
            Lang.get().message(player, Message.NOT_ENOUGH_ITEMS);
            return -1;
        }

        int maxSize = material.getMaxStackSize();
        int orderSize = amount / maxSize;
        int excess = amount % maxSize;

        float totalWorth = 0;

        for (int i = 0 ; i < orderSize ; i++) {

            totalWorth += price.getSellPrice()*maxSize*childs.get(material);

            price.changeStock(maxSize);

            if (player != null && feedback) player.getInventory().removeItem(new ItemStack(material, maxSize));
        }

        if (excess > 0) {

            totalWorth += price.getSellPrice()*excess*childs.get(material);

            price.changeStock(excess);

            if (player != null && feedback) player.getInventory().removeItem(new ItemStack(material, excess));
        }

        totalWorth = RoundUtils.round(totalWorth);

        updateInternalValues(amount,
                amount*price.getSellPrice(),
                0,
                price.getValue()*price.getSellTaxMultiplier());

        MoneyManager.getInstance().deposit(offlinePlayer, totalWorth);

        if (player != null && feedback) Lang.get().message(player, Message.SELL_MESSAGE, Formatter.format(totalWorth, Style.ROUND_BASIC), String.valueOf(amount), alias);

        SQLite.getInstance().saveTrade(uuid, this, amount, totalWorth, false, false);
        MarketManager.getInstance().addOperation();

        return totalWorth;
    }

    public void ghostBuyItem(int amount) {
        updateInternalValues(-amount, amount*price.getBuyPrice(), amount,price.getValue()*price.getBuyTaxMultiplier());
        MarketManager.getInstance().addOperation();
    }

    public void ghostSellItem(int amount) {
        updateInternalValues(amount, amount*price.getSellPrice(), amount, price.getValue()*price.getSellTaxMultiplier());
        MarketManager.getInstance().addOperation();
    }

    private void updateInternalValues(int operations, float volume, int stockChange, float taxes) {
        this.operations += operations;
        this.volume += RoundUtils.round(volume);
        this.price.changeStock(stockChange);
        this.collectedTaxes += taxes;
    }

    public void dailyUpdate() {
        pricesMonth.remove(0);
        pricesMonth.add(price.getValue());
    }

    public Material getMaterial() { return material; }

    public List<Material> getParentAndChildsMaterials() {
        List materials = new ArrayList();
        materials.add(material);
        materials.add(childs.keySet());

        return materials;
    }

    public Price getPrice() { return price; }

    public List<Float> getPrices(TimeSpan timeSpan) {
        switch (timeSpan) {
            case HOUR: return pricesHour;
            case DAY: return pricesDay;
            case MONTH: return pricesMonth;
            case YEAR: return pricesYear;
            default: return null;
        }
    }

    public HashMap<Material, Float> getChilds() { return childs; }

    public int getOperations() { return operations; }

    public void lowerOperations() {
        if (operations > 10) {
            operations -= Math.round((float) operations/60f);
            operations -= 3;
        } else if (operations > 1){
            operations -= 1;
        }
    }

    public List<GraphData> getGraphData() { return Arrays.asList(gdMinutes, gdHours, gdDays, gdMonth); }

    public GraphData getGraphData(TimeSpan timeSpan) {
        switch (timeSpan) {
            case HOUR: return gdMinutes;
            case DAY: return gdHours;
            case MONTH: return gdDays;
            case YEAR: return gdMonth;
            default: return null;
        }
    }

    public PlotData getPlotData() { return plotData; }

    public float getVolume() { return volume; }

    public float getLow(TimeSpan timeSpan) { return Collections.min(getPrices(timeSpan)); }

    public float getHigh(TimeSpan timeSpan) { return Collections.max(getPrices(timeSpan)); }

    public float getCollectedTaxes() { return collectedTaxes; }

    public void setCollectedTaxes(float newCollectedTaxes) { collectedTaxes = newCollectedTaxes; }

}
