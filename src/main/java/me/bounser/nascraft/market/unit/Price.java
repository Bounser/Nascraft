package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.RoundUtils;

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

    public float getValue() { return RoundUtils.round(value); }

    public float getBuyPrice() { return RoundUtils.round(value * Config.getInstance().getTaxBuy()); }
    public float getSellPrice() { return RoundUtils.round(value * Config.getInstance().getTaxSell()); }

    public void setStock(int stock) { this.stock = stock; }

    public int getStock() { return stock; }

    public void changeStock(int change) {

        value += RoundUtils.round((float) (value*-change*0.0003*(1 + 0.5/(1+Math.exp(-stock*0.0001)))*elasticity));

        verifyChange();
        stock += change;
    }

    public void verifyChange() {
        value = Math.min(value, Config.getInstance().getLimits()[1]);
        value = Math.max(value, Config.getInstance().getLimits()[0]);
    }

    public void applyNoise() {

        if (support != 0 && value < support && Math.random() > 0.8) {
            value = RoundUtils.preciseRound((float) (value * (0.99 + 0.03 * Math.random() * intensity)));
        } else if (resistance != 0 && value > resistance && Math.random() > 0.8) {
            value = RoundUtils.preciseRound((float) (value * (1.01 - 0.03 * Math.random() * intensity)));
        } else {
            value = RoundUtils.preciseRound((float) (value * ((1 - 0.01 * intensity + 0.02 * Math.random() * intensity))));
        }

        verifyChange();
    }

}
