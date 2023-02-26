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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

public class SlideComponent extends RectangularComponent {

    private List<Float> values;
    private TimeSpan timeFrame;

    List<Component> components;

    GraphComponent graphComponent;

    private final RectComponent bar;
    private final RectComponent translucid;
    private final ImageComponent up;
    private final ImageComponent down;
    private final TextComponent textslide;
    private final TextComponent timetext;
    private final TextComponent perslide;

    public SlideComponent(String id, Action clickAction, boolean hidden, Interaction interaction, int x, int y, int width, int height, RectComponent bar, RectComponent translucid, ImageComponent up, ImageComponent down, TextComponent textslide, TextComponent timetext, TextComponent perslide, GraphComponent gc) {
        super(id, clickAction, hidden, interaction, x, y, width, height);
        this.values = Arrays.asList(1f, 1f);
        setTimeFrame(TimeSpan.MINUTE);

        graphComponent = gc;

        this.bar = bar;
        this.translucid = translucid;
        this.up = up;
        this.down = down;
        this.textslide = textslide;
        this.timetext = timetext;
        this.perslide = perslide;

        components = Arrays.asList(this.bar, this.translucid, this.up,  this.down,  this.textslide,  this.timetext,  this.perslide);
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {

        int[] points = graphComponent.getXPoints(false);

        int point = closestNumber(points, cursor.getX());

        bar.setX(point);
        translucid.setX(point);
        translucid.setWidth((x + width - point));

        float value = values.get(findIndex(points, point));
        float multiplier = graphComponent.getChildMultiplier();

        timetext.setX(point);

        NumberFormat formatter = new DecimalFormat("#0.0");

        textslide.setText(NUtils.round(value*multiplier) + Config.getInstance().getCurrency());

        if (values.get(0) > value) {
            up.setHidden(false);
            down.setHidden(true);
            perslide.setColor(new Color(255, 46, 46));
            perslide.setText("    " + formatter.format((-100 + value*100/values.get(0))*-1) + "%");
        } else if (values.get(0) < value) {
            down.setHidden(false);
            up.setHidden(true);
            perslide.setColor((new Color(51, 238, 25)));
            perslide.setText("    " + formatter.format((-100 + value*100/values.get(0))) + "%");
        } else {
            down.setHidden(true);
            up.setHidden(true);
            perslide.setColor((new Color(250, 250, 250)));
            perslide.setText("~ 0%");
        }

        timetext.setText(TimeSpan.getTime(timeFrame, (x + width - point/(float) width)));

        components.forEach(comp -> comp.apply(graphic, player, cursor));
    }

    @Override
    public String getState(Player player, GuiPoint cursor) {
        return cursor.getX() + ":";
    }

    @Override
    public Component clone(Interaction interaction) {
        return new SlideComponent(getId(), clickAction, hidden, interaction, x, y, width, height, bar.clone(interaction), translucid.clone(interaction), up.clone(interaction), down.clone(interaction), textslide.clone(interaction), timetext.clone(interaction), perslide.clone(interaction), graphComponent);
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
