package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.market.Category;
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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.abs;

public class DisplayComponent extends RectangularComponent {

    // The purpose of this component is to add the logic of the main page.
    // It allows cycling between categories and updates the trending and top movers sections.

    List<Category> categories;

    boolean state;

    protected DisplayComponent(String id, Action clickAction, boolean hidden, Interaction interaction, List<Category> categories) {
        super(id, clickAction, hidden, interaction, 0, 0, 300, 200);
        this.categories = new ArrayList<>(categories);
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {

        GroupComponent cTree = interaction.getComponentTree();

        updateTopMovers(interaction);

        updateTrending(interaction);

        // Categories update:
        cTree.locate("description", TextComponent.class).setText(categories.get(0).getDisplayName());

        // Three rows of categories
        for (int i = 1; i <= 3; i++) {

            Category cat = categories.get(i-1);

            // Six items per category
            for (int j = 1; j <= 6; j++) {

                if (j <= cat.getNumOfItems()) {

                    cTree.locate("asdi" + i + "" + j, ImageComponent.class).setHidden(false);
                    cTree.locate("t" + i + "" + j + "1", TextComponent.class).setHidden(false);
                    cTree.locate("t" + i + "" + j + "2", TextComponent.class).setHidden(false);

                    String value = (cat.getItemOfIndex(j-1).getPrice()) + Config.getInstance().getCurrency();
                    cTree.locate("t" + i + j + "1", TextComponent.class).setText(value);
                    cTree.locate("t" + i + j + "2", TextComponent.class).setText(value);

                    ImageComponent ic = cTree.locate("asdi" + i + "" + j, ImageComponent.class);
                    BufferedImage bi = (BufferedImage) ic.getImage();

                    if(!NUtils.areEqual(NUtils.getImage(cat.getItemOfIndex(j - 1).getMaterial(), 32, 32, false), bi)) {
                        ic.setImage(NUtils.getImage(cat.getItemOfIndex(j - 1).getMaterial(), 32, 32, false));
                    }

                    int finalJ = j;
                    ic.setClickAction((interaction, p, primaryTrigger) -> {

                        interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("TradeScreen1");
                        interaction.getComponentTree().locate("graph1", GraphComponent.class).changeMat(cat.getItemOfIndex(finalJ - 1).getMaterial());
                    });
                } else {
                    cTree.locate("asdi" + i + "" + j, ImageComponent.class).setHidden(true);
                    cTree.locate("t" + i + "" + j + "1", TextComponent.class).setHidden(true);
                    cTree.locate("t" + i + "" + j + "2", TextComponent.class).setHidden(true);
                }
            }
        }
    }

    @Override
    public String getState(Player player, GuiPoint cursor) {

        // Gets updated every minute or if the categories change of order.
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("mm");
        String time = sdf.format(cal.getTime());
        return time + ":" + state;
    }

    @Override
    public Component clone(Interaction interaction) {
        return new DisplayComponent(id, null, false, interaction, categories);
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public void nextPage() { Collections.rotate(categories, -1); state = !state; }

    public void prevPage() { Collections.rotate(categories, 1); state = !state; }

    public void updateTrending(Interaction inter) {
        Item max = null;

        // We get the item with most operations.
        for(Item item : MarketManager.getInstance().getAllItems()) {
            if((max == null || max.getOperations() < item.getOperations()) && item.getOperations() > 10) max = item;
        }

        GroupComponent icTree = inter.getComponentTree();

        // And we print it
        if(max != null) {
            icTree.locate("trend1", GroupComponent.class).setHidden(false);
            ImageComponent ic = icTree.locate("trend", ImageComponent.class);

            BufferedImage bi = (BufferedImage) ic.getImage();
            if(!NUtils.areEqual(NUtils.getImage(max.getMaterial(), 33, 33, false), bi)) {
                ic.setImage(NUtils.getImage(max.getMaterial(), 33, 33, false));
            }

            Item finalMax = max;
            ic.setClickAction((interaction, player, primaryTrigger) -> {
                interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("TradeScreen1");
                interaction.getComponentTree().locate("graph1", GraphComponent.class).changeMat(finalMax.getMaterial());
            });
        } else {
            icTree.locate("trend1", GroupComponent.class).setHidden(true);
        }
    }

    public void updateTopMovers(Interaction inter) {

        GroupComponent icTree = inter.getComponentTree();
        List<Item> items = new ArrayList<>(MarketManager.getInstance().getAllItems());

        // We get up to three items. In each loop we get the item with the most change, and we remove it from the initial list.
        for(int i = 1; i <= 3 ; i++) {

            Item imax = items.get(0);
            for(Item item : items) {

                float variation = NUtils.roundToOne(-100 + 100*(item.getPrice()/item.getPricesM().get(0)));

                if(variation != 0) {
                    if(abs(variation) > abs(-100 + 100*(imax.getPrice()/imax.getPricesM().get(0)))){
                        imax = item;
                    }
                }

            }
            items.remove(imax);

            // Once we do this, the item gets printed:
            ImageComponent ic = icTree.locate("top" + i, ImageComponent.class);

            BufferedImage bi = (BufferedImage) ic.getImage();
            if(!NUtils.areEqual(NUtils.getImage(imax.getMaterial(), 33, 33, false), bi)) {
                ic.setImage(NUtils.getImage(imax.getMaterial(), 33, 33, false));
            }

            Item finalImax = imax;
            icTree.locate("top" + i, ImageComponent.class).setClickAction((interaction, player, primaryTrigger) -> {
                interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("TradeScreen1");
                interaction.getComponentTree().locate("graph1", GraphComponent.class).changeMat(finalImax.getMaterial());
            });

            float fvar = NUtils.roundToOne(-100 + 100*(imax.getPrice()/imax.getPricesM().get(0)));

            if(fvar != 0){
                if(fvar > 0) {
                    icTree.locate("topm" + i, ViewComponent.class).setView("positive");
                } else {
                    icTree.locate("topm" + i, ViewComponent.class).setView("negative");
                }
            } else {
                for(int j = 1; j <= 4; j++) {
                    icTree.locate("topm" + i, ViewComponent.class).setHidden(true);
                }
            }

            for(int j = 1; j <= 4; j++) {
                icTree.locate("topt" + j + i, TextComponent.class).setText(fvar + "%");
            }
        }
    }

}
