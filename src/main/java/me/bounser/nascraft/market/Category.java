package me.bounser.nascraft.market;

import java.util.ArrayList;
import java.util.List;

public class Category {

    String name;
    List<Item> items = new ArrayList<>();

    public Category(String name) {
        this.name = name;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public int getNumOfItems() {
        return items.toArray().length;
    }

    public Item getItemOfIndex(int index) {
        return items.get(index);
    }

    public String getName() {
        return name;
    }


}
