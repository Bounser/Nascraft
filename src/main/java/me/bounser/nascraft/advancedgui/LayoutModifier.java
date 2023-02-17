package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.market.Category;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.tools.NUtils;
import me.leoko.advancedgui.utils.LayoutExtension;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.events.GuiInteractionBeginEvent;
import me.leoko.advancedgui.utils.events.LayoutLoadEvent;

import org.bukkit.event.EventHandler;

import java.util.*;

public class LayoutModifier implements LayoutExtension {

    @Override
    @EventHandler
    public void onLayoutLoad(LayoutLoadEvent event) {

        if (!event.getLayout().getName().equals("Nascraft")) return;

        GroupComponent cTree = event.getLayout().getTemplateComponentTree();

        updateMainPage(cTree);

        // SlideComponent
        DummyComponent dct = cTree.locate("slide123", DummyComponent.class);
        dct.setComponent(new SlideComponent("slide1", null, false,
                event.getLayout().getDefaultInteraction(), 11, 54, 362, 139, Arrays.asList(1f, 1f)));

        // GraphComponent
        DummyComponent dc = cTree.locate("graph123", DummyComponent.class);
        dc.setComponent(new GraphComponent("graph1", null, false,
                event.getLayout().getDefaultInteraction(), 11, 54, 362, 139, Arrays.asList(1f, 1f)));

        // Arrows
        cTree.locate("ArrowUP").setClickAction((interaction, player, primaryTrigger) -> {
            interaction.getComponentTree().locate("display1", DisplayComponent.class).nextPage();
        });

        cTree.locate("ArrowDOWN").setClickAction((interaction, player, primaryTrigger) -> {
            interaction.getComponentTree().locate("display1", DisplayComponent.class).prevPage();
        });

        // Time Span selectors
        for (int i = 1; i <= 4; i++) {

            int finalI = i;
            cTree.locate("timesel" + i, RectComponent.class).setClickAction((interaction, player, primaryTrigger) -> {

                interaction.getComponentTree().locate("graph1", GraphComponent.class).setTimeFrame(finalI);

            });
        }

        // Back button
        cTree.locate("back").setClickAction((interaction, player, primaryTrigger) -> {
            interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("I4ztUi1d");
            interaction.getComponentTree().locate("display1", DisplayComponent.class).updateTrending(interaction);
            interaction.getComponentTree().locate("display1", DisplayComponent.class).updateTopMovers(interaction);
        });

        // Buy/Sell buttons
        for(int i : Arrays.asList(1,16,64)) {
            cTree.locate("buy" + i).setClickAction((interaction, player, primaryTrigger) -> {
                GraphComponent gc = interaction.getComponentTree().locate("graph1", GraphComponent.class);
                gc.getItem().buyItem(i, player, gc.getMat(), gc.getMultiplier());
                gc.updateButtonPrice();
            });
            cTree.locate("sell" + i).setClickAction((interaction, player, primaryTrigger) -> {
                GraphComponent gc = interaction.getComponentTree().locate("graph1", GraphComponent.class);
                gc.getItem().sellItem(i, player, gc.getMat(), gc.getMultiplier());
                gc.updateButtonPrice();
            });
        }
    }

    public void updateMainPage(GroupComponent cTree) {

        // Three rows.
        for (int i = 1; i <= 3; i++) {

            Category cat = MarketManager.getInstance().getCategoryOfIndex(i - 1);
            int numOfItems = cat.getNumOfItems();

            // Six items per row.
            for (int j = 1; j <= 6; j++) {

                if (j <= numOfItems) {

                    cTree.locate("t" + i + j + "1", TextComponent.class).setText(String.valueOf(cat.getItemOfIndex(j-1).getPrice()));
                    cTree.locate("t" + i + j + "2", TextComponent.class).setText(String.valueOf(cat.getItemOfIndex(j-1).getPrice()));

                    ImageComponent ic = cTree.locate("asdi" + i + "" + j, ImageComponent.class);
                    ic.setImage(NUtils.getImage(cat.getItemOfIndex(j - 1).getMaterial(), 32, 32, false));

                    int finalJ = j;
                    ic.setClickAction((interaction, player, primaryTrigger) -> {

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


    @EventHandler
    public void onInteractionStart(GuiInteractionBeginEvent event) {

        if (!event.getGuiInstance().getLayout().getName().equals("Nascraft")) return;

        // DisplayComponent
        DummyComponent ddc = event.getInteraction().getComponentTree().locate("display123", DummyComponent.class);
        ddc.setComponent(new DisplayComponent("display1", null, false,
                event.getInteraction(), MarketManager.getInstance().getCategories()));

        DisplayComponent dc = event.getInteraction().getComponentTree().locate("display1", DisplayComponent.class);

        dc.updateTrending(event.getInteraction());

        dc.updateTopMovers(event.getInteraction());
    }

}
