package me.bounser.nascraft.advancedgui.components;

import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.NUtils;
import me.bounser.nascraft.tools.TimeSpan;
import me.leoko.advancedgui.utils.GuiPoint;
import me.leoko.advancedgui.utils.actions.Action;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.TextComponent;
import me.leoko.advancedgui.utils.interactions.Interaction;
import org.bukkit.entity.Player;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class SlideComponent extends RectangularComponent {

    private List<Float> values;
    private TimeSpan timeFrame;

    GroupComponent components;

    public SlideComponent(String id, Action clickAction, boolean hidden, Interaction interaction, int x, int y, int width, int height, GroupComponent component) {
        super(id, clickAction, hidden, interaction, x, y, width, height);
        this.values = Arrays.asList(1f, 1f);
        setTimeFrame(TimeSpan.MINUTE);

        this.components = component;
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {

        int[] points = interaction.getComponentTree().locate("graph1", GraphComponent.class).getXPoints(false);

        int point = closestNumber(points, cursor.getX());

        components.locate("bar", RectComponent.class).setX(point);
        components.locate("translucid", RectComponent.class).setX(point);
        components.locate("translucid", RectComponent.class).setWidth((x + width - point));

        TextComponent ts1 = components.locate("textslide1", TextComponent.class);
        TextComponent timetext = components.locate("time1", TextComponent.class);
        TextComponent per = components.locate("perslide1", TextComponent.class);

        float value = values.get(findIndex(points, point));
        float multiplier = interaction.getComponentTree().locate("graph1", GraphComponent.class).getChildMultiplier();

        timetext.setX(point);

        NumberFormat formatter = new DecimalFormat("#0.0");

        ts1.setText(NUtils.round(value*multiplier) + Config.getInstance().getCurrency());

        ImageComponent up = components.locate("upgreen", ImageComponent.class);
        ImageComponent down = components.locate("downred", ImageComponent.class);

        if (values.get(0) > value) {
            up.setHidden(false);
            down.setHidden(true);
            per.setColor(new Color(255, 46, 46));
            per.setText("    " + formatter.format((-100 + value*100/values.get(0))*-1) + "%");
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
        SimpleDateFormat sdf = new SimpleDateFormat("mm");
        timetext.setText("ERROR");

        switch (timeFrame) {
            // 30 min
            case MINUTE:
                sdf = new SimpleDateFormat("HH:mm");
                cal.add(Calendar.MINUTE, Math.round(-30 * ((float) (x + width - point)/(float) width)));
                break;
            // 1 day
            case DAY:
                sdf = new SimpleDateFormat("HH:mm");
                cal.add(Calendar.HOUR, Math.round(-24 * ((float) (x + width - point)/(float) width)));
                break;
            // 1 Month
            case MONTH:
                sdf = new SimpleDateFormat("dd MMMM");
                cal.add(Calendar.DATE, Math.round(-30 * ((float) (x + width - point)/(float) width)));
                break;
            // 1 Year
            case YEAR:
                sdf = new SimpleDateFormat("dd/MM/yyyy");
                cal.add(Calendar.DATE, Math.round(-365 * ((float) (x + width - point)/(float) width)));
                break;
        }
        timetext.setText(sdf.format(cal.getTime()));

        components.apply(graphic, player, cursor);
    }

    @Override
    public String getState(Player player, GuiPoint cursor) {
        return cursor.getX() + ":";
    }

    @Override
    public Component clone(Interaction interaction) {
        return new SlideComponent(getId(), null, false, interaction, x, y, width, height, components);
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

    public void setTimeFrame(TimeSpan timeFrame) {
        this.timeFrame = timeFrame;
    }

}
