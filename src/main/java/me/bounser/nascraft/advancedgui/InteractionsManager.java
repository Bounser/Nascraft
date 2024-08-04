package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class InteractionsManager {

    public HashMap<Player, Category> playerCategory = new HashMap<>();

    // Offset of items of the selected category.
    public HashMap<Player, Integer> playerOffset = new HashMap<>();

    public List<Player> playersOnCooldown = new ArrayList<>();

    public HashMap<Player, List<Item>> options = new HashMap<>();

    private static InteractionsManager instance;

    public static InteractionsManager getInstance() {
        return instance == null ? instance = new InteractionsManager() : instance;
    }

    public Item getItemFromPlayer(Player player) {

        if (!options.containsKey(player)) return MarketManager.getInstance().getAllItems().get(0);

        return options.get(player).get(0);
    }

    public void setItemOfPlayer(Player player, Item item) {

        List<Item> options = new ArrayList<>();

        options.add(item);

        options.addAll(item.getChilds());

        this.options.put(player, options);
    }

    public void rotateOptions(Player player) {

        List<Item> playerOptions = options.get(player);

        Collections.rotate(playerOptions, -1);

        options.put(player, playerOptions);
    }

    public List<Item> getOptions(Player player) {
        return options.get(player);
    }

    public Item getParent(Player player) {

        List<Item> items = options.get(player);

        if (items == null) return null;

        if (items.get(0).isParent()) return items.get(0);
        else return items.get(0).getParent();

    }

    public boolean isOnCooldown(Player player) {
        return playersOnCooldown.contains(player);
    }

    public void addCooldownToPlayer(Player player) {
        if (!playersOnCooldown.contains(player)) {
            playersOnCooldown.add(player);

            Bukkit.getScheduler().runTaskLaterAsynchronously(Nascraft.getInstance(), () -> {

                if (playersOnCooldown.contains(player))
                    playersOnCooldown.remove(player);

            }, (long) (Config.getInstance().getLayoutCooldown() * 20.0));
        }
    }

}
