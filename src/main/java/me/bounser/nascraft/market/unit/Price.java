package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.NUtils;

public class Price {

    // Current neutral price
    private float value;

    private int stock;

    // Advanced metrics
    private final float elasticity;
    private final float support;
    private final float resistance;
    private final float intensity;

    public Price(float price, int stock, float elasticity, float support, float resistance, float intensity) {
        value = price;

        this.stock = stock;

        this.elasticity = elasticity;
        this.support = support;
        this.resistance = resistance;
        this.intensity = intensity;
    }

    public float getValue() { return NUtils.round(value, 2); }

    public float getBuyPrice() { return NUtils.round(value * Config.getInstance().getTaxBuy(), 2); }
    public float getSellPrice() { return NUtils.round(value * Config.getInstance().getTaxSell(), 2); }

    public void setStock(int stock) { this.stock = stock; }

    public int getStock() { return stock; }

    public void changeStock(int change) {

        value += NUtils.round((float) (value*-change*0.0003*(1 + 0.5/(1+Math.exp(-stock*0.0001)))*elasticity), 5);

        verifyChange();
        stock += change;
    }

    public void verifyChange() {
        value = Math.min(value, Config.getInstance().getLimits()[1]);
        value = Math.max(value, Config.getInstance().getLimits()[0]);
    }

    public void applyNoise() {

        if (support != 0 && value < support && Math.random() > 0.8) {
            value = NUtils.round((float) (value * (0.99 + 0.03 * Math.random() * intensity)), 5);
        } else if (resistance != 0 && value > resistance && Math.random() > 0.8) {
            value = NUtils.round((float) (value * (1.01 - 0.03 * Math.random() * intensity)), 5);
        } else {
            value = NUtils.round((float) (value * ((1 - 0.01 * intensity + 0.02 * Math.random() * intensity))), 5);
        }

        verifyChange();
    }

}
