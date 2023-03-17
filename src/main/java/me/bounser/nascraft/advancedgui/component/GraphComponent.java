package me.bounser.nascraft.advancedgui.component;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.unit.GraphData;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.NUtils;
import me.bounser.nascraft.market.managers.resources.TimeSpan;
import me.leoko.advancedgui.utils.GuiPoint;
import me.leoko.advancedgui.utils.actions.Action;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.TextComponent;
import me.leoko.advancedgui.utils.interactions.Interaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.*;
import java.util.List;

public class GraphComponent extends RectangularComponent {

    private Item item;

    private GraphData gd;

    private List<Float> values;
    private List<String> childsMat;
    private HashMap<String, Float> childs;
    public int width, height, yc, xc;

    private final ViewComponent background;
    private final TextComponent mainText;
    private GroupComponent childComponents;

    private SlideComponent sc;

    public GraphComponent(String id, Action clickAction, boolean hidden, Interaction interaction, int x, int y, int width, int height, ViewComponent backgroundView, TextComponent mainText) {
        super(id, clickAction, hidden, interaction, x, y, width, height);

        this.width = width-1;
        this.height = height;
        this.xc = x;
        this.yc = y;

        background = backgroundView;
        this.mainText = mainText;
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {

        Color bgcolor = setupBackGround();

        background.apply(graphic, player, cursor);
        mainText.apply(graphic, player, cursor);

        updateButtonPrice();

        graphic.setColor(new Color(0, 0, 0));

        graphic.fillPolygon(gd.getPXPositions(), gd.getPYPositions(), gd.getPLength());

        graphic.setColor(bgcolor);

        graphic.drawPolyline(gd.getXPositions(), gd.getYPositions(), gd.getLength());
    }

    @Override
    public String getState(Player player, GuiPoint cursor) {
        return gd.getGraphState();
    }

    @Override
    public Component clone(Interaction interaction) {
        return new GraphComponent(id, clickAction, hidden, interaction, x, y, width, height, background.clone(interaction), mainText.clone(interaction));
    }

    public void setTimeFrame(TimeSpan timeFrame) {

        gd = item.getGraphData(timeFrame);
        sc.setTimeFrame(timeFrame);
        interaction.getComponentTree().locate("timeview", ViewComponent.class).setView("times" + timeFrame);

        setValues(item.getPrices(timeFrame));
        gd.changeState();
    }

    public void setValues(List<Float> values) {
        this.values = values;
        sc.setValues(values);
    }

    public Color setupBackGround() {

        if (values.size() > 1) {
            float first = values.get(0);
            float last = values.get(values.size() - 1);

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

    public void updateButtonPrice() {

        GroupComponent ct = interaction.getComponentTree();
        if (childs == null) {
            for (int i : Arrays.asList(1, 16, 64)) {
                ct.locate("buyprice" + i, TextComponent.class).setText(getItem().getBuyPrice()*i + Config.getInstance().getCurrency());
                ct.locate("sellprice" + i, TextComponent.class).setText(getItem().getSellPrice()*i + Config.getInstance().getCurrency());
            }
        } else {
            for (int i : Arrays.asList(1, 16, 64)) {
                ct.locate("buyprice" + i, TextComponent.class).setText(NUtils.round(getItem().getBuyPrice()*i*childs.get(childsMat.get(0))) + Config.getInstance().getCurrency());
                ct.locate("sellprice" + i, TextComponent.class).setText(NUtils.round(getItem().getSellPrice()*i*childs.get(childsMat.get(0))) + Config.getInstance().getCurrency());
            }
        }
    }

    public void changeMat(String mat) {

        if(sc == null) { sc = this.interaction.getComponentTree().locate("slide1", SlideComponent.class); }

        childComponents = interaction.getComponentTree().locate("childC", GroupComponent.class);

        item = MarketManager.getInstance().getItem(mat);
        this.childs = item.getChilds();

        gd = item.getGraphData(TimeSpan.MINUTE);

        ImageComponent ic = childComponents.locate("MainImage", ImageComponent.class);
        ic.setImage(NUtils.getImage(mat, 60, 60, true));

        mainText.setText(item.getName());

        setValues(MarketManager.getInstance().getItem(mat).getPrices(TimeSpan.MINUTE));

        if (childs == null) {
            childComponents.locate("childs").setHidden(true);
            childComponents.locate("minichild").setHidden(true);
            childsMat = new ArrayList<>();
            childsMat.add(0, mat);
        } else {
            childComponents.locate("childs").setHidden(false);
            childComponents.locate("minichild").setHidden(false);

            childsMat = new ArrayList<>(childs.keySet());

            while (!childsMat.get(0).equals(mat)) {
                Collections.rotate(childsMat, 1);
            }

            updateChilds(childs);

            childComponents.locate("childact").setClickAction((interaction, player, primaryTrigger) -> {
                changeChildsOrder();
                updateChilds(childs);
            });
        }
        setTimeFrame(TimeSpan.MINUTE);

        gd.changeState();
    }

    public void updateChilds(HashMap<String, Float> childs) {

        for (int i = 1; i <= 8 ; i++) {

            if (childs.keySet().size() >= i) {
                childComponents.locate("child" + i, ImageComponent.class).setImage(NUtils.getImage(childsMat.get(i-1), 32, 32, true));
                childComponents.locate("child" + i, ImageComponent.class).setHidden(false);
            } else {
                childComponents.locate("child" + i, ImageComponent.class).setHidden(true);
            }
            childComponents.locate("childback", RectComponent.class).setWidth(10 + 33*childs.keySet().size());
        }

        if (childsMat.get(0).equals(item.getMaterial())) {
            childComponents.locate("minichild").setHidden(true);
        } else {
            childComponents.locate("minichild").setHidden(false);
            childComponents.locate("childper", ImageComponent.class).setImage(NUtils.getImage(childsMat.get(0), 26, 26, true));
        }
        updateButtonPrice();
    }

    public void changeChildsOrder() {
        Collections.rotate(childsMat, -1);
    }

    public Item getItem() {
        return item;
    }

    public String getMat() {
        if (childsMat == null) return item.getMaterial();
        else return childsMat.get(0);
    }

    public float getMultiplier() {
        if (childs != null) return childs.get(childsMat.get(0));
        else return 1;
    }

    public GraphData getGraphData() { return gd; }

}
