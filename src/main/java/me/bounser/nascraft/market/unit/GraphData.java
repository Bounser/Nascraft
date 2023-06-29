package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.market.managers.GraphManager;
import me.bounser.nascraft.market.resources.TimeSpan;

import java.util.Collections;
import java.util.List;

public class GraphData {

    private int[] x;
    private int[] y;
    private int[] px;
    private int[] py;

    private List<Float> values;
    private final TimeSpan timeSpan;
    private String state;
    private final int[] size = GraphManager.getInstance().getSize();
    private final int[] offset = GraphManager.getInstance().getOffset();

    public GraphData(TimeSpan timeSpan, List<Float> values) {
        this.timeSpan = timeSpan;
        this.values = values;
    }

    public TimeSpan getTimeSpan() { return timeSpan; }

    public void clear() { y = null; py = null;}

    public int[] getXPositions() {
        if(y == null) renderPositions();
        return x;
    }

    public int[] getYPositions() { return y; }

    public int[] getPXPositions() {
        if(py == null) renderPositions();
        return px;
    }

    public int[] getPYPositions() { return py; }

    public int getLength() { return x.length; }
    public int getPLength() { return px.length; }

    private void renderPositions() {

        // Y points
        y = new int[values.size()];
        py = new int[values.size() + 2];

        float maxValue = Collections.max(values);
        float minValue = Collections.min(values);

        int i = 0;
        for (float value : values) {
            int maxh = (int) Math.round(size[1]*0.8);
            y[i] = (int) ((Math.round((maxh - maxh * (value - minValue) / (maxValue - minValue))) + offset[1]) + Math.round(size[1]*0.05));
            py[i] = y[i];
            i++;
        }
        py[i++] = offset[1];
        py[i] = offset[1];

        // X points
        if(x == null || px == null) {
            int z = Math.round(size[0]/(values.size()-1));

            x = new int[values.size()];
            px = new int[values.size() + 2];

            int j;
            for (j = 0; j < (values.size()-1); j++) {
                x[j] = (int) (z*j + Math.round(j*0.5) + offset[0]);
                px[j] = x[j];
            }

            x[j] = offset[0] + size[0] + 1;
            px[j++] = offset[0] + size[0] + 2;
            px[j++] = offset[0] + size[0] + 2;
            px[j] = offset[0];
        }
    }

    public String getGraphState() { return state; }

    public void changeState() { state = String.valueOf((float) Math.random()); }

    public void setValues(List<Float> values) { this.values = values; }

    public List<Float> getValues() { return values; }

}
