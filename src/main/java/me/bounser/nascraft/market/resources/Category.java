package me.bounser.nascraft.market.resources;

import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.config.Config;

import java.util.ArrayList;
import java.util.List;

public class Category {

    private final String name;
    private final List<Item> items = new ArrayList<>();

    public Category(String name) {
        this.name = name;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public int getNumOfItems() {
        return items.size();
    }

    public Item getItemOfIndex(int index) {
        return items.get(index);
    }

    public String getName() {
        return name;
    }

    public List<Item> getItems() {
        return items;
    }

    public String getDisplayName() {
        return Config.getInstance().getDisplayName(this);
    }

}
