package me.bounser.nascraft.market;

import org.bukkit.Material;

public class Item {

    Material mat;
    float price;


    public Item(String material, float price){

        mat = Material.getMaterial(material);
        if(price == 0) {
            price = 0; // Assign default price
        } else {
            this.price = price;
        }

    }





}
