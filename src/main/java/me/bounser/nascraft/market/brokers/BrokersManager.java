package me.bounser.nascraft.market.brokers;


import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.SQLite;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BrokersManager {

    private Broker aggresiveBroker = null;
    private Broker conservativeBroker = null;
    private Broker lazyBroker = null;

    private HashMap<BrokerType, HashMap<UUID, Float>> shares;
    private HashMap<BrokerType, HashMap<UUID, Float>> costs;

    private static BrokersManager instance = null;

    public static BrokersManager getInstance() { return instance == null ? new BrokersManager() : instance; }

    private BrokersManager() {
        instance = this;

        shares = new HashMap<>();

        Config config = Config.getInstance();

        List<BrokerType> brokerTypeList = config.getBrokers();

        if (brokerTypeList.contains(BrokerType.AGGRESSIVE)) {
            aggresiveBroker = new Broker(BrokerType.AGGRESSIVE);
            shares.put(BrokerType.AGGRESSIVE, new HashMap<>());
        }
        if (brokerTypeList.contains(BrokerType.CONSERVATIVE)) {
            conservativeBroker = new Broker(BrokerType.CONSERVATIVE);
            shares.put(BrokerType.CONSERVATIVE, new HashMap<>());
        }
        if (brokerTypeList.contains(BrokerType.LAZY)) {
            lazyBroker = new Broker(BrokerType.LAZY);
            shares.put(BrokerType.LAZY, new HashMap<>());
        }

    }

    public void operateBrokers() {
        if (aggresiveBroker != null) aggresiveBroker.operate();
        if (conservativeBroker != null) conservativeBroker.operate();
        if (lazyBroker != null) lazyBroker.operate();
    }

    public Broker getAggresiveBroker() { return aggresiveBroker; }

    public Broker getConservativeBroker() { return conservativeBroker; }

    public Broker getLazyBroker() { return lazyBroker; }

    public Broker getBroker(BrokerType brokerType) {
        switch (brokerType) {
            case AGGRESSIVE: return aggresiveBroker;
            case CONSERVATIVE: return conservativeBroker;
            case LAZY: return lazyBroker;
        }
        return null;
    }

    public void addShares(UUID uuid, BrokerType brokerType, float toAdd) {

        float quantity;
        float cost;

        if (shares.get(brokerType).get(uuid) == null) {
            quantity = (float) SQLite.getInstance().retrieveShares(brokerType, uuid);
            cost = (float) SQLite.getInstance().retrieveCost(brokerType, uuid);
        } else {
            quantity = shares.get(brokerType).get(uuid);
            cost = costs.get(brokerType).get(uuid);
        }

        switch (brokerType) {
            case AGGRESSIVE: cost += toAdd * aggresiveBroker.getValue(); break;
            case CONSERVATIVE: cost += toAdd * conservativeBroker.getValue(); break;
            case LAZY: cost += toAdd * lazyBroker.getValue(); break;
        }

        shares.get(brokerType).put(uuid, quantity+toAdd);
        costs.get(brokerType).put(uuid, cost);

        SQLite.getInstance().updateShares(uuid, quantity + toAdd, cost);
    }

    public float getShares(UUID uuid, BrokerType brokerType) {

        if (shares.get(brokerType).get(uuid) == null)
            return (float) SQLite.getInstance().retrieveShares(brokerType, uuid);

        return shares.get(brokerType).get(uuid);
    }

    public void subtractShares(UUID uuid, BrokerType brokerType, float toSubtract) {
        if (shares.get(brokerType).get(uuid) == null) {
            shares.get(brokerType).put(uuid, (float) (SQLite.getInstance().retrieveShares(brokerType, uuid) - toSubtract));
        } else {
            shares.get(brokerType).put(uuid, shares.get(brokerType).get(uuid) - toSubtract);
        }
    }

}
