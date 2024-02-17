package me.bounser.nascraft.events;

import me.bounser.nascraft.market.unit.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BuyItemEvent extends Event implements Cancellable {

    private final HandlerList HANDLERS_LIST = new HandlerList();

    private boolean cancelled;

    private Player player;
    private Item item;
    private int amount;


    public BuyItemEvent(Player player, Item item, int amount) {
        cancelled = false;

        this.player = player;
        this.item = item;
        this.amount = amount;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Player getPlayer() {
        return player;
    }

    public Item getItem() {
        return item;
    }

    public int getAmount() {
        return amount;
    }
}