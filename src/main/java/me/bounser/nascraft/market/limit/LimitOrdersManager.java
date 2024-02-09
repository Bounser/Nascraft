package me.bounser.nascraft.market.limit;

import me.bounser.nascraft.database.SQLite;
import me.bounser.nascraft.market.unit.Item;

import java.util.*;

public class LimitOrdersManager {

    private final HashMap<UUID, List<LimitOrder>> limitOrders;

    private static LimitOrdersManager instance;

    public static LimitOrdersManager getInstance() {
        return instance == null ? instance = new LimitOrdersManager() : instance;
    }


    private LimitOrdersManager() {
        this.limitOrders = SQLite.getInstance().getLimitOrders();
    }


    public void createLimitOrder(String expirationDate, float cost, UUID uuid, float objective, Item item, OrderType type, int quantity) {

        SQLite.getInstance().saveLimitOrder(expirationDate, cost, uuid, item.getIdentifier(), objective, quantity);

        if (limitOrders.get(uuid) == null) {
            limitOrders.put(uuid, new ArrayList<>(Arrays.asList(new LimitOrder(objective, item, type, quantity))));
        } else {
            List<LimitOrder> previousOrders = limitOrders.get(uuid);
            previousOrders.add(new LimitOrder(objective, item, type, quantity));
            limitOrders.put(uuid, previousOrders);
        }
    }

    public void checkOrders() {

        for (UUID uuid : limitOrders.keySet()) {

            for (LimitOrder limitOrder : limitOrders.get(uuid)) {

                if (limitOrder.isExecutable()) {
                    executeOrder(limitOrder, uuid);
                }
            }
        }
    }

    public void executeOrder(LimitOrder limitOrder, UUID uuid) {

        switch (limitOrder.getType()) {

            case LIMIT_BUY:

                limitOrder.getItem().sellItem(
                        limitOrder.getQuantity(),
                        uuid,
                        true,
                        limitOrder.getItem().getItemStack().getType()
                );


            case LIMIT_SELL:

                limitOrder.getItem().sellItem(
                        limitOrder.getQuantity(),
                        uuid,
                        true,
                        limitOrder.getItem().getItemStack().getType()
                        );


        }

    }

}
