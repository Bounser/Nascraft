package me.bounser.nascraft.market;

import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.Data;
import org.bukkit.Material;

public class Item {

    Material mat;
    float price;

    int stock;

    int nextMoveBuy;
    int nextMoveSell;
    int required;

    public Item(String material, float price, int category){

        required = Config.getInstance().getRequiredToMove(category);
        nextMoveBuy = required;
        nextMoveSell = required;
        mat = Material.getMaterial(material);
        float p = Data.getInstance().getPrice(material);
        if(p != price)
        this.price = price;

    }

    public void changePrice(float percentage) {
        price = price + price * percentage;
    }

    public float buyItem(int amount) {

        if(nextMoveBuy + amount > required){

            int priceChange = (amount + nextMoveBuy) % required;

            for(int i = 0 ; i < priceChange ; i++) {

                if(stock < 0){
                    this.price = (float) (price*((Math.random() * (1.15 - 1.0) + stock*0.00001)));
                } else {
                    this.price = (float) (price*((Math.random() * (1.1 - 1.0))));
                }
            }
        }
        stock = stock + amount;
        return price;
    }

    public float sellItem(int amount) {

        if(nextMoveSell + amount > required){

            int priceChange = (amount + nextMoveSell) % required;

            for(int i = 0 ; i < priceChange ; i++) {

                if(stock > 0){
                    this.price = (float) (price*((Math.random() * (0.85 - 1.0) - stock*0.00001)));
                } else {
                    this.price = (float) (price*((Math.random() * (0.90 - 1.0))));
                }
            }
        }
        stock = stock - amount;
        return price;
    }

    public Material getMaterial(){ return mat; }
    public float getPrice() { return price; }
    public int getStock() { return stock; }


}
