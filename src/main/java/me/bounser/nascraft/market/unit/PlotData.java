package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.market.resources.TimeSpan;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;

public class PlotData {

    private int[] x;
    private int[] y;

    private final Item item;

    public PlotData(Item item) { this.item = item; }

    public int[] getYPositions(int size, int offset, boolean polygon) {

        List<Float> values = item.getPrices(TimeSpan.HALFHOUR);

        // Y points
        if(polygon) {
            y = new int[values.size()+2];
        } else {
            y = new int[values.size()];
        }


        float maxValue = Collections.max(values);
        float minValue = Collections.min(values);

        int i = 0;
        for (float value : values) {
            int maxh = (int) Math.round(size * 0.8);
            y[i] = (int) ((Math.round((maxh - maxh * (value - minValue) / (maxValue - minValue))) + offset) + Math.round(size * 0.05));
            i++;
        }

        if(polygon) {
            y[values.size()] = offset;
            y[values.size()+1] = offset;
        }

        return y;
    }

    public int[] getXPositions(int size, int offset, boolean polygon) {

        List<Float> values = item.getPrices(TimeSpan.HALFHOUR);

        int z = Math.round((float) size /(values.size()-1));

        if(polygon) {
            x = new int[values.size()+2];
        } else {
            x = new int[values.size()];
        }

        int j;
        for (j = 0; j < (values.size()-1); j++) {
            x[j] = (z*j + offset);
        }

        x[j] = offset + size + 1;

        if(polygon) {
            x[values.size()] = size + offset;
            x[values.size()+1] = offset;
        }

        return x;
    }

    public int getNPoints(boolean polygon) { return polygon ? item.getPrices(TimeSpan.HALFHOUR).size() + 2 : item.getPrices(TimeSpan.HALFHOUR).size(); }

    public boolean isGoingUp() {

        List<Float> values = item.getPrices(TimeSpan.HALFHOUR);

        return values.get(0) < values.get(values.size()-1);
    }

    public String getChange() {

        List<Float> values = item.getPrices(TimeSpan.HALFHOUR);
        NumberFormat formatter = new DecimalFormat("#0.0");

        return formatter.format((-100 + values.get(values.size()-1)*100/values.get(0))) + "%";
    }

}
