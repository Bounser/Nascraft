package me.bounser.nascraft.api.events;

import me.bounser.nascraft.market.unit.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TransactionCompletedEvent extends Event {

    private final HandlerList HANDLERS_LIST = new HandlerList();

    private Player player;
    private Item item;
    private float amount;
    private double price;
    private Action action;

    public TransactionCompletedEvent(Player player, Item item, float amount, Action action, double price) {
        this.player = player;
        this.item = item;
        this.amount = amount;
        this.action = action;
        this.price = price;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public Player getPlayer() {
        return player;
    }

    public Item getItem() {
        return item;
    }

    public float getAmount() {
        return amount;
    }

    public Action getAction() {
        return action;
    }

    public double getPrice() { return price; }

}
