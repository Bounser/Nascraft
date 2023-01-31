package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.market.Category;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.tools.ImageManager;
import me.leoko.advancedgui.utils.LayoutExtension;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.events.LayoutLoadEvent;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;

import java.util.Arrays;

public class LayoutModifier implements LayoutExtension {

    @Override
    @EventHandler
    public void onLayoutLoad(LayoutLoadEvent event) {

        if (!event.getLayout().getName().equals("Nascraft")) return;

        GroupComponent cTree = event.getLayout().getTemplateComponentTree();

        // GraphComponent
        DummyComponent dc = cTree.locate("graph123", DummyComponent.class);
        dc.setComponent(new GraphComponent("graph1", null, false,
                event.getLayout().getDefaultInteraction(), 44, 55, 300, 140, Arrays.asList(0.022f, 0.001032f, 0.01345f, 0.03542f, 0.0353f, 0.09f, 0.14f, 0.08f, 0.054f, 0.02f, 0.0231f, 0.03f)));

        // Main page
        updateMainPage(cTree);

        // Arrows
        cTree.locate("ArrowUP").setClickAction((interaction, player, primaryTrigger) -> {
            MarketManager.getInstance().changeCategoryOrder(true);
            updateMainPage(cTree);
        });

        cTree.locate("ArrowDOWN").setClickAction((interaction, player, primaryTrigger) -> {
            MarketManager.getInstance().changeCategoryOrder(false);
            updateMainPage(cTree);
        });

        // Time Span selectors
        for (int i = 1 ; i<=5 ; i++){

            int finalI = i;
            cTree.locate("timesel" + i, RectComponent.class).setClickAction((interaction, player, primaryTrigger) -> {

                interaction.getComponentTree().locate("graph1", GraphComponent.class).setTimeFrame(finalI, Arrays.asList(10.2f, 5.3f, 3.1f, 5f, 10f, 100f, 1100f, 800f));

            });
        }

        // List



        /*
        // Buy

        for (int i : Arrays.asList(1, 16, 64)){
            cTree.locate("buy" + i).setClickAction((interaction, player, primaryTrigger) -> {
                String mat = interaction.getComponentTree().locate("maintext", TextComponent.class).getText().replace(" ", "_");
                ItemsManager.getInstance().getItem(mat).buyItem(i);
            });
        }

        // Sell

        for (int i : Arrays.asList(1, 16, 64)){
            cTree.locate("sell" + i).setClickAction((interaction, player, primaryTrigger) -> {
                String mat = interaction.getComponentTree().locate("maintext", TextComponent.class).getText().replace(" ", "_");
                ItemsManager.getInstance().getItem(mat).sellItem(i);
            });
        }
        */

    }

    public void updateMainPage(GroupComponent cTree) {

        Bukkit.broadcastMessage("actualizando");
        for(int i = 1; i < (MarketManager.getInstance().getNumOfCategories()+1); i++) {

            Category cat = MarketManager.getInstance().getCategoryOfIndex(i - 1);
            int numOfItems = cat.getNumOfItems();

            for (int j = 1; j < (numOfItems + 1); j++) {

                if (j <= 5) {

                    // cTree.locate("t" + i + j, TextComponent.class).setText(String.valueOf(cat.getItemOfIndex(j).getPrice()));
                    ImageComponent ic = cTree.locate("asdi" + i + "" + j, ImageComponent.class);
                    ic.setImage(ImageManager.getInstance().getImage(cat.getItemOfIndex(j - 1).getMaterial(), 32, 32));

                    int finalJ = j;
                    ic.setClickAction((interaction, player, primaryTrigger) -> {

                        interaction.getComponentTree().locate("nbk2fMcG", ViewComponent.class).setView("qrRtaAnd");
                        interaction.getComponentTree().locate("graph1", GraphComponent.class).changeMat(cat.getItemOfIndex(finalJ - 1).getMaterial());

                    });
                }
            }
        }
    }


}
