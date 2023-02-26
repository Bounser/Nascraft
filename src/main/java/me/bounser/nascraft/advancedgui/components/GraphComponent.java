package me.bounser.nascraft.advancedgui.components;

import me.bounser.nascraft.market.Item;
import me.bounser.nascraft.market.MarketManager;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class GraphComponent extends RectangularComponent {

    private Item item;
    private List<Float> values;
    private List<String> childsMat;
    private HashMap<String, Float> childs;
    public int width, height, yc, xc;

    private TimeSpan timeFrame;

    private final ViewComponent background;
    private final TextComponent mainText;
    private GroupComponent childComponents;

    public GraphComponent(String id, Action clickAction, boolean hidden, Interaction interaction, int x, int y, int width, int height, ViewComponent backgroundView, TextComponent mainText) {
        super(id, clickAction, hidden, interaction, x, y, width, height);

        this.width = width-1;
        this.height = height;
        this.xc = x;
        this.yc = y;
        this.values = Arrays.asList(1f, 1f);

        timeFrame = TimeSpan.MINUTE;

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

        graphic.fillPolygon(getXPoints(true), getYPoints(0, true), getXPoints(true).length);

        graphic.setColor(bgcolor);

        graphic.drawPolyline(getXPoints(false), getYPoints(0, false), getXPoints(false).length);
    }

    @Override
    public String getState(Player player, GuiPoint cursor) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd:HH:mm");
        String time = sdf.format(cal.getTime());
        return timeFrame + "-" + time + "-" + item.getMaterial();
    }

    @Override
    public Component clone(Interaction interaction) {
        return new GraphComponent(id, clickAction, hidden, interaction, x, y, width, height, background.clone(interaction), mainText.clone(interaction));
    }

    public void setTimeFrame(TimeSpan timeFrame) {

        this.timeFrame = timeFrame;
        interaction.getComponentTree().locate("slide1", SlideComponent.class).setTimeFrame(timeFrame);
        interaction.getComponentTree().locate("timeview", ViewComponent.class).setView("times" + timeFrame);

        switch(timeFrame) {
            case MINUTE:
                setValues(item.getPricesM());
                break;
            case DAY:
                setValues(item.getPricesH());
                break;
            case MONTH:
                setValues(item.getPricesMM());
                break;
            case YEAR:
                setValues(item.getPricesY());
                break;
        }
    }

    public void setValues(List<Float> values) {
        this.values = values;
        interaction.getComponentTree().locate("slide1", SlideComponent.class).setValues(values);
    }

    public int[] getYPoints(int offset, boolean polygon) {

        int size = values.size();
        if (polygon) size += 2;
        int[] y = new int[size];

        float maxValue = Collections.max(values);
        float minValue = Collections.min(values);

        int i = 0;
        for (float value : values) {

            int maxh = (int) Math.round(height*0.8);
            y[i] = (int) ((Math.round((maxh - maxh * (value - minValue) / (maxValue - minValue))) + yc - offset) + Math.round(height*0.05));
            i++;
        }
        if (polygon){
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

        float a = 0;
        if (timeFrame != TimeSpan.MONTH) a = 0.5f;
        for (int i = 0; i < (values.size()-1); i++) {
            x[j] = (z*i + Math.round(a*i) + xc);
            j++;
        }
        if (polygon) {
            x[j++] = xc + width + 2;
            x[j++] = xc + width + 2;
            x[j] = xc;
        } else {
            x[j++] = xc + width + 1;
        }
        return x;
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

        childComponents = interaction.getComponentTree().locate("childC", GroupComponent.class);

        item = MarketManager.getInstance().getItem(mat);
        this.childs = item.getChilds();

        GroupComponent ct = interaction.getComponentTree();

        ImageComponent ic = childComponents.locate("MainImage", ImageComponent.class);
        ic.setImage(NUtils.getImage(mat, 60, 60, true));
        String modified = Character.toUpperCase(mat.charAt(0)) + mat.substring(1);
        mainText.setText(modified.replace("_", " "));

        this.values = MarketManager.getInstance().getItem(mat).getPricesM();
        ct.locate("slide1", SlideComponent.class).setValues(values);
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

    public float getChildMultiplier() {
        if (childs == null) {
            return 1;
        } else {
            return childs.get(childsMat.get(0));
        }
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

}
