package me.bounser.nascraft.advancedgui.components;

import me.bounser.nascraft.advancedgui.Images;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.market.unit.GraphData;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.formatter.RoundUtils;
import me.leoko.advancedgui.manager.ResourceManager;
import me.leoko.advancedgui.utils.GuiPoint;
import me.leoko.advancedgui.utils.actions.Action;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.components.Component;
import me.leoko.advancedgui.utils.components.TextComponent;
import me.leoko.advancedgui.utils.interactions.Interaction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.awt.*;
import java.util.*;
import java.util.List;

public class GraphComponent extends RectangularComponent {

    private Item item;

    private GraphData graphData;

    private List<ItemStack> childsMat;
    private HashMap<ItemStack, Float> childs;
    public int width, height, yc, xc;

    private final ViewComponent background;
    private final TextComponent mainText;
    private GroupComponent childComponents;

    private SlideComponent slideComponent;

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

        graphic.fillPolygon(graphData.getPXPositions(), graphData.getPYPositions(), graphData.getPLength());

        graphic.setColor(bgcolor);

        graphic.drawPolyline(graphData.getXPositions(), graphData.getYPositions(), graphData.getLength());
    }

    @Override
    public String getState(Player player, GuiPoint cursor) { return graphData.getGraphState(); }

    @Override
    public Component clone(Interaction interaction) {
        return new GraphComponent(id, clickAction, hidden, interaction, x, y, width, height, background.clone(interaction), mainText.clone(interaction));
    }

    public void setTimeFrame(TimeSpan timeFrame) {

        graphData = item.getGraphData(timeFrame);
        interaction.getComponentTree().locate("timeview", ViewComponent.class).setView("times" + timeFrame);

        setGraphData(graphData);
        graphData.changeState();
    }

    public void setGraphData(GraphData graphData) { slideComponent.setGraphData(graphData); }

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

    public void updateButtonPrice() {

        GroupComponent ct = interaction.getComponentTree();

        for (int i : Arrays.asList(1, 16, 64)) {
            ct.locate("buyprice" + i, TextComponent.class).setText(RoundUtils.round(getItem().getPrice().getBuyPrice()*i*childs.get(childsMat.get(0))) + Lang.get().message(Message.CURRENCY));
            ct.locate("sellprice" + i, TextComponent.class).setText(RoundUtils.round(getItem().getPrice().getSellPrice()*i*childs.get(childsMat.get(0))) + Lang.get().message(Message.CURRENCY));
        }

    }

    public void changeMat(Item item) {

        this.item = item;

        if (slideComponent == null) { slideComponent = this.interaction.getComponentTree().locate("slide1", SlideComponent.class); }

        childComponents = interaction.getComponentTree().locate("childC", GroupComponent.class);

        this.childs = item.getChilds();

        graphData = item.getGraphData(TimeSpan.HOUR);

        ImageComponent ic = childComponents.locate("MainImage", ImageComponent.class);

        ic.setImage(ResourceManager.getInstance().processImage(item.getIcon(), 60, 60, true));

        mainText.setText(item.getName());

        setGraphData(item.getGraphData(TimeSpan.HOUR));

        if (childs == null) {
            childComponents.locate("childs").setHidden(true);
            childComponents.locate("minichild").setHidden(true);
            childsMat = new ArrayList<>();
            childsMat.add(0, item.getItemStack());
        } else {
            childComponents.locate("childs").setHidden(false);
            childComponents.locate("minichild").setHidden(false);

            childsMat = new ArrayList<>(childs.keySet());

            while (!(childsMat.get(0).equals(item.getItemStack()))) {
                Collections.rotate(childsMat, 1);
            }

            updateChilds(childs);

            childComponents.locate("childact").setClickAction((interaction, player, primaryTrigger) -> {
                changeChildsOrder();
                updateChilds(childs);
            });
        }
        setTimeFrame(TimeSpan.HOUR);

        graphData.changeState();
    }

    public void updateChilds(HashMap<ItemStack, Float> childs) {

        for (int i = 1; i <= 8 ; i++) {

            if (childs.keySet().size() >= i) {
                childComponents.locate("child" + i, ImageComponent.class).setImage(Images.getInstance().getImage(childsMat.get(i-1).getType(), 32, 32, true));
                childComponents.locate("child" + i, ImageComponent.class).setHidden(false);
            } else {
                childComponents.locate("child" + i, ImageComponent.class).setHidden(true);
            }
            childComponents.locate("childback", RectComponent.class).setWidth(10 + 33*childs.keySet().size());
        }

        if (childsMat.get(0).equals(item.getItemStack().getType())) {
            childComponents.locate("minichild").setHidden(true);
        } else {
            childComponents.locate("minichild").setHidden(false);
            childComponents.locate("childper", ImageComponent.class).setImage(Images.getInstance().getImage(childsMat.get(0).getType(), 26, 26, true));
        }
        updateButtonPrice();
    }

    public void changeChildsOrder() { Collections.rotate(childsMat, -1); }

    public Item getItem() { return item; }

    public Material getMaterial() {
        if (childsMat == null) return item.getItemStack().getType();
        else return childsMat.get(0).getType();
    }

    public float getMultiplier() {
        if (childs != null) return childs.get(childsMat.get(0));
        else return 1;
    }

    public GraphData getGraphData() { return graphData; }

}
