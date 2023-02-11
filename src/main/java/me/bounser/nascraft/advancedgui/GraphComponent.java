package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.market.Item;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.ImageManager;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;


public class GraphComponent extends RectangularComponent {

    String mat;
    List<Float> values;
    List<String> childsMat;
    HashMap<String, Float> childs;
    int width, height, yc, xc;

    // Time frames: 1 = 15 min, 2 = 1 day, 3 = 1 Month, 4 = 1 year, 5 = 1 ytd
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

        updateButtons();

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
        return timeFrame + ":" + time + ":" + mat;
    }

    @Override
    public Component clone(Interaction interaction) {
        return new GraphComponent(id, clickAction, hidden, interaction, x, y, width, height, values);
    }

    public void setTimeFrame(int timeFrame) {
        this.timeFrame = timeFrame;
        interaction.getComponentTree().locate("slide1", SlideComponent.class).setTimeFrame(timeFrame);
        switch(timeFrame) {
            case 1:
                setValues(MarketManager.getInstance().getItem(mat).getPricesM());
                break;
            case 2:
                setValues(MarketManager.getInstance().getItem(mat).getPricesH());
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
        for (int i = 0; i < (values.size()-1); i++) {
            x[j] = z*i+1*i + xc;
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

    public static float round(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(Config.getInstance().getDecimalPrecission(), RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public void updateButtons() {

        // Buy

        for (int i : Arrays.asList(1, 16, 64)){
            Item item = MarketManager.getInstance().getItem(mat);
            GroupComponent cTree = interaction.getComponentTree();
            cTree.locate("pb" + i, TextComponent.class).setText(item.getBuyPrice()*i + "");
            cTree.locate("buy" + i).setClickAction((interaction, player, primaryTrigger) -> {

                item.buyItem(i, player);
                updateButtonPrice(cTree.locate("pb" + i, TextComponent.class), item, true);

            });
        }

        // Sell

        for (int i : Arrays.asList(1, 16, 64)){
            Item item = MarketManager.getInstance().getItem(mat);
            GroupComponent cTree = interaction.getComponentTree();
            cTree.locate("ps" + i, TextComponent.class).setText(item.getSellPrice()*i + "");
            cTree.locate("sell" + i).setClickAction((interaction, player, primaryTrigger) -> {

                item.sellItem(i, player);
                updateButtonPrice(cTree.locate("ps" + i, TextComponent.class), item, false);

            });
        }

    }

    public void updateButtonPrice(TextComponent priceText, Item item, boolean buy) {
        if(buy) {
            for (int i : Arrays.asList(1, 16, 64)){
                priceText.setText(item.getBuyPrice()*i + Config.getInstance().getCurrency());
            }
        } else {
            for (int i : Arrays.asList(1, 16, 64)){
                priceText.setText(item.getSellPrice()*i + Config.getInstance().getCurrency());
            }
        }
    }

    public void changeMat(String mat) {

        this.mat = mat;
        this.childs = MarketManager.getInstance().getItem(mat).getChilds();

        ImageComponent ic = interaction.getComponentTree().locate("MainImage", ImageComponent.class);
        ic.setImage(ImageManager.getInstance().getImage(mat, 60, 60, true));
        String modified = Character.toUpperCase(mat.charAt(0)) + mat.substring(1);
        interaction.getComponentTree().locate("maintext", TextComponent.class).setText(modified.replace("_", " "));

        this.values = MarketManager.getInstance().getItem(mat).getPricesM();
        interaction.getComponentTree().locate("slide1", SlideComponent.class).setValues(values);

        if (childs == null) {
            interaction.getComponentTree().locate("childs").setHidden(true);
            interaction.getComponentTree().locate("minichild").setHidden(true);
        } else {
            interaction.getComponentTree().locate("childs").setHidden(false);
            interaction.getComponentTree().locate("minichild").setHidden(false);

            childsMat = new ArrayList<>(childs.keySet());

            updateChilds(childs);

            interaction.getComponentTree().locate("childact").setClickAction((interaction, player, primaryTrigger) -> {
                changeChildsOrder();
                updateChilds(childs);
            });
        }
    }

    public void updateChilds(HashMap<String, Float> childs) {

        for (int i = 1; i <= 8 ; i++) {

            if (childs.keySet().size() >= i) {
                interaction.getComponentTree().locate("child" + i, ImageComponent.class).setImage(ImageManager.getInstance().getImage(childsMat.get(i-1), 32, 32, true));
                int j = 0;
                interaction.getComponentTree().locate("child" + i, ImageComponent.class).setHidden(false);
            } else {
                interaction.getComponentTree().locate("child" + i, ImageComponent.class).setHidden(true);
            }
            interaction.getComponentTree().locate("childback", RectComponent.class).setWidth(5 + 34*childs.keySet().size());
        }

        if(childsMat.get(0).equals(mat)) {
            interaction.getComponentTree().locate("minichild").setHidden(true);
        } else {
            interaction.getComponentTree().locate("minichild").setHidden(false);
            interaction.getComponentTree().locate("childper", ImageComponent.class).setImage(ImageManager.getInstance().getImage(childsMat.get(0), 26, 26, true));
        }
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

}
