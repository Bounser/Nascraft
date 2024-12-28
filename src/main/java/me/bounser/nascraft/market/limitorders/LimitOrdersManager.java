package me.bounser.nascraft.market.limitorders;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class LimitOrdersManager {

    private HashMap<LimitOrder, Boolean> limitOrders = new HashMap<>();

    private List<Duration> durations;

    private static LimitOrdersManager instance = null;

    public static LimitOrdersManager getInstance() { return instance == null ? new LimitOrdersManager() : instance; }

    private LimitOrdersManager() {

        if(!Config.getInstance().getLimitOrdersEnabled()) return;

        instance = this;
        DatabaseManager.get().getDatabase().retrieveLimitOrders();

        durations = Config.getInstance().getDurations();

        Bukkit.getScheduler().runTaskTimer(Nascraft.getInstance(),
                this::checkOrders, 200, 20L * Config.getInstance().getCheckingPeriod());
    }

    public List<Duration> getDurationOptions() {
        return durations;
    }

    public void registerLimitOrder(LimitOrder limitOrder) {
        limitOrders.put(limitOrder, !limitOrder.isExpired());
    }

    public void registerNewLimitOrder(UUID uuid, LocalDateTime expiration, Item item, int type, double price, int amount) {

        limitOrders.put(new LimitOrder(uuid, item, expiration, amount, 0, price, 0, type == 1 ? OrderType.LIMIT_BUY : OrderType.LIMIT_SELL), true);

        DatabaseManager.get().getDatabase().addLimitOrder(uuid, expiration, item, type, price, amount);
    }

    public void deleteLimitOrder(LimitOrder limitOrder) {

        limitOrders.remove(limitOrder);

        DatabaseManager.get().getDatabase().removeLimitOrder(limitOrder.getOwnerUuid().toString(), limitOrder.getItem().getIdentifier());

    }

    public void checkOrders() {

        for (LimitOrder limitOrder : limitOrders.keySet()) {

            if (!limitOrders.get(limitOrder)) continue;

            if (limitOrder.isExpired() || limitOrder.isCompleted()) {
                limitOrders.put(limitOrder, false);
            } else {
                limitOrder.checkOrder();
            }
        }
    }

    public List<LimitOrder> getPlayerLimitOrders(UUID uuid) {

        List<LimitOrder> limitOrders = new ArrayList<>();

        for (LimitOrder limitOrder : this.limitOrders.keySet())
            if (limitOrder.getOwnerUuid().equals(uuid)) limitOrders.add(limitOrder);

        return limitOrders;
    }

}
