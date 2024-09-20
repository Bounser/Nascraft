package me.bounser.nascraft.api.events;

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
    private float amount;

    public BuyItemEvent(Player player, Item item, float amount) {
        cancelled = false;

        this.item = item;
        this.player = player;
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

    public Item getItem() { return item; }

    public float getAmount() {
        return amount;
    }
}
