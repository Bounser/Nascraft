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

    public float getValue() { return NUtils.round(value); }

    public float getBuyPrice() { return NUtils.round(value + value*Config.getInstance().getTaxBuy()); }
    public float getSellPrice() { return NUtils.round(value - value*Config.getInstance().getTaxSell()); }

    public void setStock(int stock) { this.stock = stock; }

    public int getStock() { return stock; }

    public void changeStock(int change) {

        if(value < 50 + 20 * Math.random() && elasticity*0.2 > Math.random()) {
            value += 0.01 * change * Integer.signum(change);
        } else {
            value = NUtils.round((float) (value + value*change*0.0003*(1 + 0.5/(1+Math.exp(-stock*0.0001)))*elasticity));
            verifyChange();
        }

        stock += change;
    }

    public void verifyChange() {
        value = Math.min(value, Config.getInstance().getLimits()[1]);
        value = Math.max(value, Config.getInstance().getLimits()[0]);
    }

    public void applyNoise() {

        if (value > 30 + 50 * Math.random()) {

            if (support != 0 || resistance != 0) {
                if (support != 0 ) {
                    if (value < support && Math.random() > 0.5) {
                        value = (float) (value * (0.99 + 0.03 * Math.random() * intensity));
                    }
                }
                if (resistance != 0) {
                    if (value > resistance && Math.random() > 0.5) {
                        value = (float) (value * (1.01 - 0.03 * Math.random() * intensity));
                    }
                }
            } else if (Math.random() > 0.4) {
                value = (float) (value * (0.99 + 0.02 * Math.random() * intensity));
            }
            value = NUtils.round(value);

        } else {

            if (support != 0 || resistance != 0) {
                if (support != 0) {
                    if (value < support && Math.random() * intensity > 0.3) {
                        value += 0.01;
                    }
                }
                if (resistance != 0){
                    if (value > resistance && Math.random()*intensity > 0.3) {
                        value -= 0.01;
                    }
                }
            } else {
                if (0.5 > Math.random() && Math.random()*intensity > 0.3) {
                    value += 0.01;
                } else {
                    value -= 0.01;
                }
            }
        }
        verifyChange();
    }

}
