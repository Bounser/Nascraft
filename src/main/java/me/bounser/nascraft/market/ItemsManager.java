package me.bounser.nascraft.market;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.tools.Config;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class ItemsManager {

    List<Item> items = new ArrayList<>();

    private static ItemsManager instance;
    private static Nascraft main;

    public static ItemsManager getInstance() { return instance == null ? instance = new ItemsManager() : instance; }

    public void setupItem() {

        for(int i = 1; i <= 3; i++ )
        for(String mat : Config.getInstance().getAllMaterials(i)) {

            items.add(new Item(mat, Config.getInstance().getInitialPrice(i), 1));

        }

    }

    public Item getItem(String material) {

        for(Item item : items) {

            if(item.getMaterial() == Material.getMaterial(material)){ return item; }

        }
        return null;
    }

    public List<Item> getAllItems() { return items; }

}
