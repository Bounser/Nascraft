package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.advancedgui.components.GraphComponent;
import me.bounser.nascraft.advancedgui.components.SlideComponent;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.market.RoundUtils;
import me.leoko.advancedgui.utils.LayoutExtension;
import me.leoko.advancedgui.utils.actions.Action;
import me.leoko.advancedgui.utils.components.*;
import me.leoko.advancedgui.utils.events.GuiInteractionBeginEvent;
import me.leoko.advancedgui.utils.events.GuiInteractionExitEvent;
import me.leoko.advancedgui.utils.events.LayoutLoadEvent;

import me.leoko.advancedgui.utils.interactions.Interaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.awt.image.BufferedImage;
import java.util.*;

import static java.lang.Math.abs;

public class LayoutModifier implements LayoutExtension {

    private SlideComponent sc = null;
    private GraphComponent gc = null;

    public HashMap<Player, Category> playerCategory;
    public HashMap<Player, Integer> playerOffset;

    private static LayoutModifier instance;

    public static LayoutModifier getInstance() { return instance == null ? instance = new LayoutModifier() : instance; }

    @Override
    @EventHandler
    public void onLayoutLoad(LayoutLoadEvent event) {

        if (!event.getLayout().getName().equals("Nascraft")) return;

        playerCategory = new HashMap<>();
        playerOffset = new HashMap<>();

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
            changeCategory(player, interaction, -1);
        });

        cTree.locate("ArrowDOWN").setClickAction((interaction, player, primaryTrigger) -> {
            changeCategory(player, interaction, 1);
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
                g.getItem().buyItem(j, player, g.getMultiplier());
                g.updateButtonPrice();
            });
            cTree.locate("sell" + j).setClickAction((interaction, player, primaryTrigger) -> {
                GraphComponent g = interaction.getComponentTree().locate("graph1", GraphComponent.class);
                g.getItem().sellItem(j, player, g.getMultiplier());
                g.updateButtonPrice();
            });
        }
        setLang(cTree);
    }

    public void changeCategory(Player player, Interaction interaction, int rotateDirection) {

        if (playerCategory.get(player) == null) {
            playerCategory.put(player, MarketManager.getInstance().getCategories().get(0));
        } else {
            Category category = playerCategory.get(player);
            int indexOfCategory = MarketManager.getInstance().getCategories().indexOf(category);
            switch (rotateDirection) {

                case -1:

                    if(indexOfCategory > 0) {
                        playerCategory.put(player, MarketManager.getInstance().getCategories().get(indexOfCategory-1));
                    } else {
                        playerCategory.put(player, MarketManager.getInstance().getCategories().get(MarketManager.getInstance().getCategories().size()-1));
                    }
                    break;
                case +1:

                    if(indexOfCategory+2 >= MarketManager.getInstance().getCategories().size()) {
                        playerCategory.put(player, MarketManager.getInstance().getCategories().get(indexOfCategory+1));
                    } else {
                        playerCategory.put(player, MarketManager.getInstance().getCategories().get(0));
                    }
                    break;
            }
        }
        playerOffset.put(player, 0);
        updateMainPage(interaction.getComponentTree(), false, player);
    }

    @EventHandler
    public void onInteractionStart(GuiInteractionBeginEvent event) {

        if (!event.getGuiInstance().getLayout().getName().equals("Nascraft")) return;

        playerCategory.put(event.getPlayer(), MarketManager.getInstance().getCategories().get(0));

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

        List<Category> categories = new ArrayList<>();

        if (player == null) {
            categories = MarketManager.getInstance().getCategories();
        } else {
            List<Category> defaultCategories = MarketManager.getInstance().getCategories();
            Category category = playerCategory.get(player);
            int indexOfCategory = defaultCategories.indexOf(category);

            if(indexOfCategory + 2 <= defaultCategories.size()) {

                categories.add(category);
                categories.add(defaultCategories.get(indexOfCategory+1));
                categories.add(defaultCategories.get(indexOfCategory+2));

            } else if (indexOfCategory == defaultCategories.size() - 1) {

                categories.add(category);
                categories.add(defaultCategories.get(0));
                categories.add(defaultCategories.get(1));

            } else if (indexOfCategory == defaultCategories.size() - 2) {

                categories.add(category);
                categories.add(defaultCategories.get(indexOfCategory+1));
                categories.add(defaultCategories.get(0));
            }
        }

        cTree.locate("description", TextComponent.class).setText(categories.get(0).getDisplayName());

        for (int i = 1; i <= 3; i++) {
            Category category = categories.get(i - 1);

            if (i == 1 && category.getItems().size() > 6) {

                ImageComponent arrowRight = cTree.locate("ArrowRight", ImageComponent.class);
                ImageComponent arrowLeft = cTree.locate("ArrowLeft", ImageComponent.class);

                if (playerOffset.get(player) == null || playerOffset.get(player) == 0) {
                    arrowRight.setHidden(false);
                    arrowLeft.setHidden(true);
                } else {
                    if (playerOffset.get(player) >= (category.getItems().size())-6) {
                        arrowRight.setHidden(true);
                        arrowLeft.setHidden(false);
                    } else {
                        Bukkit.broadcastMessage("Not Hidden");
                        arrowRight.setHidden(false);
                        arrowLeft.setHidden(false);
                    }
                }
                arrowRight.setClickAction((interaction, player1, primaryTrigger) -> {

                    playerOffset.put(player1, playerOffset.get(player1)+1);
                    renderRow(cTree, category, playerOffset.get(player1), 1);

                });
                arrowLeft.setClickAction((interaction, player1, primaryTrigger) -> {

                    playerOffset.put(player1, playerOffset.get(player1)-1);
                    renderRow(cTree, category, playerOffset.get(player1), 1);

                });
            } else {
                renderRow(cTree, category, 0, i);
            }
        }
    }

    public void renderRow(GroupComponent componentTree, Category category, int offset, int position) {

        int numberOfItems = category.getItems().size();

        // Six items per row.
        for (int j = 1; j <= 6; j++) {

            if (j <= numberOfItems) {

                componentTree.locate("t" + position + j + "1", TextComponent.class).setText(category.getItemOfIndex(j-1).getPrice().getValue() + Config.getInstance().getCurrency());
                componentTree.locate("t" + position + j + "2", TextComponent.class).setText(category.getItemOfIndex(j-1).getPrice().getValue() + Config.getInstance().getCurrency());

                ImageComponent ic = componentTree.locate("asdi" + position + "" + j, ImageComponent.class);
                ic.setImage(Images.getInstance().getImage(category.getItemOfIndex(j - 1).getMaterial(), 32, 32, false));

                int finalJ = j;
                ic.setClickAction((interaction, p, primaryTrigger) -> {

                    interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("TS1");
                    interaction.getComponentTree().locate("graph1", GraphComponent.class).changeMat(category.getItemOfIndex(finalJ - 1).getMaterial());

                });
            }
            componentTree.locate("asdi" + position + j, ImageComponent.class).setHidden(!(j <= numberOfItems));
            componentTree.locate("t" + position + j + "1", TextComponent.class).setHidden(!(j <= numberOfItems));
            componentTree.locate("t" + position + j + "2", TextComponent.class).setHidden(!(j <= numberOfItems));
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
            if (!Images.areEqual(Images.getInstance().getImage(max.getMaterial(), 33, 33, false), bi)) {
                ic.setImage(Images.getInstance().getImage(max.getMaterial(), 33, 33, false));
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

                float variation = RoundUtils.roundToOne(-100 + 100*(item.getPrice().getValue()/item.getPrices(TimeSpan.MINUTE).get(0)));

                if (variation != 0) {
                    if (abs(variation) > abs(-100 + 100*(imax.getPrice().getValue()/imax.getPrices(TimeSpan.MINUTE).get(0)))){
                        imax = item;
                    }
                }
            }
            items.remove(imax);

            ImageComponent ic = icTree.locate("top" + i, ImageComponent.class);

            BufferedImage bi = (BufferedImage) ic.getImage();
            if (!Images.areEqual(Images.getInstance().getImage(imax.getMaterial(), 33, 33, false), bi)) {
                ic.setImage(Images.getInstance().getImage(imax.getMaterial(), 33, 33, false));
            }

            Item finalImax = imax;
            icTree.locate("top" + i, ImageComponent.class).setClickAction((interaction, player, primaryTrigger) -> {
                interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("TS1");
                interaction.getComponentTree().locate("graph1", GraphComponent.class).changeMat(finalImax.getMaterial());
            });

            float fvar = RoundUtils.roundToOne(-100 + 100*(imax.getPrice().getValue()/imax.getPrices(TimeSpan.MINUTE).get(0)));

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

        Config config = Config.getInstance();
        // Title
        for (int i = 1; i <= 4 ; i++) { cTree.locate("title" + i, TextComponent.class).setText(config.getTitle()); }
        // Top Movers
        cTree.locate("w1omKKFS", TextComponent.class).setText(config.getTopMoversText());
        // Sub top movers
        cTree.locate("ypqwCPVb", TextComponent.class).setText(config.getSubTopMoversText());
        // Buy
        cTree.locate("8mbDiOVM", TextComponent.class).setText(config.getBuyText());
        cTree.locate("rK8xOEwj", TextComponent.class).setText(config.getBuyText());
        // Sell
        cTree.locate("jEXxBLF2", TextComponent.class).setText(config.getSellText());
        cTree.locate("ZakfQVQ0", TextComponent.class).setText(config.getSellText());
        // Price text
        cTree.locate("EGgOeYza", TextComponent.class).setText(config.getPriceText());
        cTree.locate("EfJrz4vo", TextComponent.class).setText(config.getPriceText());
        // Amount
        cTree.locate("pfFe6Wjt", TextComponent.class).setText(config.getAmountSelectionText());
        cTree.locate("ityZyfNt", TextComponent.class).setText(config.getAmountSelectionText());
        // Trend
        cTree.locate("pfV4FIy1", TextComponent.class).setText(config.getTrendText());
    }

}
