package me.bounser.nascraft.advancedgui;

import me.leoko.advancedgui.utils.GuiPoint;
import me.leoko.advancedgui.utils.actions.Action;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.RectangularComponent;
import me.leoko.advancedgui.utils.interactions.Interaction;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;


public class GraphComponent extends RectangularComponent {

    private HashMap<Float, Float> values;
    int width, height, yc, xc;

    // Time frames: 1 = 3 min, 2 = 1 hour, 3 = 1 week, 4 = 1 Month, 5 = 1 year
    private int timeFrame;

    public GraphComponent(String id, Action clickAction, boolean hidden, Interaction interaction, int x, int y, int width, int height, HashMap<Float, Float> values) {

        super(id, clickAction, hidden, interaction, x, y, width, height);

        this.values = values;
        this.width = width;
        this.height = height;
        this.xc = x;
        this.yc = y;

        timeFrame = 1;
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {
        graphic.setColor(new Color(0, 0, 0));

        graphic.fillPolygon(getXPoints(true), getYPoints(0, true), getXPoints(true).length);
        graphic.setColor(new Color(10, 200, 0));
        graphic.drawPolyline(getXPoints(false), getYPoints(0, false), getXPoints(false).length);
    }

    @Override
    public String getState(Player player, GuiPoint cursor) {
        return values.toString() + ":" + timeFrame;
    }

    @Override
    public Component clone(Interaction interaction) {
        return new GraphComponent(id, clickAction, hidden, interaction, x, y, width, height, values);
    }

    public void setTimeFrame(int timeFrame, HashMap<Float, Float> values) {
        this.timeFrame = timeFrame;
        this.values = values;
    }

    public void setValues(HashMap<Float, Float> values) {
        this.values = values;
    }

    public int[] getYPoints(int offset, boolean polygon) {

        int size = values.size();
        if(polygon) size += 2;
        int[] y = new int[size];

        float maxValue = Collections.max(values.values());
        float minValue = Collections.min(values.values());

        int i = 0;
        for (float value : values.values()) {

            int maxh = (int) Math.round(height*0.8);
            y[i] = (int) ((Math.round((maxh - maxh * (value - minValue) / (maxValue - minValue))) + yc - offset) + Math.round(height*0.05));
            i++;
        }
        if(polygon){
            y[i++] = yc;
            y[i] = yc;
        }
        return y;
    }

    public int[] getXPoints(boolean polygon) {

        int z = Math.round(width/(values.size()-1));
        int size = values.size();
        if(polygon) size += 2;
        int[] x = new int[size];

        int j = 0;
        for(int i = 0; i < (values.size()-1); i++) {
            x[j] = z*i + xc;
            j++;
        }
        x[j] = xc + width;
        if(polygon){
            j++;
            x[j++] = xc + width;
            x[j] = xc;
        }
        return x;
    }

}
