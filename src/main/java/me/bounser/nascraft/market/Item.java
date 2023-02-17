package me.bounser.nascraft.market;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.Data;
import me.bounser.nascraft.tools.NUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

public class Item {

    String mat;
    float price;
    Category cat;
    Trend trend;

    int stock;

    int operations;

    // 15 (0-14) prices representing the prices in the last 15 minutes.
    List<Float> pricesM;
    // 24 (0-23) prices representing the prices in all 24 hours of the day.
    List<Float> pricesH;
    // 30 (0-29) prices representing the prices in the last month.
    List<Float> pricesMM;
    // 24 (0-23) prices representing 2 prices each month.
    List<Float> pricesY;

    HashMap<String, Float> childs;

    public Item(String material, Category category){

        mat = material;
        setupPrices();
        cat = category;
        operations = 0;
        this.childs = Config.getInstance().getChilds(material, category.getName());
        trend = Trend.valueOf(Config.getInstance().getItemDefaultTrend(category.getName(), material));

    }

    public void setPrice(float price) {
        this.price = price;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public void setupPrices() {
        pricesH = Data.getInstance().getHPrice(mat);
        pricesM = Data.getInstance().getMPrice(mat);
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

        if(price + NUtils.round(price * percentage/100) > Math.pow(10, -Config.getInstance().getDecimalPrecission())) {
            price += NUtils.round(price * percentage/100);
        }
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

                if(is.getType().toString().equals(mat.toUpperCase()) && amount < is.getMaxStackSize() - is.getAmount()) { hasSpace = true; }

            }
            if(!hasSpace) {
                player.sendMessage(ChatColor.RED + "Not enough space in inventory!");
                return;
            }
        }

        econ.depositPlayer(player, -getBuyPrice()*amount*multiplier);

        player.getInventory().addItem(new ItemStack(Material.getMaterial(mat.toUpperCase()), amount));

        player.sendMessage(ChatColor.GRAY + "You just bought " + ChatColor.AQUA + amount + ChatColor.GRAY + " x " + ChatColor.AQUA + mat + ChatColor.GRAY + " worth " + ChatColor.GOLD + getBuyPrice()*amount*multiplier);

        if (price < 10) {
            if (Math.random() < (amount * 0.01 - stock * 0.001))
                price += 0.01;
        } else {
            float val = NUtils.round((float) (price* (1 +(Math.random() * 0.001 * amount) - stock*0.0001)));

            if(val < Config.getInstance().getLimits()[1]) price = val;
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

        player.sendMessage(ChatColor.GRAY + "You just sold " + ChatColor.AQUA + amount + ChatColor.GRAY + " x " + ChatColor.AQUA + mat + ChatColor.GRAY + " worth " + ChatColor.GOLD + getSellPrice()*amount*multiplier);

        if(price < 10) {
            if(Math.random() < (amount * 0.01 - stock * 0.001))
                if(price - 0.01 > Config.getInstance().getLimits()[0]) price -= 0.01;
        } else {
            price = NUtils.round((float) (price* (1 -(Math.random() * 0.001 * amount) + stock*0.0001)));
        }
        operations += amount;
        stock += amount;
    }

    public String getMaterial() { return mat; }

    public float getPrice() { return NUtils.round(price); }

    public List<Float> getPricesH() { return pricesH; }
    public List<Float> getPricesM() { return pricesM; }
    public List<Float> getPricesMM() { return pricesMM; }
    public List<Float> getPricesY() { return pricesY; }

    public int getStock() { return stock; }

    public String getCategory() { return cat.getName(); }

    public HashMap<String, Float> getChilds() {
        return childs;
    }

    public int getOperations() { return operations; }

    public void lowerOperations() {

        if(operations > 10) {
            operations = operations - Math.round((float) operations/4f);
            operations -= 5;
        } else if (operations > 1){
            operations -= 1;
        }
    }

    public float getBuyPrice() { return NUtils.round(price + price*Config.getInstance().getTaxBuy()); }
    public float getSellPrice() { return NUtils.round(price - price*Config.getInstance().getTaxSell()); }

    public Trend getTrend() { return trend; }

}
