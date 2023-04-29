package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.managers.resources.Category;
import me.bounser.nascraft.market.managers.resources.TimeSpan;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.Data;
import me.bounser.nascraft.tools.NUtils;
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
    private final Category cat;

    private final Price price;

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

        this.price = new Price(setupPrices(),
                Data.getInstance().getStock(mat),
                Config.getInstance().getElasticity(mat, category.getName()),
                Config.getInstance().getSupport(mat, category.getName()),
                Config.getInstance().getResistance(mat, category.getName()),
                Config.getInstance().getNoiseIntensity(mat, category.getName()));

        cat = category;
        operations = 0;
        this.childs = Config.getInstance().getChilds(material, category.getName());

        gd1 = new GraphData(TimeSpan.MINUTE, pricesM);
        gd2 = new GraphData(TimeSpan.DAY, pricesH);
        gd3 = new GraphData(TimeSpan.MONTH, pricesMM);
        gd4 = new GraphData(TimeSpan.YEAR, pricesY);
    }

    public String getName() { return alias; }

    public float setupPrices() {
        pricesM = Data.getInstance().getMPrice(mat);
        pricesH = Data.getInstance().getHPrice(mat);
        pricesMM = Data.getInstance().getMMPrice(mat);
        pricesY = Data.getInstance().getYPrice(mat);

        return pricesM.get(pricesM.size()-1);
    }

    public void addValueToH(float value) {
        pricesH.remove(0);
        pricesH.add(NUtils.round(value));
    }

    public void addValueToM(float value) {
        pricesM.remove(0);
        pricesM.add(NUtils.round(value));
    }

    public void buyItem(int amount, Player player, String mat, float multiplier) {

        Economy econ = Nascraft.getEconomy();

        if (!econ.has(player, price.getBuyPrice()*amount*multiplier)) {
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

        econ.withdrawPlayer(player, price.getBuyPrice()*amount*multiplier);

        player.getInventory().addItem(new ItemStack(Material.getMaterial(mat.toUpperCase()), amount));

        String msg = Config.getInstance().getBuyMessage().replace("&", "§").replace("[AMOUNT]", String.valueOf(amount)).replace("[WORTH]", String.valueOf(NUtils.round(price.getBuyPrice()*amount*multiplier))).replace("[MATERIAL]", mat);

        player.sendMessage(msg);

        price.changeStock(-amount);

        operations += amount;
    }

    public void sellItem(int amount, Player player, String mat, float multiplier) {

        if (!player.getInventory().containsAtLeast(new ItemStack(Material.getMaterial(mat.toUpperCase())), amount)) {
            player.sendMessage(ChatColor.RED + "Not enough items to sell.");
            return;
        }

        player.getInventory().removeItem(new ItemStack(Material.getMaterial(mat.toUpperCase()), amount));

        Nascraft.getEconomy().depositPlayer(player, price.getSellPrice()*amount*multiplier);

        String msg = Config.getInstance().getSellMessage().replace("&", "§").replace("[AMOUNT]", String.valueOf(amount)).replace("[WORTH]", String.valueOf(NUtils.round(price.getBuyPrice()*amount*multiplier))).replace("[MATERIAL]", mat.replace("_", ""));

        player.sendMessage(msg);

        price.changeStock(amount);

        operations += amount;
    }

    public void dailyUpdate() {
        pricesMM.remove(0);
        pricesMM.add(price.getValue());

        pricesY = Data.getInstance().getYPrice(mat);
    }

    public String getMaterial() { return mat; }

    public Price getPrice() { return price; }

    public List<Float> getPrices(TimeSpan timeSpan) {
        switch (timeSpan) {
            case MINUTE: return pricesM;
            case DAY: return pricesH;
            case MONTH: return pricesMM;
            case YEAR: return pricesY;
            default: return null;
        }
    }

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

}
