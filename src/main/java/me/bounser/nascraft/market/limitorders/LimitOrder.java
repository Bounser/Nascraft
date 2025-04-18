package me.bounser.nascraft.market.limitorders;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.UUID;

public class LimitOrder {

    private UUID uuid;
    private Item item;
    private LocalDateTime expiration;
    private int toComplete;
    private int completed;
    private double priceLimit;
    private double cost;
    private OrderType orderType;

    private boolean expired = false;

    public LimitOrder(UUID uuid, Item item, LocalDateTime expiration, int toComplete, int completed, double priceLimit, double cost, OrderType orderType) {
        this.uuid = uuid;
        this.item = item;
        this.expiration = expiration;
        this.toComplete = toComplete;
        this.completed = completed;
        this.priceLimit = priceLimit;
        this.cost = cost;
        this.orderType = orderType;
    }

    public UUID getOwnerUuid() { return uuid; }

    public Item getItem() { return item; }

    public int getCompleted() { return completed; }
    public int getToComplete() { return toComplete; }

    public boolean isCompleted() { return completed == toComplete; }

    public double getPrice() { return priceLimit; }

    public double getCost() { return  cost; }

    public OrderType getOrderType() { return orderType; }

    public boolean isExpired() { return expired; }

    public LocalDateTime getExpiration() { return expiration; }

    public void checkOrder() {

        if (completed == toComplete) { return; }

        if (expired) return;

        if (expiration.isBefore(LocalDateTime.now())) {
            expired = true;
            return;
        }

        switch (orderType) {

            case LIMIT_BUY:

                if (item.getPrice().getBuyPrice() >= priceLimit) return;

                int toBuy = (int) item.getPrice().stockChangeUntilPriceReached(priceLimit/item.getPrice().getBuyTaxMultiplier());

                if (toBuy < 0) return;

                int buyOrder = Math.min(Math.abs(toBuy), toComplete-completed);

                double buyImpact = item.buy(buyOrder, uuid, false);

                if (buyImpact <= 0) return;

                cost += Math.abs(buyImpact);
                completed += buyOrder;

                if (toComplete == completed) {

                    Player player = Bukkit.getPlayer(uuid);

                    if (player != null) {
                        Lang.get().message(player, Message.LIMIT_BUY_COMPLETED, Formatter.format(item.getCurrency(), cost/completed, Style.ROUND_BASIC), String.valueOf(completed), item.getTaggedName());
                    }
                }

                break;

            case LIMIT_SELL:

                if (item.getPrice().getSellPrice() <= priceLimit) return;

                int toSell = (int) item.getPrice().stockChangeUntilPriceReached(priceLimit/item.getPrice().getSellTaxMultiplier());

                if (toSell > 0) return;

                int sellOrder = Math.min(Math.abs(toSell), toComplete-completed);

                double sellImpact = item.sell(sellOrder, uuid, false);

                if (sellImpact < 0) return;

                cost += Math.abs(sellImpact);
                completed += sellOrder;

                if (toComplete == completed) {

                    Player player = Bukkit.getPlayer(uuid);

                    if (player != null) {
                        Lang.get().message(player, Message.LIMIT_SELL_COMPLETED, Formatter.format(item.getCurrency(), cost/completed, Style.ROUND_BASIC), String.valueOf(completed), item.getTaggedName());
                    }
                }

                break;
        }

        DatabaseManager.get().getDatabase().updateLimitOrder(uuid, item, completed, cost);

    }

}
