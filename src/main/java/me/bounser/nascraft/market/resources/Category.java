package me.bounser.nascraft.market.resources;

import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.config.Config;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;


public class Category {


    private String identifier;

    private String displayName;

    private Material material;

    private List<Item> items = new ArrayList<>();

    public Category(String identifier) {
        this.identifier = identifier;
        this.displayName = Config.getInstance().getDisplayName(this);
        this.material = Config.getInstance().getMaterialOfCategory(this);
    }

    public void addItem(Item item) { items.add(item); }

    public void removeItem(Item item) { items.remove(item); }

    //

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public void setDisplayMaterial(Material material) { this.material = material; }

    //

    public int getNumberOfItems() { return items.size(); }

    public Item getItemOfIndex(int index) { return items.get(index); }

    public String getIdentifier() { return identifier; }

    public String getDisplayName() { return displayName; }

    public Material getMaterial() { return material; }

    public List<Item> getItems() { return items; }

    public List<String> getItemsIdentifiers() {

        List<String> itemsIdentifiers = new ArrayList<>();

        for (Item item : items)
            itemsIdentifiers.add(item.getIdentifier());

        return itemsIdentifiers;
    }

    public void setItems(List<Item> items) { this.items = items; }

}
