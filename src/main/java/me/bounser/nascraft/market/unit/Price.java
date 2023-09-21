package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.formatter.RoundUtils;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

public class Price {

    // Current neutral price
    private float value;

    private float initialValue;

    private float stock;

    // Advanced metrics
    private final float support;
    private final float resistance;
    private final float noiseIntensity;

    private final float elasticity;

    private float historicalHigh;
    private float historicalLow;

    private float hourHigh;
    private float hourLow;
    private final List<Float> dayHigh = new ArrayList<>();
    private final List<Float> dayLow = new ArrayList<>();

    public Price(float initialValue, int stock, float historicalHigh, float elasticity, float support, float resistance, float noiseIntensity) {

        value = (float) (initialValue * Math.exp(-0.0005 * elasticity * stock));

        this.initialValue = initialValue;

        this.stock = stock;
        this.historicalHigh = historicalHigh;
        hourHigh = value;
        hourLow = value;
        dayHigh.add(hourHigh);
        dayLow.add(hourLow);
        this.support = support;
        this.resistance = resistance;
        this.noiseIntensity = noiseIntensity;
        this.elasticity = elasticity;
    }

    public float getValue() { return RoundUtils.round(value); }

    public float getBuyPrice() {

        if (value * Config.getInstance().getTaxBuy() < 0.01) {
            return (float) (value + 0.01);
        } else {
            return value * Config.getInstance().getTaxBuy();
        }
    }

    public float getSellPrice() {

        if (value - value*Config.getInstance().getTaxSell() < 0.01) {
            return (float) (value - 0.01);
        } else {
            return value * Config.getInstance().getTaxSell();
        }
    }

    public void setStock(int stock) { this.stock = stock; }

    public float getStock() { return stock; }

    public void changeStock(int change) {
        stock += change;

        value = (float) (initialValue * Math.exp(-0.0005 * elasticity * stock));

        updateLimits();
    }

    public void verifyChange() {
        value = Math.min(value, Config.getInstance().getLimits()[1]);
        value = Math.max(value, Config.getInstance().getLimits()[0]);
    }

    public void applyNoise() {

        if (support != 0 && value < support && Math.random() > 0.8) {

            stock -= (8 - 12 * Math.random()) * noiseIntensity;

        } else if (resistance != 0 && value > resistance && Math.random() > 0.8) {

            stock += (8 - 12 * Math.random()) * noiseIntensity;

        } else {

            stock += (10 - 20 * Math.random()) * noiseIntensity;

        }

        value = (float) (initialValue * Math.exp(-0.0005 * elasticity * stock));
        verifyChange();

        updateLimits();
    }

    public float getHistoricalHigh() { return historicalHigh; }

    public float getHistoricalLow() { return historicalLow; }

    public void setHistoricalHigh(float newHistoricalHigh) { historicalHigh = newHistoricalHigh; }

    public void setHistoricalLow(float newHistoricalLow) { historicalLow = newHistoricalLow; }

    public float getDayHigh() {

        float high = dayHigh.get(0);

        for (float value : dayHigh) if (value > high) high = value;

        return Math.max(hourHigh, high);

    }

    public float getDayLow() {

        float low = dayLow.get(0);

        for (float value : dayLow) if (low > value) low = value;

        return Math.min(hourLow, low);
    }

    public void updateLimits() {
        if (value > historicalHigh) { historicalHigh = value; }
        if (value < historicalLow) { historicalLow = value; }

        if (value > hourHigh) { hourHigh = value; }
        if (value < hourLow) { hourLow = value; }
    }

    public void restartHourLimits() {

        if (dayHigh.size() == 24) {
            dayHigh.remove(0);
            dayLow.remove(0);

            dayHigh.add(hourHigh);
            dayLow.add(hourLow);
        }
        hourLow = value;
        hourHigh = value;
    }

}
