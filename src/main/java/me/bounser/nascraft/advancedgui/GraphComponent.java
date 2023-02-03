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
import org.bukkit.ChatColor;
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
    int width, height, yc, xc;

    // Time frames: 1 = 15 min, 2 = 1 day, 3 = 1 Month, 4 = 1 year, 5 = 1 ytd
    private int timeFrame;

    public GraphComponent(String id, Action clickAction, boolean hidden, Interaction interaction, int x, int y, int width, int height, List<Float> values) {

        super(id, clickAction, hidden, interaction, x, y, width, height);

        this.width = width;
        this.height = height;
        this.xc = x;
        this.yc = y;
        this.values = values;

        timeFrame = 1;
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {

        updateButtons();

        // setScaleReferences();

        graphic.setColor(new Color(0, 0, 0));

        graphic.fillPolygon(getXPoints(true), getYPoints(0, true), getXPoints(true).length);

        graphic.setColor(setupBackGround());

        graphic.drawPolyline(getXPoints(false), getYPoints(0, false), getXPoints(false).length);

        interaction.getComponentTree().locate("timespan1", ViewComponent.class).setView("opt" + timeFrame);

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
            x[j] = z*i + xc;
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

            if (Float.compare(first, last) < 0) {
                this.interaction.getComponentTree().locate("bear123").setHidden(true);
                this.interaction.getComponentTree().locate("flat123").setHidden(true);
                return new Color(0,200,20);

            } else if (Float.compare(first, last) > 0){
                this.interaction.getComponentTree().locate("bear123").setHidden(false);
                return new Color(200,10,20);

            } else {
                this.interaction.getComponentTree().locate("bear123").setHidden(true);
                this.interaction.getComponentTree().locate("flat123").setHidden(false);
                return new Color(250,250,250);
            }
        }
        return new Color(250,250,250);
    }

    public void setScaleReferences() {

        float maxValue = Collections.max(values);
        float minValue = Collections.min(values);

        if(maxValue == minValue){
            for (int i = 1; i<=4 ; i++) {
                interaction.getComponentTree().locate("scale" + i, TextComponent.class).setHidden(true);
                interaction.getComponentTree().locate("backscale" + i, RectComponent.class).setHidden(true);
            }
            interaction.getComponentTree().locate("scale0", TextComponent.class).setHidden(true);
            interaction.getComponentTree().locate("last", RectComponent.class).setHidden(true);
            interaction.getComponentTree().locate("12backscale0", RectComponent.class).setHidden(true);
            return;
        }

        int i = 1;

        for(float val : Arrays.asList(maxValue, minValue, round((maxValue-minValue)*2/3 + minValue), round((maxValue-minValue)*1/3 + minValue))) {

            float reference;
            if((maxValue-minValue) > 10) {
                int firstDigits = getFirstDigits(val);

                int numDigits = (int) Math.floor(Math.log10(val)) + (int) Math.floor(Math.log10(1 - val));
                if (numDigits >= 0) numDigits -= 1;

                reference = (float) firstDigits * (float) (Math.pow(10, numDigits));
            } else {
                reference = round(val);
            }
            int pos = (int) (Math.round((height*0.8 - height*0.8 * (reference - minValue) / (maxValue - minValue))) + yc + Math.round(height*0.05));

            interaction.getComponentTree().locate("scale" + i, TextComponent.class).setText(reference + "");
            interaction.getComponentTree().locate("scale" + i, TextComponent.class).setY(pos+4);
            interaction.getComponentTree().locate("backscale" + i, RectComponent.class).setY(pos-7);

            interaction.getComponentTree().locate("ref" + i, RectComponent.class).setY(pos);
            i++;
        }

        // 0
        float escalated = (int) ((Math.round((height*0.8 - height*0.8 * (-minValue) / (maxValue - minValue))) + yc) + Math.round(height*0.05));

        TextComponent tc = interaction.getComponentTree().locate("scale0", TextComponent.class);
        RectComponent rc = interaction.getComponentTree().locate("last", RectComponent.class);
        RectComponent bc = interaction.getComponentTree().locate("12backscale0", RectComponent.class);

        if (escalated < height + yc) {
            tc.setY((int) escalated +4);
            tc.setHidden(false);

            rc.setY((int) escalated);
            rc.setHidden(false);

            bc.setY((int) escalated-5);
            bc.setHidden(false);
        } else {
            tc.setHidden(true);
            rc.setHidden(true);
            bc.setHidden(true);
        }
    }

    public int getFirstDigits(float num) {

        // If the number is greater (or equal) than 10 (Has 2 digits or more) we simply get the 2 first digits.
        if(num >= 10){
            return Integer.parseInt(String.valueOf(num).substring(0,2));
            // In case the number is smaller than 10, we delete the "." and ignore all the 0 before the first digit
        } else {
            String numString = String.valueOf(num);
            numString = numString.replace(".", "");
            int i = 0;
            while (i < numString.length() && numString.charAt(i) == '0') {
                i++;
            }
            numString = numString.substring(i);
            int result = Integer.parseInt(numString.substring(0, 2));
            if(result < 10) result *= 10;
            return result;
        }
    }

    public static float round(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(Config.getInstance().getDecimalPrecission(), RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public void updateButtons() {

        for (int i : Arrays.asList(1, 16, 64)){
            Item item = MarketManager.getInstance().getItem(mat);
            GroupComponent cTree = interaction.getComponentTree();
            cTree.locate("pb" + i, TextComponent.class).setText(item.getBuyPrice()*i + Config.getInstance().getCurrency());
            cTree.locate("buy" + i).setClickAction((interaction, player, primaryTrigger) -> {
                item.buyItem(i);
                updateButtonPrice(cTree.locate("pb" + i, TextComponent.class), item, true);
                player.sendMessage(ChatColor.GRAY + "You just bought " + ChatColor.AQUA + i + ChatColor.GRAY + " of " + ChatColor.AQUA + mat + ChatColor.GRAY + " worth " + ChatColor.GOLD + item.getBuyPrice()*i);
            });
        }

        // Sell

        for (int i : Arrays.asList(1, 16, 64)){
            Item item = MarketManager.getInstance().getItem(mat);
            GroupComponent cTree = interaction.getComponentTree();
            cTree.locate("ps" + i, TextComponent.class).setText(item.getSellPrice()*i + Config.getInstance().getCurrency());
            cTree.locate("sell" + i).setClickAction((interaction, player, primaryTrigger) -> {
                item.sellItem(i);
                updateButtonPrice(cTree.locate("ps" + i, TextComponent.class), item, false);
                player.sendMessage(ChatColor.GRAY + "You just sold " + ChatColor.AQUA + i + ChatColor.GRAY + " of " + ChatColor.AQUA + mat + ChatColor.GRAY + " worth " + ChatColor.GOLD + item.getBuyPrice()*i);
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

        ImageComponent ic = interaction.getComponentTree().locate("MainImage", ImageComponent.class);
        ic.setImage(ImageManager.getInstance().getImage(mat, 60, 60, true));
        String modified = Character.toUpperCase(mat.charAt(0)) + mat.substring(1);
        interaction.getComponentTree().locate("maintext", TextComponent.class).setText(modified.replace("_", " "));

        this.values = MarketManager.getInstance().getItem(mat).getPricesM();
        interaction.getComponentTree().locate("slide1", SlideComponent.class).setValues(values);

        HashMap<String, Float> childs = MarketManager.getInstance().getItem(mat).getChilds();

        if(childs == null) {
            interaction.getComponentTree().locate("childs").setHidden(true);
        } else {
            childsMat = new ArrayList<>(childs.keySet());

            interaction.getComponentTree().locate("childact").setClickAction((interaction, player, primaryTrigger) -> {

                changeChildsOrder();

                for(int i = 1; i < 8 ; i++) {

                    interaction.getComponentTree().locate("child" + i, ImageComponent.class).setImage(ImageManager.getInstance().getImage(childsMat.get(i-1), 32, 32, true));
                    int j = 0;
                    for(float v : values) {
                        values.set(j, v*childs.get(childsMat.get(i-1)));
                    }
                }
            });
        }
    }

    public void changeChildsOrder() {
        Collections.rotate(childsMat, -1);
    }

}
