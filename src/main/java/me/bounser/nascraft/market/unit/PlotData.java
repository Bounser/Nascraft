package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.formatter.RoundUtils;
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

    public int[] getYPositions(int size, int offset, boolean polygon, boolean precise) {

        List<Float> values = item.getPrices(TimeSpan.HOUR);

        if (!precise) {
            values.replaceAll(RoundUtils::round);
        }

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

        List<Float> values = item.getPrices(TimeSpan.HOUR);

        float z = (float) size /(values.size()-1);

        if(polygon) {
            x = new int[values.size()+2];
        } else {
            x = new int[values.size()];
        }

        int j;
        for (j = 0; j < (values.size()); j++) {
            x[j] = Math.round(z*j + offset);
        }

        if(polygon) {
            x[values.size()] = size + offset;
            x[values.size()+1] = offset;
        }

        return x;
    }

    public int getNPoints(boolean polygon) { return polygon ? item.getPrices(TimeSpan.HOUR).size() + 2 : item.getPrices(TimeSpan.HOUR).size(); }

    public boolean isGoingUp() {

        List<Float> values = item.getPrices(TimeSpan.HOUR);

        return values.get(0) < values.get(values.size()-1);
    }

    public String getChange() {

        List<Float> values = item.getPrices(TimeSpan.HOUR);
        NumberFormat formatter = new DecimalFormat("#0.0");

        return formatter.format((-100 + values.get(values.size()-1)*100/values.get(0))) + "%";
    }

    public float[] getLowestValue(int size, int points) {

        float z = (float) size /(points-1);

        float min = Collections.min(item.getPrices(TimeSpan.HOUR));

        return new float[]{min, z*item.getPrices(TimeSpan.HOUR).lastIndexOf(min)};
    }

    public float[] getHighestValue(int size, int points) {

        float z = (float) size /(points-1);

        float max = Collections.max(item.getPrices(TimeSpan.HOUR));

        return new float[]{max, z*item.getPrices(TimeSpan.HOUR).lastIndexOf(max)};
    }

    public int[] getExtremePositions(int offset, int size) {

        if (Collections.min(item.getPrices(TimeSpan.HOUR)) == Collections.max(item.getPrices(TimeSpan.HOUR)))
            return new int[]{0, 0};

        int graphArea = (int) Math.round(size*0.9);
        return new int[]{offset + size-graphArea, offset + graphArea};

    }

}
