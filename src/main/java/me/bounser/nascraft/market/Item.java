package me.bounser.nascraft.market;

import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.Data;

import java.util.HashMap;
import java.util.List;

public class Item {

    String mat;
    float price;
    Category cat;

    int stock;

    int nextMoveBuy;
    int nextMoveSell;
    int required;

    // 24 (0-23) prices representing the prices in all 24 hours of the day
    List<Float> prices;
    HashMap<String, Float> childs;

    public Item(String material, Category category){

        required = Config.getInstance().getRequiredToMove(category.getName());
        nextMoveBuy = required;
        nextMoveSell = required;
        mat = material;
        // setPrices();
        cat = category;
        // this.childs = Config.getInstance().getChilds(material);

    }

    public void setPrices() {

        float[] p = Data.getInstance().getPrice(mat);

        // If the price wasn't updated in the prev 24H or if it wasn't saved, all the record is the last price or the initial one.
        if(p[1] > 23 || p[1] == -1){
            price = p[0];
            for(int i = 0; i <= 23; i++) prices.add(price);
        } else {
            for(int i = 0; i<= p[1]; i++) prices.add(p[0]);
        }

    }

    public void changePrice(float percentage) {
        price += price * percentage;
    }

    public float buyItem(int amount) {

        if(nextMoveBuy + amount > required){

            if(stock < 0){
                this.price = (float) (price*((Math.random() * (1.15 - 1.0) + stock*0.00001)));
            } else {
                this.price = (float) (price*((Math.random() * (1.1 - 1.0))));
            }
            buyItem(amount - required);

        } else {
            nextMoveBuy += amount;
        }
        stock -= amount;
        return price += price * Config.getInstance().getTaxBuy();
    }

    public float sellItem(int amount) {

        if(nextMoveSell + amount > required){

            if(stock > 0){
                this.price = (float) (price*((Math.random() * (0.85 - 1.0) - stock*0.00001)));
            } else {
                this.price = (float) (price*((Math.random() * (0.90 - 1.0))));
            }
            sellItem(amount-required);

        } else {
            nextMoveSell += amount;
        }
        stock += amount;
        return price -= price*Config.getInstance().getTaxSell();
    }

    public String getMaterial() { return mat; }
    public float getPrice() { return price; }
    public int getStock() { return stock; }
    public String getCategory() { return cat.getName(); }
    public HashMap<String, Float> getChilds() { return childs; }

}
