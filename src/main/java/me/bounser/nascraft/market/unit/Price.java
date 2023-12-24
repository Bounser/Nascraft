package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.config.Config;

import java.util.ArrayList;
import java.util.List;

public class Price {

    // Current neutral price
    private float value;

    private float previousValue;

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

    public Price(float initialValue, float elasticity, float support, float resistance, float noiseIntensity) {

        value = (float) (initialValue * Math.exp(-0.0005 * elasticity * stock));
        previousValue = value;

        this.initialValue = initialValue;

        hourHigh = value;
        hourLow = value;
        dayHigh.add(hourHigh);
        dayLow.add(hourLow);
        this.support = support;
        this.resistance = resistance;
        this.noiseIntensity = noiseIntensity;
        this.elasticity = elasticity;
    }

    public float getValue() { return value; }

    public float getBuyPrice() {

        if (value * Config.getInstance().getTaxBuy() < 0.005) {
            return (float) (value + 0.002);
        } else {
            return value * Config.getInstance().getTaxBuy();
        }
    }

    public float getSellPrice() {

        if (value - value*Config.getInstance().getTaxSell() < 0.005) {
            if (value - 0.002 <= 0) return value;
            return (float) (value - 0.002);
        } else {
            return value * Config.getInstance().getTaxSell();
        }
    }

    public void setStock(int stock) { this.stock = stock; }

    public float getStock() { return stock; }

    public void changeStock(int change) {
        stock += change * elasticity;

        updateValue();
    }

    public void verifyChange() {
        value = Math.min(value, Config.getInstance().getLimits()[1]);
        value = Math.max(value, Config.getInstance().getLimits()[0]);
    }

    public float applyNoise() {


        if (support != 0 && value < support && Math.random() > 0.8) {

            stock -= (float) ((8 - 12 * Math.random()) * noiseIntensity);

        } else if (resistance != 0 && value > resistance && Math.random() > 0.8) {

            stock += (float) ((8 - 12 * Math.random()) * noiseIntensity);

        } else {

            stock += (float) ((10 - 20 * Math.random()) * noiseIntensity);

        }
        updateValue();

        float change = -100 + 100*value/previousValue;
        previousValue = value;

        return change;
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

    public void updateValue() {

        value = (float) (initialValue * Math.exp(-0.0004 * stock));
        verifyChange();
        updateLimits();

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
