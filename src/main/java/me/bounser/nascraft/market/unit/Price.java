package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.formatter.RoundUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Price {

    private Item item;

    private double value;

    private int precission;

    private double topLimit;
    private double lowLimit;

    private double upperStockThreshold;
    private double lowerStockThreshold;

    private double previousValue;

    private double initialValue;

    private float stock;

    private double support;
    private double resistance;
    private float noiseIntensity;

    private float elasticity;

    private double historicalHigh;
    private double historicalLow;

    private double hourHigh;
    private double hourLow;
    private final List<Double> dayHigh = new ArrayList<>();
    private final List<Double> dayLow = new ArrayList<>();

    private List<Double> hourValues;

    private final float taxBuy;
    private final float taxSell;

    private double chartDayHigh;
    private double monthHigh;
    private double yearHigh;
    private double allHigh;

    private double chartDayLow;
    private double monthLow;
    private double yearLow;
    private double allLow;

    private float dayChange;
    private float monthChange;
    private float yearChange;
    private float allChange;

    public Price(Item item, float initialValue, float elasticity, float support, float resistance, float noiseIntensity) {

        this.item = item;

        updateValue();
        previousValue = value;

        precission = item.getCurrency().getDecimalPrecission();

        topLimit = Config.getInstance().getHighLimit(item.getIdentifier());
        if (topLimit < 0) topLimit = item.getCurrency().getTopLimit();

        lowLimit = Config.getInstance().getLowLimit(item.getIdentifier());
        if (lowLimit < 0) lowLimit = (float) Math.max(item.getCurrency().getLowLimit(), (float) 1.0/(Math.pow(10f, precission)));

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

        upperStockThreshold = getStockFromValue(topLimit);
        lowerStockThreshold = getStockFromValue(lowLimit);
    }

    public double getValue() { return value; }

    public double getBuyPrice() { return value * taxBuy; }

    public double getSellPrice() { return value * taxSell; }

    public void setStock(float stock) {
        this.stock = stock;
        updateValue();
    }

    public void setDayHigh(double dayHigh) {
        this.chartDayHigh = dayHigh;
    }

    public double getChartDayHigh() {
        return chartDayHigh;
    }

    public void setMonthHigh(double monthHigh) {
        this.monthHigh = monthHigh;
    }

    public double getMonthHigh() {
        return monthHigh;
    }

    public void setYearHigh(double yearHigh) {
        this.yearHigh = yearHigh;
    }

    public double getYearHigh() {
        return yearHigh;
    }

    public void setAllHigh(double allHigh) {
        this.allHigh = allHigh;
    }

    public double getAllHigh() {
        return allHigh;
    }

    public void setDayLow(double dayLow) {
        this.chartDayLow = dayLow;
    }

    public double getChartDayLow() {
        return chartDayLow;
    }

    public void setMonthLow(double monthLow) {
        this.monthLow = monthLow;
    }

    public double getMonthLow() {
        return monthLow;
    }

    public void setYearLow(double yearLow) {
        this.yearLow = yearLow;
    }

    public double getYearLow() {
        return yearLow;
    }

    public void setAllLow(double allLow) {
        this.allLow = allLow;
    }

    public double getAllLow() {
        return allLow;
    }

    public void setDayChange(float dayChange) {
        this.dayChange = dayChange;
    }

    public float getDayChange() { return dayChange; }

    public void setMonthChange(float monthChange) {
        this.monthChange = monthChange;
    }

    public float getMonthChange() { return monthChange; }

    public void setYearChange(float yearChange) {
        this.yearChange = yearChange;
    }

    public float getYearChange() { return yearChange; }

    public void setAllChange(float allChange) {
        this.allChange = allChange;
    }

    public float getAllChange() { return allChange; }

    public float getStock() { return stock; }

    public float getElasticity() { return elasticity; }

    public double getUpperStockLimit() { return upperStockThreshold; }

    public double getLowerStockThreshold() { return lowerStockThreshold; }

    public boolean canStockChange(float change, boolean buy) {

        if (elasticity == 0) return true;

        float newStock = stock + change;

        if (!buy) return !(newStock > lowerStockThreshold);
        else return !(newStock < upperStockThreshold);
    }

    public double stockChangeUntilPriceReached(double priceToReach) {

        double stockToReach = getStockFromValue(priceToReach);

        if (priceToReach > value) {
            return Math.floor(Math.abs(stock - stockToReach));
        } else {
            return -Math.floor(Math.abs(stock - stockToReach));
        }
    }

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
        value = Math.min(value, topLimit);
        value = Math.max(value, lowLimit);
    }

    public void applyNoise() {

        if (elasticity == 0 || noiseIntensity == 0) return;

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

    public double getChange() {
        double change = -100 + 100*value/previousValue;
        previousValue = value;

        return change;
    }

    public double getHistoricalHigh() { return historicalHigh; }

    public double getHistoricalLow() { return historicalLow; }

    public void setHistoricalHigh(float newHistoricalHigh) { historicalHigh = newHistoricalHigh; }

    public void setHistoricalLow(float newHistoricalLow) { historicalLow = newHistoricalLow; }

    public double getDayHigh() {

        double high = dayHigh.get(0);

        for (double value : dayHigh) if (value > high) high = value;

        return Math.max(hourHigh, high);
    }

    public double getDayLow() {

        double low = dayLow.get(0);

        for (double value : dayLow) if (low > value) low = value;

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

    public void initializeHourValues(double value) {
        if (hourValues == null)
            hourValues = new ArrayList<>(Collections.nCopies(60, value));
    }

    public void addValueToShortTermStorage() {
        hourValues.remove(0);
        hourValues.add(value);
    }

    public float getValueChangeLastHour() {
        return RoundUtils.roundToOne((float) (-100 + 100*value/hourValues.get(0)));
    }

    public double getValueAnHourAgo() { return hourValues.get(0); }

    public List<Double> getValuesPastHour() { return hourValues; }

    public double getProjectedCost(float stockChange, float tax) {

        if (elasticity == 0)
            return roundToDecimals(Math.abs((value * stockChange * tax)), precission);

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

        double cost = integratePiecewise(stock, stock + change);

        return roundToDecimals(cost*tax, precission);
    }

    public double integratePiecewise(double start, double end) {

        double initialStock = start;
        double finalStock = end;

        if (start >= end) {
            initialStock = end;
            finalStock = start;
        }

        double totalIntegral = 0.0;

        double y1 = initialValue * Math.exp(-0.0005* elasticity * upperStockThreshold);
        double y2 = initialValue * Math.exp(-0.0005* elasticity * lowerStockThreshold);

        double segment1_end = Math.min(finalStock, upperStockThreshold);
        if (segment1_end > initialStock) {
            double width = segment1_end - initialStock;
            totalIntegral += y1 * width;
        }

        double segment2_start = Math.max(initialStock, upperStockThreshold);
        double segment2_end = Math.min(finalStock, lowerStockThreshold);
        if (segment2_end > segment2_start) {
            totalIntegral += integrateAnalytically(segment2_start, segment2_end);
        }

        double segment3_start = Math.max(initialStock, lowerStockThreshold);
        if (finalStock > segment3_start) {
            double width = finalStock - segment3_start;
            totalIntegral += y2 * width;
        }

        return totalIntegral;
    }

    public double integrateAnalytically(double x1, double x2) {

        final double k = 0.0005 * elasticity;

        double factor = initialValue / k;
        double expTerm1 = Math.exp(-k * x1);
        double expTerm2 = Math.exp(-k * x2);
        return factor * (expTerm1 - expTerm2);
    }

    public float getBuyTaxMultiplier() { return taxBuy; }
    public float getSellTaxMultiplier() { return taxSell; }

    public Item getItem() { return item; }

    public double getInitialValue() { return initialValue; }
    public float getNoiseIntensity() { return noiseIntensity; }
    public double getSupport() { return support; }
    public double getResistance() { return resistance; }

    public Price setInitialValue(double initialValue) { this.initialValue = initialValue; return this; }
    public Price setElasticity(float elasticity) {
        this.elasticity = elasticity * Config.getInstance().getElasticityMultiplier(); return this; }
    public Price setNoiseIntensity(float noiseIntensity) { this.noiseIntensity = noiseIntensity * Config.getInstance().getNoiseMultiplier(); return this; }
    public Price setSupport(double support) { this.support = support; return this; }
    public Price setResistance(double resistance) { this.resistance = resistance; return this; }

    public static float roundToDecimals(double value, int decimals) {
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(decimals, RoundingMode.HALF_UP); // Rounds up if the digit is >= 5
        return (float) bd.doubleValue();
    }

    public double getStockFromValue(double value) {
        return (Math.log(value / initialValue) / (-0.0005 * elasticity));
    }

}
