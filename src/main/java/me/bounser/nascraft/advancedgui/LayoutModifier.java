package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.advancedgui.component.GraphComponent;
import me.bounser.nascraft.advancedgui.component.SlideComponent;
import me.bounser.nascraft.market.managers.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.NUtils;
import me.bounser.nascraft.market.managers.resources.TimeSpan;
import me.leoko.advancedgui.utils.LayoutExtension;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.events.GuiInteractionBeginEvent;
import me.leoko.advancedgui.utils.events.GuiInteractionExitEvent;
import me.leoko.advancedgui.utils.events.LayoutLoadEvent;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.awt.image.BufferedImage;
import java.util.*;

import static java.lang.Math.abs;

public class LayoutModifier implements LayoutExtension {

    private SlideComponent sc = null;
    private GraphComponent gc = null;

    public HashMap<Player, List<Category>> playerCategory;

    private static LayoutModifier instance;

    public static LayoutModifier getInstance() { return instance == null ? instance = new LayoutModifier() : instance; }

    @Override
    @EventHandler
    public void onLayoutLoad(LayoutLoadEvent event) {

        if (!event.getLayout().getName().equals("Nascraft")) return;

        playerCategory = new HashMap<>();

        GroupComponent cTree = event.getLayout().getTemplateComponentTree();
        GroupComponent TS = cTree.locate("TS1", GroupComponent.class); // Trade Screen

        updateMainPage(cTree, false, null);

        // GraphComponent
        ViewComponent backgroundView = TS.locate("bview1", ViewComponent.class);
        TextComponent mainText = TS.locate("maintext1", TextComponent.class);

        TS.getComponents().remove(backgroundView);
        TS.getComponents().remove(mainText);

        if (gc == null)
            gc = new GraphComponent("graph1", null, false,
                event.getLayout().getDefaultInteraction(), 11, 54, 362, 139, backgroundView, mainText);
        TS.locate("graph123", DummyComponent.class).setComponent(gc);

        // SlideComponent
        GroupComponent slideComponents = TS.locate("SComp1", GroupComponent.class);

        TS.getComponents().remove(slideComponents);

        if (sc == null) {
            RectComponent bar = slideComponents.locate("bar", RectComponent.class);
            RectComponent translucid = slideComponents.locate("translucid", RectComponent.class);
            ImageComponent up = slideComponents.locate("upgreen", ImageComponent.class);
            ImageComponent down = slideComponents.locate("downred", ImageComponent.class);
            TextComponent textslide = slideComponents.locate("textslide1", TextComponent.class);
            TextComponent timetext = slideComponents.locate("time1", TextComponent.class);
            TextComponent perslide = slideComponents.locate("perslide1", TextComponent.class);
            RectComponent divisor = slideComponents.locate("TIAYadch", RectComponent.class);

            sc = new SlideComponent("slide1", null, false,
                    event.getLayout().getDefaultInteraction(), 11, 60, 362, 121, bar, translucid, up, down, textslide, timetext, perslide, divisor);
        }

        TS.locate("slide123", DummyComponent.class).setComponent(sc);

        // Arrows
        cTree.locate("ArrowUP").setClickAction((interaction, player, primaryTrigger) -> {
            List<Category> categories = new ArrayList<>(playerCategory.get(player));
            Collections.rotate(categories, -1);
            playerCategory.put(player, new ArrayList<>(categories));
            updateMainPage(interaction.getComponentTree(), false, player);
        });

        cTree.locate("ArrowDOWN").setClickAction((interaction, player, primaryTrigger) -> {
            List<Category> categories = new ArrayList<>(playerCategory.get(player));
            Collections.rotate(categories, 1);
            playerCategory.put(player, new ArrayList<>(categories));
            updateMainPage(interaction.getComponentTree(), false, player);
        });

        // Time Span selectors
        for (TimeSpan timeSpan : TimeSpan.values()) {
            cTree.locate("timesel" + timeSpan.toString(), RectComponent.class).setClickAction((interaction, player, primaryTrigger) -> {
                interaction.getComponentTree().locate("graph1", GraphComponent.class).setTimeFrame(timeSpan);
            });
        }

        // Back button
        cTree.locate("back").setClickAction((interaction, player, primaryTrigger) -> {
            interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("I4ztUi1d");
            updateMainPage(interaction.getComponentTree(), true, player);
        });

        // Buy/Sell buttons
        for (int j : Arrays.asList(1,16,64)) {
            cTree.locate("buy" + j).setClickAction((interaction, player, primaryTrigger) -> {
                GraphComponent g = interaction.getComponentTree().locate("graph1", GraphComponent.class);
                g.getItem().buyItem(j, player, g.getMat(), g.getMultiplier());
                g.updateButtonPrice();
            });
            cTree.locate("sell" + j).setClickAction((interaction, player, primaryTrigger) -> {
                GraphComponent g = interaction.getComponentTree().locate("graph1", GraphComponent.class);
                g.getItem().sellItem(j, player, g.getMat(), g.getMultiplier());
                g.updateButtonPrice();
            });
        }

        // Notifications
        if (!Config.getInstance().getNotificationsEnabled()) {
            cTree.locate("a8lw0trw").setHidden(true);
            cTree.locate("RDKH4p2v").setHidden(true);
        }

        setLang(cTree);
    }

    @EventHandler
    public void onInteractionStart(GuiInteractionBeginEvent event) {

        if (!event.getGuiInstance().getLayout().getName().equals("Nascraft")) return;

        playerCategory.put(event.getPlayer(), MarketManager.getInstance().getCategories());

        updateMainPage(event.getInteraction().getComponentTree(), true, event.getPlayer());
    }

    @EventHandler
    public void onInteractionEnd(GuiInteractionExitEvent event) {

        if (!event.getGuiInstance().getLayout().getName().equals("Nascraft")) return;

        playerCategory.remove(event.getPlayer());
    }

    public void updateMainPage(GroupComponent cTree, boolean allSections, Player player) {

        if (allSections) {
            updateTrending(cTree);

            updateTopMovers(cTree);
        }

        List<Category> categories;
        if (player == null) {
            categories = MarketManager.getInstance().getCategories();
        } else {
            categories = playerCategory.get(player);
        }

        cTree.locate("description", TextComponent.class).setText(categories.get(0).getDisplayName());

        // Three rows.
        for (int i = 1; i <= 3; i++) {

            Category cat = categories.get(i - 1);
            int numOfItems = cat.getNumOfItems();

            // Six items per row.
            for (int j = 1; j <= 6; j++) {

                if (j <= numOfItems) {

                    cTree.locate("t" + i + j + "1", TextComponent.class).setText(cat.getItemOfIndex(j-1).getPrice() + Config.getInstance().getCurrency());
                    cTree.locate("t" + i + j + "2", TextComponent.class).setText(cat.getItemOfIndex(j-1).getPrice() + Config.getInstance().getCurrency());

                    ImageComponent ic = cTree.locate("asdi" + i + "" + j, ImageComponent.class);
                    ic.setImage(NUtils.getImage(cat.getItemOfIndex(j - 1).getMaterial(), 32, 32, false));

                    int finalJ = j;
                    ic.setClickAction((interaction, p, primaryTrigger) -> {

                        interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("TS1");
                        interaction.getComponentTree().locate("graph1", GraphComponent.class).changeMat(cat.getItemOfIndex(finalJ - 1).getMaterial());

                    });
                }
                cTree.locate("asdi" + i + "" + j, ImageComponent.class).setHidden(!(j <= numOfItems));
                cTree.locate("t" + i + "" + j + "1", TextComponent.class).setHidden(!(j <= numOfItems));
                cTree.locate("t" + i + "" + j + "2", TextComponent.class).setHidden(!(j <= numOfItems));
            }
        }
    }

    public void updateTrending(GroupComponent icTree) {
        Item max = null;

        for (Item item : MarketManager.getInstance().getAllItems()) {
            if ((max == null || max.getOperations() < item.getOperations()) && item.getOperations() > 10) max = item;
        }

        if (max != null) {
            icTree.locate("trend1", GroupComponent.class).setHidden(false);
            ImageComponent ic = icTree.locate("trend", ImageComponent.class);

            BufferedImage bi = (BufferedImage) ic.getImage();
            if (!NUtils.areEqual(NUtils.getImage(max.getMaterial(), 33, 33, false), bi)) {
                ic.setImage(NUtils.getImage(max.getMaterial(), 33, 33, false));
            }

            Item finalMax = max;
            ic.setClickAction((interaction, player, primaryTrigger) -> {
                interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("TS1");
                interaction.getComponentTree().locate("graph1", GraphComponent.class).changeMat(finalMax.getMaterial());
            });
        } else {
            icTree.locate("trend1", GroupComponent.class).setHidden(true);
        }
    }

    public void updateTopMovers(GroupComponent icTree) {

        List<Item> items = new ArrayList<>(MarketManager.getInstance().getAllItems());

        // We get up to three items. In each loop we get the item with the biggest change, and we remove it from the initial list.
        for (int i = 1; i <= 3 ; i++) {

            Item imax = items.get(0);
            for (Item item : items) {

                float variation = NUtils.roundToOne(-100 + 100*(item.getPrice()/item.getPrices(TimeSpan.MINUTE).get(0)));

                if (variation != 0) {
                    if (abs(variation) > abs(-100 + 100*(imax.getPrice()/imax.getPrices(TimeSpan.MINUTE).get(0)))){
                        imax = item;
                    }
                }
            }
            items.remove(imax);

            ImageComponent ic = icTree.locate("top" + i, ImageComponent.class);

            BufferedImage bi = (BufferedImage) ic.getImage();
            if (!NUtils.areEqual(NUtils.getImage(imax.getMaterial(), 33, 33, false), bi)) {
                ic.setImage(NUtils.getImage(imax.getMaterial(), 33, 33, false));
            }

            Item finalImax = imax;
            icTree.locate("top" + i, ImageComponent.class).setClickAction((interaction, player, primaryTrigger) -> {
                interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("TS1");
                interaction.getComponentTree().locate("graph1", GraphComponent.class).changeMat(finalImax.getMaterial());
            });

            float fvar = NUtils.roundToOne(-100 + 100*(imax.getPrice()/imax.getPrices(TimeSpan.MINUTE).get(0)));

            if (fvar != 0){
                if (fvar > 0) {
                    icTree.locate("topm" + i, ViewComponent.class).setView("positive");
                } else {
                    icTree.locate("topm" + i, ViewComponent.class).setView("negative");
                }
            } else {
                for (int j = 1; j <= 4; j++) {
                    icTree.locate("topm" + i, ViewComponent.class).setHidden(true);
                }
            }

            for (int j = 1; j <= 4; j++) {
                icTree.locate("topt" + j + i, TextComponent.class).setText(String.valueOf(fvar).replace("-", "") + "%");
            }
        }
    }

    public void setLang(GroupComponent cTree) {

        List<String> lang = Config.getInstance().getLang();
        // Title
        for (int i = 1; i <= 4 ; i++) { cTree.locate("title" + i, TextComponent.class).setText(lang.get(0)); }
        // Top Movers
        cTree.locate("w1omKKFS", TextComponent.class).setText(lang.get(1));
        // Sub top movers
        cTree.locate("ypqwCPVb", TextComponent.class).setText(lang.get(2));
        // Buy
        cTree.locate("8mbDiOVM", TextComponent.class).setText(lang.get(3));
        cTree.locate("rK8xOEwj", TextComponent.class).setText(lang.get(3).toUpperCase());
        // Sell
        cTree.locate("jEXxBLF2", TextComponent.class).setText(lang.get(4));
        cTree.locate("ZakfQVQ0", TextComponent.class).setText(lang.get(4).toUpperCase());
        // Price text
        cTree.locate("EGgOeYza", TextComponent.class).setText(lang.get(5));
        cTree.locate("EfJrz4vo", TextComponent.class).setText(lang.get(5));
        // Amount
        cTree.locate("pfFe6Wjt", TextComponent.class).setText(lang.get(6));
        cTree.locate("ityZyfNt", TextComponent.class).setText(lang.get(6));
        // Trend
        cTree.locate("pfV4FIy1", TextComponent.class).setText(lang.get(7));
    }

}
