package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.config.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Price {

    private Item item;

    private float value;

    private float previousValue;

    private float initialValue;

    private float stock;

    private float support;
    private float resistance;
    private float noiseIntensity;

    private float elasticity;

    private float historicalHigh;
    private float historicalLow;

    private float hourHigh;
    private float hourLow;
    private final List<Float> dayHigh = new ArrayList<>();
    private final List<Float> dayLow = new ArrayList<>();

    private List<Float> hourValues;

    private final float taxBuy;
    private final float taxSell;

    public Price(Item item, float initialValue, float elasticity, float support, float resistance, float noiseIntensity) {

        this.item = item;

        updateValue();
        previousValue = value;

        this.initialValue = initialValue;

        hourHigh = value;
        hourLow = value;
        dayHigh.add(hourHigh);
        dayLow.add(hourLow);
        this.support = support;
        this.resistance = resistance;
        this.noiseIntensity = noiseIntensity * Config.getInstance().getNoiseMultiplier();
        this.elasticity = elasticity * Config.getInstance().getElasticityMultiplier();

        taxBuy = Config.getInstance().getTaxBuy(getItem().getIdentifier());
        taxSell = Config.getInstance().getTaxSell(getItem().getIdentifier());
    }

    public float getValue() { return value; }

    public float getBuyPrice() { return value * taxBuy; }

    public float getSellPrice() { return value * taxSell; }

    public void setStock(float stock) {
        this.stock = stock;
        updateValue();
    }

    public float getStock() { return stock; }

    public float getElasticity() { return elasticity; }

    public void changeStock(float change) {

        if (Config.getInstance().takeIntoAccountTax()) {

            if (change > 0) {
                stock += change * (1 + (1-taxSell));
            } else {
                stock += change * taxBuy;
            }

        } else {
            stock += change;
        }

        updateValue();
    }

    public void enforceLimits() {
        value = Math.min(value, Config.getInstance().getLimits()[1]);
        value = Math.max(value, Config.getInstance().getLimits()[0]);
    }

    public void applyNoise() {

        float prevStock = stock;

        if (support != 0 && value < support && Math.random() > 0.8) {

            stock -= (float) ((8 - 12 * Math.random()) * noiseIntensity);

        } else if (resistance != 0 && value > resistance && Math.random() > 0.8) {

            stock += (float) ((8 - 12 * Math.random()) * noiseIntensity);

        } else {

            stock += (float) ((10 - 20 * Math.random()) * noiseIntensity);

        }

        updateValue();

        item.addVolume(Math.abs(Math.round(stock - prevStock)));

    }

    public float getChange() {
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

        value = (float) (initialValue * Math.exp(-0.0005 * elasticity * stock));
        enforceLimits();
        updateLimits();

    }

    private void updateLimits() {
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

    public void initializeHourValues(float value) {
        if (hourValues == null)
            hourValues = new ArrayList<>(Collections.nCopies(60, value));
    }

    public void addValueToShortTermStorage() {
        hourValues.remove(0);
        hourValues.add(value);
    }

    public float getValueAnHourAgo() { return hourValues.get(0); }

    public List<Float> getValuesPastHour() { return hourValues; }

    public float getProjectedCost(float stockChange, float tax) {

        float change;

        if (Config.getInstance().takeIntoAccountTax()) {

            if (stockChange > 0) {
                change = stockChange * (1 + (1-taxSell));
            } else {
                change = stockChange * taxBuy;
            }

        } else {
            change = stockChange;
        }

        int maxSize = (int) Math.round((item.getItemStack().getType().getMaxStackSize())/(elasticity*4) + 0.5);
        int orderSize = (int) Math.abs(change / maxSize);
        float excess = Math.abs(change % maxSize);

        float fictitiousValue = value;
        float fictitiousStock = stock;
        float cost = 0;

        for (int i = 0 ; i < orderSize ; i++) {
            cost += fictitiousValue * maxSize;
            fictitiousStock += maxSize * Math.signum(change);
            fictitiousValue = (float) (initialValue * Math.exp(-0.0005 * elasticity * fictitiousStock));
        }

        if (excess > 0) {
            cost += fictitiousValue * excess;
        }

        return cost*tax;
    }

    public float getBuyTaxMultiplier() { return taxBuy; }
    public float getSellTaxMultiplier() { return taxSell; }

    public Item getItem() { return item; }

    public float getInitialValue() { return initialValue; }
    public float getNoiseIntensity() { return noiseIntensity; }
    public float getSupport() { return support; }
    public float getResistance() { return resistance; }

    public Price setInitialValue(float initialValue) { this.initialValue = initialValue; return this; }
    public Price setElasticity(float elasticity) {
        this.elasticity = elasticity * Config.getInstance().getElasticityMultiplier(); return this; }
    public Price setNoiseIntensity(float noiseIntensity) { this.noiseIntensity = noiseIntensity * Config.getInstance().getNoiseMultiplier(); return this; }
    public Price setSupport(float support) { this.support = support; return this; }
    public Price setResistance(float resistance) { this.resistance = resistance; return this; }

}
