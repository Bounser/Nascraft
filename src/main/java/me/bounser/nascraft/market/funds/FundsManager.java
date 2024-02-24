package me.bounser.nascraft.market.funds;

import me.bounser.nascraft.config.Config;

import java.util.HashMap;

public class FundsManager {

    private HashMap<String, Fund> funds;

    private static FundsManager instance = null;

    public static FundsManager getInstance() { return instance == null ? new FundsManager() : instance; }

    private FundsManager() {
        instance = this;

        Config.getInstance().setupFunds();
    }

    public void createFund(String identifier, HashMap<Strategy, Float> weightedStrategy) {

        funds.put(identifier, new Fund(identifier, weightedStrategy));

    }

    public Fund getFund(String identifier) {
        return funds.get(identifier);
    }

}
