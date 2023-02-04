package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.market.Category;
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
import java.util.Collections;
import java.util.List;

public class DisplayComponent extends RectangularComponent {

    List<Category> categories;

    boolean state;

    protected DisplayComponent(String id, Action clickAction, boolean hidden, Interaction interaction, List<Category> categories) {
        super(id, clickAction, hidden, interaction, 0, 0, 5, 5);
        this.categories = categories;
    }

    @Override
    public void apply(Graphics graphic, Player player, GuiPoint cursor) {

        GroupComponent cTree = interaction.getComponentTree();

        cTree.locate("description", TextComponent.class).setText(categories.get(0).getDisplayName());

        for (int i = 1; i <= 3; i++) {

            Category cat = categories.get(i-1);

            float change = cat.getCategoryChange();
            TextComponent textc = cTree.locate("change" + i, TextComponent.class);
            textc.setText(change + "%");

            if (change < 0) {
                textc.setColor(new Color(240, 0, 0));
            } else if(change > 0) {
                textc.setColor(new Color(0, 240, 0));
            } else {
                textc.setColor(new Color(240, 240, 240));
            }

            for (int j = 1; j <= 5; j++) {

                if (j <= cat.getNumOfItems()) {

                    cTree.locate("asdi" + i + "" + j, ImageComponent.class).setHidden(false);
                    cTree.locate("t" + i + "" + j + "1", TextComponent.class).setHidden(false);
                    cTree.locate("t" + i + "" + j + "2", TextComponent.class).setHidden(false);

                    cTree.locate("t" + i + j + "1", TextComponent.class).setText(String.valueOf(cat.getItemOfIndex(j-1).getPrice()));
                    cTree.locate("t" + i + j + "2", TextComponent.class).setText(String.valueOf(cat.getItemOfIndex(j-1).getPrice()));
                    ImageComponent ic = cTree.locate("asdi" + i + "" + j, ImageComponent.class);
                    ic.setImage(ImageManager.getInstance().getImage(cat.getItemOfIndex(j - 1).getMaterial(), 32, 32, false));

                    int finalJ = j;
                    ic.setClickAction((interaction, p, primaryTrigger) -> {

                        interaction.getComponentTree().locate("nbk2fMcG", ViewComponent.class).setView("qrRtaAnd");
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
        return state + "";
    }

    @Override
    public Component clone(Interaction interaction) {
        return new DisplayComponent(id, null, false, interaction, categories);
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public void nextPage() {
        Collections.rotate(categories, -1);
        state = !state;
    }

    public void prevPage() {
        Collections.rotate(categories, 1);
        state = !state;
    }

}
