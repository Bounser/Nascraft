package me.bounser.nascraft.market;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Plot {


    public static int[] getYPositions(int height, int offset, boolean polygon, List<Float> values) {

        int[] y;

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
            int maxh = (int) Math.round(height * 0.8);
            y[i] = (int) ((Math.round((maxh - maxh * (value - minValue) / (maxValue - minValue))) + offset) + Math.round(height * 0.05));
            i++;
        }

        if(polygon) {
            y[values.size()] = offset;
            y[values.size()+1] = offset;
        }

        return y;
    }

    public static int[] getXPositions(int width, int offset, boolean polygon, int pointsSize) {

        int[] x;

        float z = (float) width /(pointsSize-1);

        if(polygon) {
            x = new int[pointsSize+2];
        } else {
            x = new int[pointsSize];
        }

        int j;
        for (j = 0; j < pointsSize; j++) {
            x[j] = Math.round(z*j + offset);
        }

        if(polygon) {
            x[pointsSize] = pointsSize + offset;
            x[pointsSize+1] = offset;
        }

        return x;
    }

    public static int[] getYYPositions(int height, int offset, boolean polygon, List<Float> values1, List<Float> values2) {

        int[] y;

        // Y points
        if(polygon) {
            y = new int[values1.size()+2];
        } else {
            y = new int[values1.size()];
        }

        List<Float> merged = new ArrayList<>(values1);
        merged.addAll(values2);

        float maxValue = Collections.max(merged);
        float minValue = Collections.min(merged);

        int i = 0;
        for (float value : values1) {
            int maxh = (int) Math.round(height * 0.8);
            y[i] = (int) ((Math.round((maxh - maxh * (value - minValue) / (maxValue - minValue))) + offset) + Math.round(height * 0.05));
            i++;
        }

        if(polygon) {
            y[values1.size()] = offset;
            y[values1.size()+1] = offset;
        }

        return y;
    }

}
