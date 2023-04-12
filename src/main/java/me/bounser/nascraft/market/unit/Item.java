package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.managers.resources.Category;
import me.bounser.nascraft.market.managers.resources.TimeSpan;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.Data;
import me.bounser.nascraft.tools.NUtils;
import me.bounser.nascraft.market.managers.resources.Trend;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Item {

    private final String mat;
    private final String alias;
    private float price;
    private final Category cat;
    private final Trend trend;

    private int stock;
    private int operations;

    // 30 min
    private final GraphData gd1;
    // 1 day
    private final GraphData gd2;
    // 1 month
    private final GraphData gd3;
    // 1 year
    private final GraphData gd4;

    // 30 (0-29) values representing the prices in the last 30 minutes.
    private List<Float> pricesM;
    // 24 (0-23) values representing the prices in all 24 hours of the day.
    private List<Float> pricesH;
    // 30 (0-29) values representing the prices in the last month. *
    private List<Float> pricesMM;
    // 24 (0-23) values representing 2 prices each month. *
    private List<Float> pricesY;

    private final HashMap<String, Float> childs;

    public Item(String material, String alias, Category category){
        mat = material;
        this.alias = alias;
        setupPrices();
        cat = category;
        operations = 0;
        this.childs = Config.getInstance().getChilds(material, category.getName());
        trend = Trend.valueOf(Config.getInstance().getItemDefaultTrend(category.getName(), material));

        stock = Data.getInstance().getStock(material);

        gd1 = new GraphData(TimeSpan.MINUTE, pricesM);
        gd2 = new GraphData(TimeSpan.DAY, pricesH);
        gd3 = new GraphData(TimeSpan.MONTH, pricesMM);
        gd4 = new GraphData(TimeSpan.YEAR, pricesY);
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public void setupPrices() {
        pricesM = Data.getInstance().getMPrice(mat);
        pricesH = Data.getInstance().getHPrice(mat);
        pricesMM = Data.getInstance().getMMPrice(mat);
        pricesY = Data.getInstance().getYPrice(mat);

        price = pricesM.get(pricesM.size()-1);
    }

    public void addValueToH(float value) {
        pricesH.remove(0);
        pricesH.add(NUtils.round(value));
    }

    public void addValueToM(float value) {
        pricesM.remove(0);
        pricesM.add(NUtils.round(value));
    }

    public void changePrice(float percentage) {
        if (price + NUtils.round(price * percentage/100) > Math.pow(10, -Config.getInstance().getDecimalPrecission())) {
            price += NUtils.round(price * percentage/100);
        }
        if (price < Config.getInstance().getLimits()[0]) price = Config.getInstance().getLimits()[0];
        if (price > Config.getInstance().getLimits()[1]) price = Config.getInstance().getLimits()[1];
    }

    public void buyItem(int amount, Player player, String mat, float multiplier) {

        Economy econ = Nascraft.getEconomy();

        if (!econ.has(player, getBuyPrice()*amount*multiplier)) {
            player.sendMessage(ChatColor.RED + "You can't afford to pay that!");
            return;
        }

        boolean hasSpace = false;

        if (player.getInventory().firstEmpty() == -1) {
            for (ItemStack is : player.getInventory()) {
                if(is != null && is.getType().toString().equals(mat.toUpperCase()) && amount < is.getMaxStackSize() - is.getAmount()) { hasSpace = true; }
            }
            if (!hasSpace) {
                player.sendMessage(ChatColor.RED + "Not enough space in inventory!");
                return;
            }
        }

        econ.withdrawPlayer(player, getBuyPrice()*amount*multiplier);

        player.getInventory().addItem(new ItemStack(Material.getMaterial(mat.toUpperCase()), amount));

        String msg = Config.getInstance().getBuyMessage().replace("&", "§").replace("[AMOUNT]", String.valueOf(amount)).replace("[WORTH]", String.valueOf(NUtils.round(getBuyPrice()*amount*multiplier))).replace("[MATERIAL]", mat);

        player.sendMessage(msg);

        if (price < Math.random()*20 + 30) {
            if (Math.random() < (amount * 0.01 * (2/(1+Math.exp(-stock*0.0001)))))
                price += 0.01;
        } else {
            float val = NUtils.round((float) (price + price*0.01*(1 + 0.5/(1+Math.exp(-stock*0.0001))) + amount*0.1));
            price = Math.min(val, Config.getInstance().getLimits()[1]);
        }
        operations += amount;
        stock -= amount;
    }

    public void sellItem(int amount, Player player, String mat, float multiplier) {

        if (!player.getInventory().containsAtLeast(new ItemStack(Material.getMaterial(mat.toUpperCase())), amount)) {
            player.sendMessage(ChatColor.RED + "Not enough items to sell.");
            return;
        }

        player.getInventory().removeItem(new ItemStack(Material.getMaterial(mat.toUpperCase()), amount));

        Nascraft.getEconomy().depositPlayer(player, getSellPrice()*amount*multiplier);

        String msg = Config.getInstance().getSellMessage().replace("&", "§").replace("[AMOUNT]", String.valueOf(amount)).replace("[WORTH]", String.valueOf(NUtils.round(getBuyPrice()*amount*multiplier))).replace("[MATERIAL]", mat.replace("_", ""));

        player.sendMessage(msg);

        if (price < Math.random()*20 + 30) {
            if (Math.random() < (amount * 0.01 * (2/(1+Math.exp(-stock*0.0001))))) {
                if (price - 0.01 > Config.getInstance().getLimits()[0]) price -= 0.01;
                else price = Config.getInstance().getLimits()[0];
            }

        } else {
            if (Math.random() < amount * 0.01) {
                price = NUtils.round((float) (price - price*0.01*(1 + 0.5/(1+Math.exp(stock*0.0001))) + amount*0.1));
            }
        }
        operations += amount;
        stock += amount;
    }

    public void dailyUpdate() {
        pricesMM.remove(0);
        pricesMM.add(price);

        pricesY = Data.getInstance().getYPrice(mat);
    }

    public String getMaterial() { return mat; }

    public float getPrice() { return NUtils.round(price); }

    public List<Float> getPrices(TimeSpan timeSpan) {
        switch (timeSpan) {
            case MINUTE: return pricesM;
            case DAY: return pricesH;
            case MONTH: return pricesMM;
            case YEAR: return pricesY;
            default: return null;
        }
    }

    public int getStock() { return stock; }

    public String getCategory() { return cat.getName(); }

    public HashMap<String, Float> getChilds() { return childs; }

    public int getOperations() { return operations; }

    public void lowerOperations() {
        if (operations > 10) {
            operations -= Math.round((float) operations/10f);
            operations -= 5;
        } else if (operations > 1){
            operations -= 1;
        }
    }

    public float getBuyPrice() { return NUtils.round(price + price*Config.getInstance().getTaxBuy()); }
    public float getSellPrice() { return NUtils.round(price - price*Config.getInstance().getTaxSell()); }

    public Trend getTrend() { return trend; }

    public List<GraphData> getGraphData() {
        return Arrays.asList(gd1, gd2, gd3, gd4);
    }

    public GraphData getGraphData(TimeSpan timeSpan) {
        switch (timeSpan) {
            case MINUTE: return gd1;
            case DAY: return gd2;
            case MONTH: return gd3;
            case YEAR: return gd4;
            default: return null;
        }
    }

    public String getName() { return alias; }

    public String getMaterialName() { return mat; }

}
