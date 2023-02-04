package me.bounser.nascraft.market;

import me.bounser.nascraft.tools.Config;
import org.bukkit.Bukkit;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public String getDisplayName() {
        return Config.getInstance().getDisplayName(this);
    }

    public float getCategoryChange() {
        List<Float> pricesChange = new ArrayList<>();

        for(Item item : items) {
            pricesChange.add(-100 + 100*((float) item.getPricesM().get(23)/item.getPricesM().get(0)));
        }
        float sum = 0;
        for(float num : pricesChange) {
            sum += num;
        }
        BigDecimal bd = new BigDecimal(sum);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.floatValue();
    }
}
