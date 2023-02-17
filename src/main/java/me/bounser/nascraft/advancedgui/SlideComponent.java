package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.tools.Config;
import me.leoko.advancedgui.utils.GuiPoint;
import me.leoko.advancedgui.utils.actions.Action;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.TextComponent;
import me.leoko.advancedgui.utils.interactions.Interaction;
import org.bukkit.entity.Player;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class SlideComponent extends RectangularComponent {

    List<Float> values;
    // Time frames: 1 = 15 min, 2 = 1 day, 3 = 1 Month, 4 = 1 year
    private int timeFrame;

    public SlideComponent(String id, Action clickAction, boolean hidden, Interaction interaction, int x, int y, int width, int height, List<Float> values) {
        super(id, clickAction, hidden, interaction, x, y, width, height);
        this.values = values;
        setTimeFrame(1);
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {

        // If It's not in bounds means that it's not being hovered.
        if(isInBounds(player, cursor)) {
            GroupComponent cTree = interaction.getComponentTree();

            cTree.locate("slider").setHidden(false);

            int[] points = cTree.locate("graph1", GraphComponent.class).getXPoints(false);

            int point = closestNumber(points, cursor.getX());

            cTree.locate("bar", RectComponent.class).setX(point);
            cTree.locate("translucid", RectComponent.class).setX(point);
            cTree.locate("translucid", RectComponent.class).setWidth((x + width - point));

            TextComponent ts1 = cTree.locate("textslide1", TextComponent.class);
            TextComponent timetext = cTree.locate("time1", TextComponent.class);
            TextComponent per = cTree.locate("perslide1", TextComponent.class);

            float value = values.get(findIndex(points, point));
            float multiplier = cTree.locate("graph1", GraphComponent.class).getChildMultiplier();

            timetext.setX(point);

            NumberFormat formatter = new DecimalFormat("#0.0");

            ts1.setText(round(value*multiplier) + Config.getInstance().getCurrency());

            ImageComponent up = cTree.locate("upgreen", ImageComponent.class);
            ImageComponent down = cTree.locate("downred", ImageComponent.class);

            if(values.get(0) > value) {
                up.setHidden(false);
                down.setHidden(true);
                per.setColor(new Color(255, 46, 46));
                per.setText("    " + formatter.format((-100 + value*100/values.get(0))) + "%");
            } else if (values.get(0) < value) {
                down.setHidden(false);
                up.setHidden(true);
                per.setColor((new Color(51, 238, 25)));
                per.setText("    " + formatter.format((-100 + value*100/values.get(0))) + "%");
            } else {
                down.setHidden(true);
                up.setHidden(true);
                per.setColor((new Color(250, 250, 250)));
                per.setText("~ 0%");
            }

            // Time stamp

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf;
            String time = "ERROR";

            switch (timeFrame) {
                // 30 min
                case 1:
                    sdf = new SimpleDateFormat("HH:mm");
                    cal.add(Calendar.MINUTE, Math.round(-30 * ((float) (x + width - point)/(float) width)));
                    time = sdf.format(cal.getTime());
                    break;
                // 1 day
                case 2:
                    sdf = new SimpleDateFormat("HH:mm");
                    cal.add(Calendar.HOUR, Math.round(-24 * ((float) (x + width - point)/(float) width)));
                    time = sdf.format(cal.getTime());
                    break;
                // 1 Month
                case 3:
                    sdf = new SimpleDateFormat("dd MMMM");
                    cal.add(Calendar.DATE, Math.round(-30 * ((float) (x + width - point)/(float) width)));
                    time = sdf.format(cal.getTime());
                    break;
                // 1 Year
                case 4:
                    sdf = new SimpleDateFormat("dd/MM/yyyy");
                    cal.add(Calendar.DATE, Math.round(-365 * ((float) (x + width - point)/(float) width)));
                    time = sdf.format(cal.getTime());
                    break;
            }
            timetext.setText(time);
        } else {
            interaction.getComponentTree().locate("slider").setHidden(true);
        }

    }

    @Override
    public String getState(Player player, GuiPoint cursor) {
        return cursor.getX() + ":";
    }

    @Override
    public Component clone(Interaction interaction) {
        return new SlideComponent(getId(), null, false, interaction, x, y, width, height, values);
    }

    public int closestNumber(int[] numbers, int x) {
        int closest = numbers[0];
        int minDiff = Math.abs(numbers[0] - x);
        for (int i = 1; i < numbers.length; i++) {
            int currentDiff = Math.abs(numbers[i] - x);
            if (currentDiff < minDiff) {
                closest = numbers[i];
                minDiff = currentDiff;
            }
        }
        return closest;
    }

    public int findIndex(int[] numbers, int x) {
        for (int i = 0; i < numbers.length; i++) {
            if (numbers[i] == x) {
                return i;
            }
        }
        return -1;
    }

    public void setValues(List<Float> values) {
        this.values = values;
    }

    public void setTimeFrame(int timeFrame) {
        this.timeFrame = timeFrame;
    }

    public static float round(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(Config.getInstance().getDecimalPrecission(), RoundingMode.HALF_UP);
        return bd.floatValue();
    }

}
