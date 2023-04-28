package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.NUtils;

public class Price {

    // Current neutral price
    private float cPrice;

    private int stock;

    // Advanced metrics
    private final int elasticity;
    private final float support;
    private final float resistance;

    public Price(float price, int stock, int elasticity, float support, float resistance) {
        cPrice = price;

        this.stock = stock;

        this.elasticity = elasticity;
        this.support = support;
        this.resistance = resistance;
    }

    public float getValue() { return NUtils.round(cPrice); }

    public float getBuyPrice() { return NUtils.round(cPrice + cPrice*Config.getInstance().getTaxBuy()); }
    public float getSellPrice() { return NUtils.round(cPrice - cPrice*Config.getInstance().getTaxSell()); }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getStock() { return stock; }

    public void changeStock(int change) {

        float val = (float) (cPrice + Integer.signum(change)*cPrice*0.001*(1 + 0.5/(1+Math.exp(-stock*0.0001)))*elasticity + Math.abs(change)*0.1);
        cPrice = Math.min(val, Config.getInstance().getLimits()[1]);

        verifyChange();

        stock += change;
    }

    public void verifyChange() {
        if (cPrice < Config.getInstance().getLimits()[0]) cPrice = Config.getInstance().getLimits()[0];
        if (cPrice > Config.getInstance().getLimits()[1]) cPrice = Config.getInstance().getLimits()[1];
    }

    public void applyNoise() {


    }


}
