package me.bounser.nascraft.api;

import me.bounser.nascraft.market.MarketManager;

public class NascraftAPI {

    public MarketManager getMarketManager() { return MarketManager.getInstance(); }

}
