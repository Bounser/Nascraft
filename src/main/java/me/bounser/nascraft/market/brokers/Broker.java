package me.bounser.nascraft.market.brokers;


import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.database.SQLite;
import me.bounser.nascraft.market.MarketManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Broker {

    private float value;

    private List<Float> prices;

    private float fee;

    private float marketSensibility;
    private float volatility;
    private float positiveReturn;

    private BrokerType brokerType;

    public Broker(BrokerType brokerType) {
        this.brokerType = brokerType;
        this.value = SQLite.getInstance().getBrokerSharePrice(brokerType);

        prices = new ArrayList<>(Collections.nCopies(60, value));

        Config config = Config.getInstance();

        fee = config.getBrokerFee(brokerType);
        marketSensibility = config.getMarketSensibility(brokerType);
        volatility = config.getVolatility(brokerType);
        positiveReturn = config.getPositiveReturn(brokerType);
    }

    public void operate() {

        value += value * (MarketManager.getInstance().getLastChange()/100) * marketSensibility + value * (-volatility + (volatility*2*positiveReturn)*Math.random());
        value -= value * fee/(24*60);

        prices.remove(0);
        prices.add(value);

        SQLite.getInstance().updateSharePrice(brokerType, value);
    }

    public List<Float> getPrices() { return prices; }

    public void setValue(float value) { this.value = value; }

    public float getValue() { return value; }

    public String getAlias() { return brokerType.toString(); }

}
