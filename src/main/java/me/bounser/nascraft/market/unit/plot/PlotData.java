package me.bounser.nascraft.market.unit.plot;

import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.market.unit.Item;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;

public class PlotData {

    private int[] x;
    private int[] y;

    private final Item item;

    public PlotData(Item item) { this.item = item; }

    public int[] getYPositions(int size, int offset, boolean polygon, boolean precise) {

        List<Double> values = item.getPrice().getValuesPastHour();

        if (!precise) {
            try {
                //values.replaceAll(RoundUtils::round);
            } catch (ConcurrentModificationException e) {
                return new int[]{1};
            }
        }

        // Y points
        if(polygon) {
            y = new int[values.size()+2];
        } else {
            y = new int[values.size()];
        }

        double maxValue = Collections.max(values);
        double minValue = Collections.min(values);

        int i = 0;
        for (double value : values) {
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

        List<Double> values = item.getPrice().getValuesPastHour();

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

    public int getNPoints(boolean polygon) { return polygon ? item.getPrice().getValuesPastHour().size() + 2 : item.getPrice().getValuesPastHour().size(); }

    public boolean isGoingUp() {

        List<Double> values = item.getPrice().getValuesPastHour();

        return values.get(0) < values.get(values.size()-1);
    }

    public String getChange() {

        List<Double> values = item.getPrice().getValuesPastHour();
        NumberFormat formatter = new DecimalFormat("#0.0");

        return formatter.format((-100 + values.get(values.size()-1)*100/values.get(0))) + "%";
    }

    public double[] getLowestValue(int size, int points) {

        float z = (float) size /(points-1);

        Double min = Collections.min(item.getPrice().getValuesPastHour());

        return new double[]{min, z*item.getPrice().getValuesPastHour().lastIndexOf(min)};
    }

    public double[] getHighestValue(int size, int points) {

        float z = (float) size /(points-1);

        double max = Collections.max(item.getPrice().getValuesPastHour());

        return new double[]{max, z*item.getPrice().getValuesPastHour().lastIndexOf(max)};
    }

    public int[] getExtremePositions(int offset, int size) {

        if (Collections.min(item.getPrice().getValuesPastHour()) == Collections.max(item.getPrice().getValuesPastHour()))
            return new int[]{0, 0};

        int graphArea = (int) Math.round(size*0.9);
        return new int[]{offset + size-graphArea, offset + graphArea};

    }

}
