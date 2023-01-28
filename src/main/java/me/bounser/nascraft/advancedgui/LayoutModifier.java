package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.market.ItemsManager;
import me.bounser.nascraft.tools.Config;
import me.leoko.advancedgui.utils.LayoutExtension;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.events.LayoutLoadEvent;

import org.bukkit.event.EventHandler;

import java.util.Arrays;

public class LayoutModifier implements LayoutExtension {

    @Override
    @EventHandler
    public void onLayoutLoad(LayoutLoadEvent event) {

        if (!event.getLayout().getName().equals("Nascraft")) return;

        GroupComponent cTree = event.getLayout().getTemplateComponentTree();

        // Lang changes

        // Icons

        cTree.locate("icon1", DummyComponent.class).setComponent(
                new RemoteImageComponent("icon1", null, false, event.getLayout().getDefaultInteraction(), "https://mc.nerothe.com/img/1.19.2/" + Config.getInstance().getIcon(1) + ".png", null, 54, 112, 61, 61, false));
        cTree.locate("icon1", DummyComponent.class).setComponent(
                new RemoteImageComponent("icon2", null, false, event.getLayout().getDefaultInteraction(), "https://mc.nerothe.com/img/1.19.2/" + Config.getInstance().getIcon(2) + ".png", null, 159, 72, 61, 61, false));
        cTree.locate("icon1", DummyComponent.class).setComponent(
                new RemoteImageComponent("icon3", null, false, event.getLayout().getDefaultInteraction(), "https://mc.nerothe.com/img/1.19.2/" + Config.getInstance().getIcon(3) + ".png", null, 264, 112, 61, 61, false));

        // GraphComponent
        DummyComponent dc = cTree.locate("graph123", DummyComponent.class);
        dc.setComponent(new GraphComponent("graph1", null, false,
                event.getLayout().getDefaultInteraction(), 44, 55, 300, 140, Arrays.asList(564.6f, 20f, 1100f, 800f, 1800f, 1700f, 1000f, 1500f, 400f)));

        // Time Span selectors

        for (int i = 1 ; i<=5 ; i++){

            int finalI = i;
            cTree.locate("timesel" + i, RectComponent.class).setClickAction((interaction, player, primaryTrigger) -> {

                interaction.getComponentTree().locate("graph1", GraphComponent.class).setTimeFrame(finalI, Arrays.asList(10.2f, 5.3f, 3.1f, 5f, 10f, 100f, 1100f, 800f));

            });
        }

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

    }
}
