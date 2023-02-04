package me.bounser.nascraft.market;

import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.Data;
import org.bukkit.Bukkit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;

public class Item {

    String mat;
    float price;
    Category cat;

    int stock;

    int operations;

    // 24 (0-23) prices representing the prices in all 24 hours of the day
    List<Float> pricesH;
    // 15 (0-14) prices representing the prices in the last 15 minutes
    List<Float> pricesM;
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

    public float buyItem(int amount) {

        operations++;

        if(price < 10) {
            if(Math.random() < amount * 0.01 - stock * 0.001)
            price += 0.01;
        } else {
            price = round((float) (price* (1 +(Math.random() * 0.05) - stock*0.0001)));
        }

        stock -= amount;
        return price + round(price * Config.getInstance().getTaxBuy());
    }

    public float sellItem(int amount) {

        operations++;

        if(price < 10) {
            if(Math.random() < amount * 0.01 - stock * 0.001)
                price -= 0.01;
        } else {
            price = round((float) (price* (1 -(Math.random() * 0.05) + stock*0.0001)));
        }
        stock += amount;
        return price - round(price*Config.getInstance().getTaxSell());
    }

    public String getMaterial() { return mat; }

    public float getPrice() { return round(price); }

    public List<Float> getPricesH() { return pricesH; }
    public List<Float> getPricesM() { return pricesM; }

    public int getStock() { return stock; }

    public String getCategory() { return cat.getName(); }

    public HashMap<String, Float> getChilds() { return childs; }

    public int getOperations() { return operations; }

    public static float round(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(Config.getInstance().getDecimalPrecission(), RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public float getBuyPrice() {
        return round(price + price*Config.getInstance().getTaxBuy());
    }
    public float getSellPrice() {
        return round(price - price*Config.getInstance().getTaxSell());
    }

}
