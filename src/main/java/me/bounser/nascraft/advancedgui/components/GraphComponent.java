package me.bounser.nascraft.advancedgui.components;

import me.bounser.nascraft.advancedgui.InteractionsManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.plot.GraphData;
import me.leoko.advancedgui.utils.GuiPoint;
import me.leoko.advancedgui.utils.actions.Action;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.interactions.Interaction;
import org.bukkit.entity.Player;

import java.awt.*;

public class GraphComponent extends RectangularComponent {

    private GraphData graphData;

    public int width, height, yc, xc;

    private final ViewComponent background;


    public GraphComponent(String id, Action clickAction, boolean hidden, Interaction interaction, int x, int y, int width, int height, ViewComponent backgroundView) {
        super(id, clickAction, hidden, interaction, x, y, width, height);

        this.width = width-1;
        this.height = height;
        this.xc = x;
        this.yc = y;

        background = backgroundView;
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {

        graphData = new GraphData(InteractionsManager.getInstance().getItemFromPlayer(player));

        Color bgcolor = setupBackGround();

        background.apply(graphic, player, cursor);

        graphic.setColor(new Color(0, 0, 0));

        graphic.fillPolygon(graphData.getPXPositions(), graphData.getPYPositions(), graphData.getPLength());

        graphic.setColor(bgcolor);

        graphic.drawPolyline(graphData.getXPositions(), graphData.getYPositions(), graphData.getLength());
    }

    @Override
    public String getState(Player player, GuiPoint cursor) {

        Item item = InteractionsManager.getInstance().getItemFromPlayer(player);

        if (item == null) return "0";

        return item.getValuesPastHour() == null ? "0" : item.getValuesPastHour().toString();
    }

    @Override
    public Component clone(Interaction interaction) {
        return new GraphComponent(id, clickAction, hidden, interaction, x, y, width, height, background.clone(interaction));
    }

    public Color setupBackGround() {

        if (graphData.getValues().size() > 1) {
            float first = graphData.getValues().get(0);
            float last = graphData.getValues().get(graphData.getValues().size() - 1);

            if (Float.compare(first, last) < 0) {
                background.setView("bull123");
                return new Color(0,200,20);

            } else if (Float.compare(first, last) > 0){
                background.setView("bear123");
                return new Color(200,10,20);

            } else {
                background.setView("flat123");
                return new Color(250,250,250);
            }
        }
        return new Color(250,250,250);
    }

    public GraphData getGraphData() { return graphData; }

}
