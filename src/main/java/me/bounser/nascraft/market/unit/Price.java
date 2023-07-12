package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.RoundUtils;
import org.bukkit.Bukkit;

public class Price {

    // Current neutral price
    private float value;

    // The counter will keep track of the short-term changes on stock.
    private int counter = 0;

    // Tolerance threshold. Once the stock changes this amount the price will be affected.
    private final int threshold;

    private int stock;

    // Advanced metrics
    private final float support;
    private final float resistance;
    private final float noiseIntensity;

    public Price(float price, int stock, float elasticity, float support, float resistance, float noiseIntensity) {
        value = price;

        this.stock = stock;
        this.support = support;
        this.resistance = resistance;
        this.noiseIntensity = noiseIntensity;

        threshold = Math.max(Math.round(64*(1/elasticity)), 1);
    }

    public float getValue() { return RoundUtils.round(value); }

    public float getBuyPrice() { return RoundUtils.round(value * Config.getInstance().getTaxBuy()); }
    public float getSellPrice() { return RoundUtils.round(value * Config.getInstance().getTaxSell()); }

    public void setStock(int stock) { this.stock = stock; }

    public int getStock() { return stock; }

    public void changeStock(int change) {

        counter += change;

        while(counter <= -threshold) {

            if(stock <= 0)
                value += Math.max(RoundUtils.round((float) (value * 0.01 * Math.log10(-stock+10))), 0.01);
            else
                value += Math.max(RoundUtils.round((float) (value * 0.01)), 0.01);

            verifyChange();

            counter += threshold;
        }

        while(counter >= threshold) {

            if(stock > 0)
                value -= Math.max(RoundUtils.round((float) (value * 0.01 * Math.log10(stock+10))), 0.01);
            else
                value -= Math.max(RoundUtils.round((float) (value * 0.01)), 0.01);

            verifyChange();

            counter -= threshold;
        }

        stock += change;

    }

    public void verifyChange() {
        value = Math.min(value, Config.getInstance().getLimits()[1]);
        value = Math.max(value, Config.getInstance().getLimits()[0]);
    }

    public void applyNoise() {

        if (support != 0 && value < support && Math.random() > 0.8) {

            value = RoundUtils.preciseRound((float) (value * (0.99 + 0.03 * Math.random() * noiseIntensity)));

        } else if (resistance != 0 && value > resistance && Math.random() > 0.8) {

            value = RoundUtils.preciseRound((float) (value * (1.01 - 0.03 * Math.random() * noiseIntensity)));

        } else {

            value = RoundUtils.preciseRound((float) (value * ((1 - 0.01 * noiseIntensity + 0.02 * Math.random() * noiseIntensity))));

        }
        verifyChange();
    }
}
