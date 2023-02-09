package me.bounser.nascraft.market;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.Data;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Item {

    String mat;
    float price;
    Category cat;

    int stock;

    int operations;

    // 24 (0-23) prices representing the prices in all 24 hours of the day.
    List<Float> pricesH;
    // 15 (0-14) prices representing the prices in the last 15 minutes.
    List<Float> pricesM;
    // 30 (0-29) prices representing the prices in the last month.
    List<Float> pricesMM;

    HashMap<String, Float> childs;

    public Item(String material, Category category){

        mat = material;
        setupPrices();
        cat = category;
        operations = 0;
        this.childs = Config.getInstance().getChilds(material, category.getName());

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

        price = pricesM.get(pricesM.size()-1);
    }

    public void addValueToH(float value) {
        pricesH.remove(0);
        pricesH.add(round(value));
    }

    public void addValueToM(float value) {
        pricesM.remove(0);
        pricesM.add(round(value));
    }

    public void changePrice(float percentage) {
        price += round(price * percentage);
    }

    public void buyItem(int amount, Player player) {

        Economy econ = Nascraft.getEconomy();

        if (!econ.has(player, getBuyPrice()*amount)) {
            player.sendMessage(ChatColor.RED + "You can't afford to pay that!");
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

        econ.depositPlayer(player, -getBuyPrice()*amount);

        player.getInventory().addItem(new ItemStack(Material.getMaterial(mat.toUpperCase()), amount));

        player.sendMessage(ChatColor.GRAY + "You just bought " + ChatColor.AQUA + amount + ChatColor.GRAY + " x " + ChatColor.AQUA + mat + ChatColor.GRAY + " worth " + ChatColor.GOLD + getBuyPrice()*amount);

        if (price < 10) {
            if (Math.random() < amount * 0.01 - stock * 0.001)
                price +=  + 0.01;
        } else {
            price = round((float) (price* (1 +(Math.random() * 0.005 * amount) - stock*0.0001)));
        }
        operations++;
        stock -= amount;
    }

    public void sellItem(int amount, Player player) {

        if (!player.getInventory().contains(new ItemStack(Material.getMaterial(mat.toUpperCase()), amount))) {
            player.sendMessage(ChatColor.RED + "Not enough items to sell.");
        }

        player.getInventory().removeItem(new ItemStack(Material.getMaterial(mat.toUpperCase()), amount));

        Nascraft.getEconomy().depositPlayer(player, getSellPrice()*amount);

        player.sendMessage(ChatColor.GRAY + "You just sold " + ChatColor.AQUA + amount + ChatColor.GRAY + " x " + ChatColor.AQUA + mat + ChatColor.GRAY + " worth " + ChatColor.GOLD + getSellPrice()*amount);

        if(price < 10) {
            if(Math.random() < amount * 0.01 - stock * 0.001)
                price -= 0.01;
        } else {
            price = round((float) (price* (1 -(Math.random() * 0.005 * amount) + stock*0.0001)));
        }
        operations++;
        stock += amount;
    }

    public String getMaterial() { return mat; }

    public float getPrice() { return round(price); }

    public List<Float> getPricesH() { return pricesH; }
    public List<Float> getPricesM() { return pricesM; }
    public List<Float> getPricesMM() { return pricesMM; }

    public int getStock() { return stock; }

    public String getCategory() { return cat.getName(); }

    public HashMap<String, Float> getChilds() { return childs; }

    public int getOperations() { return operations; }

    public static float round(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(Config.getInstance().getDecimalPrecission(), RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public float getBuyPrice() { return round(price + price*Config.getInstance().getTaxBuy()); }
    public float getSellPrice() { return round(price - price*Config.getInstance().getTaxSell()); }

}
