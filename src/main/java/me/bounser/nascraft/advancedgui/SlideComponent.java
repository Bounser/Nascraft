package me.bounser.nascraft.advancedgui;

import me.leoko.advancedgui.utils.GuiPoint;
import me.leoko.advancedgui.utils.actions.Action;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.GroupComponent;
import me.leoko.advancedgui.utils.components.RectComponent;
import me.leoko.advancedgui.utils.components.TextComponent;
import me.leoko.advancedgui.utils.components.RectangularComponent;
import me.leoko.advancedgui.utils.interactions.Interaction;
import org.bukkit.entity.Player;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public class SlideComponent extends RectangularComponent {

    List<Float> values;
    // Time frames: 1 = 15 min, 2 = 1 day, 3 = 1 Month, 4 = 1 year, 5 = 1 ytd
    private int timeFrame;

    public SlideComponent(String id, Action clickAction, boolean hidden, Interaction interaction, int x, int y, int width, int height, List<Float> values) {
        super(id, clickAction, hidden, interaction, x, y, width, height);
        this.values = values;
        setTimeFrame(1);
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {

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

            ts1.setX(point);
            timetext.setX(point);
            per.setX(point);

            NumberFormat formatter = new DecimalFormat("#0.0");

            ts1.setText(String.valueOf(value));
            per.setText("(" + formatter.format((-100 + value*100/values.get(0))) + "%)");

            if(values.get(0) > value) {
                per.setColor(new Color(200, 0, 0));
            } else if (values.get(0) < value) {
                per.setColor((new Color(0, 200, 0)));
            } else {
                per.setColor((new Color(250, 250, 250)));
            }

            // Time stamp

            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf;
            String time = "ERROR";

            switch (timeFrame) {
                // 15 min
                case 1:
                    sdf = new SimpleDateFormat("HH:mm");
                    cal.add(Calendar.MINUTE, Math.round(-15 * ((float) (x + width - point)/(float) width)));
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
                // Ytd
                case 5:
                    Calendar resultDate = Calendar.getInstance();
                    Calendar startOfYear = Calendar.getInstance();
                    startOfYear.set(Calendar.MONTH, Calendar.JANUARY);
                    startOfYear.set(Calendar.DAY_OF_MONTH, 1);

                    long daysBetween = ((resultDate.getTimeInMillis() - startOfYear.getTimeInMillis()) / (24 * 60 * 60 * 1000))* ((x + width - point)/width);

                    resultDate.setTimeInMillis(startOfYear.getTimeInMillis() + (daysBetween * 24 * 60 * 60 * 1000));

                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                    time = dateFormat.format(resultDate.getTime());
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

}
