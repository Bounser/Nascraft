package me.bounser.nascraft.market.limit;

import me.bounser.nascraft.market.unit.Item;

public class LimitOrder {

    private final float objective;
    private final Item item;
    private final OrderType type;
    private final int quantity;

    public LimitOrder(float objective, Item item, OrderType type, int quantity) {
        this.objective = objective;
        this.item = item;
        this.type = type;
        this.quantity = quantity;
    }

    public boolean isExecutable() {

        if (type.equals(OrderType.LIMIT_BUY)) {
            return objective > item.getPrice().getBuyPrice();
        } else {
            return objective < item.getPrice().getSellPrice();
        }
    }

    public float getCost() {


        return 0;
    }

    public OrderType getType() { return type; }

    public Item getItem() { return item; }

    public int getQuantity() { return quantity; }

}
