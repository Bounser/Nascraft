package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.Tradable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class InteractionsManager {

    public HashMap<Player, Category> playerCategory = new HashMap<>();

    // Offset of items of the selected category.
    public HashMap<Player, Integer> playerOffset = new HashMap<>();

    private final HashMap<Player, Tradable> tradables = new HashMap<>();

    public HashMap<Player, Item> items = new HashMap<>();

    public HashMap<Player, List<ItemStack>> childs = new HashMap<>();

    private static InteractionsManager instance;

    public static InteractionsManager getInstance() {
        return instance == null ? instance = new InteractionsManager() : instance;
    }

    public Item getItemFromPlayer(Player player) { return items.get(player); }

    public Tradable getTradableFromPlayer(Player player) { return tradables.get(player); }

    public void setTradableOfPlayer(Player player, Tradable tradable) {
        tradables.put(player, tradable);
    }

    public void setItemOfPlayer(Player player, Item item) {

        childs.put(player, new ArrayList<>(item.getChilds().keySet()));

        items.put(player, item);
    }

    public void rotateChilds(Player player) {

        List<ItemStack> playerChilds = childs.get(player);

        Collections.rotate(playerChilds, -1);

        childs.put(player, playerChilds);
    }

    public ItemStack getChildFromPlayer(Player player) {
        return childs.get(player).get(0);
    }

    public ItemStack getChildFromPositionAndPlayer(int position, Player player) {
        return childs.get(player).get(position);
    }

    public float getMultiplier(Player player) {

        if (childs.get(player) == null) return 1;

        return items.get(player).getChilds().get(childs.get(player).get(0));
    }

    public List<ItemStack> getChildsFromPlayer(Player player) {
        return childs.get(player);
    }

}
