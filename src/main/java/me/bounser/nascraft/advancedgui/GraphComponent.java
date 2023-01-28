package me.bounser.nascraft.advancedgui;

import me.leoko.advancedgui.utils.GuiPoint;
import me.leoko.advancedgui.utils.actions.Action;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.TextComponent;
import me.leoko.advancedgui.utils.interactions.Interaction;
import org.bukkit.entity.Player;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;


public class GraphComponent extends RectangularComponent {

    List<Float> values ;
    int width, height, yc, xc;

    // Time frames: 1 = 15 min, 2 = 1 day, 3 = 1 Month, 4 = 1 year, 5 = 1 ytd
    private int timeFrame;

    public GraphComponent(String id, Action clickAction, boolean hidden, Interaction interaction, int x, int y, int width, int height, List<Float> values) {

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

        setScaleReferences();

        graphic.setColor(new Color(0, 0, 0));

        graphic.fillPolygon(getXPoints(true), getYPoints(0, true), getXPoints(true).length);

        graphic.setColor(setupBackGround());

        graphic.drawPolyline(getXPoints(false), getYPoints(0, false), getXPoints(false).length);

        interaction.getComponentTree().locate("timespan1", ViewComponent.class).setView("opt" + timeFrame);

        Calendar cal = Calendar.getInstance();

        switch (timeFrame){

            // 10 min
            case 1:
                for(int i = 1; i<=3 ; i++){
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    String time = sdf.format(cal.getTime());
                    interaction.getComponentTree().locate("time" + i, TextComponent.class).setText(time);
                    cal.add(Calendar.MINUTE, -5);
                }
                break;
            // 1 day
            case 2:
                for(int i = 1; i<=3 ; i++){
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    String time = sdf.format(cal.getTime());
                    interaction.getComponentTree().locate("time" + i, TextComponent.class).setText(time);
                    cal.add(Calendar.HOUR, -12);
                }
                break;
            // 1 Month
            case 3:
                for(int i = 1; i<=3 ; i++){
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM");
                    String time = sdf.format(cal.getTime());
                    interaction.getComponentTree().locate("time" + i, TextComponent.class).setText(time);
                    cal.add(Calendar.DATE, -13);
                }
                break;
            // 1 Year
            case 4:
                for(int i = 1; i<=3 ; i++){
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM");
                    String time = sdf.format(cal.getTime());
                    interaction.getComponentTree().locate("time" + i, TextComponent.class).setText(time);
                    cal.add(Calendar.DATE, -182);
                }
                break;
            // Ytd
            case 5:
                Calendar currentDate = Calendar.getInstance();
                Calendar startYear = Calendar.getInstance();
                startYear.set(Calendar.MONTH, Calendar.JANUARY);
                startYear.set(Calendar.DAY_OF_MONTH, 1);

                SimpleDateFormat sdf = new SimpleDateFormat("dd MM yyyy");

                interaction.getComponentTree().locate("time3", TextComponent.class).setText(sdf.format(startYear));
                interaction.getComponentTree().locate("time1", TextComponent.class).setText(sdf.format(currentDate));

                long diff = currentDate.getTimeInMillis() - startYear.getTimeInMillis();
                long middle = diff / 2;

                Calendar middleDate = Calendar.getInstance();
                middleDate.setTimeInMillis(startYear.getTimeInMillis() + middle);

                interaction.getComponentTree().locate("time2", TextComponent.class).setText(sdf.format(middleDate));
                break;
        }
    }

    @Override
    public String getState(Player player, GuiPoint cursor) {

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String time = sdf.format(cal.getTime());
        return values.toString() + ":" + timeFrame + ":" + time;
    }

    @Override
    public Component clone(Interaction interaction) {
        return new GraphComponent(id, clickAction, hidden, interaction, x, y, width, height, values);
    }

    public void setTimeFrame(int timeFrame, List<Float> values) {
        this.timeFrame = timeFrame;
        this.values = values;
    }

    public void setValues(List<Float> values) {
        this.values = values;
    }

    public int[] getYPoints(int offset, boolean polygon) {

        int size = values.size();
        if(polygon) size += 2;
        int[] y = new int[size];

        float maxValue = Collections.max(values);
        float minValue = Collections.min(values);

        int i = 0;
        for (float value : values) {

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
        if (polygon) size += 2;
        int[] x = new int[size];

        int j = 0;
        for (int i = 0; i < (values.size()-1); i++) {
            x[j] = z*i + xc;
            j++;
        }
        x[j] = xc + width;
        if (polygon) {
            j++;
            x[j++] = xc + width;
            x[j] = xc;
        }
        return x;
    }

    public Color setupBackGround() {

        if(values.size() > 1) {
            float first = values.get(0);
            float last = values.get(values.size() - 1);
            if (Float.compare(first, last) < 0) {
                this.interaction.getComponentTree().locate("bear123").setHidden(true);
                return new Color(0,200,20);
            } else {
                this.interaction.getComponentTree().locate("bear123").setHidden(false);
                return new Color(200,10,20);
            }
        }
        return new Color(250,250,250);
    }

    public void setScaleReferences() {

        float maxValue = Collections.max(values);
        float minValue = Collections.min(values);

        float escalated = (int) ((Math.round((height*0.8 - height*0.8 * (-minValue) / (maxValue - minValue))) + yc) + Math.round(height*0.05));
        float deescalated;
        int i = 1;

        if (escalated < height + xc) {
            TextComponent tc = interaction.getComponentTree().locate("scale0", TextComponent.class);
            tc.setText("0");
            tc.setY((int) escalated);
            tc.setHidden(false);

            RectComponent rc = interaction.getComponentTree().locate("last", RectComponent.class);
            rc.setY((int) escalated);
            rc.setHidden(false);
        }

        for (float val : Arrays.asList(79, 109, 139, 169)) {
            deescalated = ((-val+yc+Math.round(height*0.85))*(maxValue-minValue)/(Math.round(height*0.8))) + minValue;
            interaction.getComponentTree().locate("scale" + i, TextComponent.class).setText(String.valueOf(round(deescalated)));
            i++;
        }
    }

    private double round(double value) {
        int scale = (int) Math.pow(10, 1);
        return (double) Math.round(value * scale) / scale;
    }

}
