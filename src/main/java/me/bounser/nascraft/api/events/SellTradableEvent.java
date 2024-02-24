package me.bounser.nascraft.api.events;

import me.bounser.nascraft.market.unit.Tradable;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SellTradableEvent extends Event implements Cancellable {

    private final HandlerList HANDLERS_LIST = new HandlerList();

    private boolean cancelled;

    private Player player;
    private Tradable tradable;
    private int amount;


    public SellTradableEvent(Player player, Tradable tradable, int amount) {
        cancelled = false;

        this.player = player;
        this.tradable = tradable;
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

    public Tradable getTradable() {
        return tradable;
    }

    public int getAmount() {
        return amount;
    }
}
