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

        value = NUtils.round((float) (value + value*change*0.01*(1 + 0.5/(1+Math.exp(-stock*0.0001)))*elasticity));

        verifyChange();

        stock += change;
    }

    public void verifyChange() {
        value = Math.min(value, Config.getInstance().getLimits()[1]);
        value = Math.max(value, Config.getInstance().getLimits()[0]);
    }

    public void applyNoise() {

        if(support != 0) {
            if(value < support && Math.random() > 0.3) {
                value = (float) (value*(1 + 0.2*Math.random()*intensity));
            }
        } else if(resistance != 0){
            if(value > resistance && Math.random() > 0.3) {
                value = (float) (value*(1 - 0.2*Math.random()*intensity));
            }
        } else {
            value = (float) (value*0.9 + 0.2*Math.random()*intensity);
        }

        value = NUtils.round(value);
        verifyChange();
    }

}
