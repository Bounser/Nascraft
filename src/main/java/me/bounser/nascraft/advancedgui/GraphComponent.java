package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.market.Item;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.NUtils;
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

    Item item;
    List<Float> values;
    List<String> childsMat;
    HashMap<String, Float> childs;
    int width, height, yc, xc;

    // Time frames: 1 = 30 min, 2 = 1 day, 3 = 1 Month, 4 = 1 year
    private int timeFrame;

    public GraphComponent(String id, Action clickAction, boolean hidden, Interaction interaction, int x, int y, int width, int height, List<Float> values) {
        super(id, clickAction, hidden, interaction, x, y, width, height);

        this.width = width-1;
        this.height = height;
        this.xc = x;
        this.yc = y;
        this.values = values;

        timeFrame = 1;
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {
        updateButtonPrice();

        graphic.setColor(new Color(0, 0, 0));

        graphic.fillPolygon(getXPoints(true), getYPoints(0, true), getXPoints(true).length);

        graphic.setColor(setupBackGround());

        graphic.drawPolyline(getXPoints(false), getYPoints(0, false), getXPoints(false).length);
    }

    @Override
    public String getState(Player player, GuiPoint cursor) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("mm");
        String time = sdf.format(cal.getTime());
        return timeFrame + ":" + time + ":" + item.getMaterial();
    }

    @Override
    public Component clone(Interaction interaction) {
        return new GraphComponent(id, clickAction, hidden, interaction, x, y, width, height, values);
    }

    public void setTimeFrame(int timeFrame) {

        this.timeFrame = timeFrame;
        interaction.getComponentTree().locate("slide1", SlideComponent.class).setTimeFrame(timeFrame);
        interaction.getComponentTree().locate("timeview", ViewComponent.class).setView("times" + timeFrame);

        switch(timeFrame) {
            case 1:
                setValues(item.getPricesM());
                break;
            case 2:
                setValues(item.getPricesH());
                break;
            case 3:
                setValues(item.getPricesMM());
                break;
            case 4:
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

        float a = 0;
        if(timeFrame != 3) a = 0.5f;
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

        if(values.size() > 1) {
            float first = values.get(0);
            float last = values.get(values.size() - 1);

            ViewComponent backg = interaction.getComponentTree().locate("backgroundview", ViewComponent.class);

            if (Float.compare(first, last) < 0) {
                backg.setView("bull123");
                return new Color(0,200,20);

            } else if (Float.compare(first, last) > 0){
                backg.setView("bear123");
                return new Color(200,10,20);

            } else {
                backg.setView("flat123");
                return new Color(250,250,250);
            }
        }
        return new Color(250,250,250);
    }

    public void updateButtonPrice() {

        if(childs == null) {
            for(int i : Arrays.asList(1, 16, 64)) {
                interaction.getComponentTree().locate("buyprice" + i, TextComponent.class).setText(getItem().getBuyPrice()*i + Config.getInstance().getCurrency());
                interaction.getComponentTree().locate("sellprice" + i, TextComponent.class).setText(getItem().getSellPrice()*i + Config.getInstance().getCurrency());
            }
        } else {
            for(int i : Arrays.asList(1, 16, 64)) {
                interaction.getComponentTree().locate("buyprice" + i, TextComponent.class).setText(NUtils.round(getItem().getBuyPrice()*i*childs.get(childsMat.get(0))) + Config.getInstance().getCurrency());
                interaction.getComponentTree().locate("sellprice" + i, TextComponent.class).setText(NUtils.round(getItem().getSellPrice()*i*childs.get(childsMat.get(0))) + Config.getInstance().getCurrency());
            }
        }
    }

    public void changeMat(String mat) {

        item = MarketManager.getInstance().getItem(mat);
        this.childs = item.getChilds();

        ImageComponent ic = interaction.getComponentTree().locate("MainImage", ImageComponent.class);
        ic.setImage(NUtils.getImage(mat, 60, 60, true));
        String modified = Character.toUpperCase(mat.charAt(0)) + mat.substring(1);
        interaction.getComponentTree().locate("maintext", TextComponent.class).setText(modified.replace("_", " "));

        this.values = MarketManager.getInstance().getItem(mat).getPricesM();
        interaction.getComponentTree().locate("slide1", SlideComponent.class).setValues(values);

        if (childs == null) {
            interaction.getComponentTree().locate("childs").setHidden(true);
            interaction.getComponentTree().locate("minichild").setHidden(true);
            childsMat = new ArrayList<>();
            childsMat.add(0, mat);
        } else {
            interaction.getComponentTree().locate("childs").setHidden(false);
            interaction.getComponentTree().locate("minichild").setHidden(false);

            childsMat = new ArrayList<>(childs.keySet());

            while(!childsMat.get(0).equals(mat)) {
                Collections.rotate(childsMat, 1);
            }

            updateChilds(childs);

            interaction.getComponentTree().locate("childact").setClickAction((interaction, player, primaryTrigger) -> {
                changeChildsOrder();
                updateChilds(childs);
            });
        }

        setTimeFrame(1);
    }

    public void updateChilds(HashMap<String, Float> childs) {

        for (int i = 1; i <= 8 ; i++) {

            if (childs.keySet().size() >= i) {
                interaction.getComponentTree().locate("child" + i, ImageComponent.class).setImage(NUtils.getImage(childsMat.get(i-1), 32, 32, true));
                interaction.getComponentTree().locate("child" + i, ImageComponent.class).setHidden(false);
            } else {
                interaction.getComponentTree().locate("child" + i, ImageComponent.class).setHidden(true);
            }
            interaction.getComponentTree().locate("childback", RectComponent.class).setWidth(10 + 33*childs.keySet().size());
        }

        if(childsMat.get(0).equals(item.getMaterial())) {
            interaction.getComponentTree().locate("minichild").setHidden(true);
        } else {
            interaction.getComponentTree().locate("minichild").setHidden(false);
            interaction.getComponentTree().locate("childper", ImageComponent.class).setImage(NUtils.getImage(childsMat.get(0), 26, 26, true));
        }

        updateButtonPrice();
    }

    public void changeChildsOrder() {
        Collections.rotate(childsMat, -1);
    }

    public float getChildMultiplier() {
        if(childs == null) {
            return 1;
        } else {
            return childs.get(childsMat.get(0));
        }
    }

    public Item getItem() {
        return item;
    }

    public String getMat() {
        if(childsMat == null) return item.getMaterial();
        else return childsMat.get(0);
    }

    public float getMultiplier() {
        if(childs != null) return childs.get(childsMat.get(0));
        else return 1;
    }

}
