package me.bounser.nascraft.advancedgui;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.advancedgui.components.GraphComponent;
import me.bounser.nascraft.advancedgui.components.SlideComponent;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.formatter.RoundUtils;
import me.leoko.advancedgui.manager.LayoutManager;
import me.leoko.advancedgui.utils.LayoutExtension;
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

    private SlideComponent slideComponent = null;
    private GraphComponent graphComponent = null;

    public HashMap<Player, Category> playerCategory;
    // Offset of items of the selected category.
    public HashMap<Player, Integer> playerOffset;

    private static LayoutModifier instance;

    public static LayoutModifier getInstance() { return instance == null ? instance = new LayoutModifier() : instance; }

    private LayoutModifier () { LayoutManager.getInstance().registerLayoutExtension(this, Nascraft.getInstance()); }

    @Override
    @EventHandler
    public void onLayoutLoad(LayoutLoadEvent event) {

        if (!event.getLayout().getName().equals("Nascraft")) return;

        playerCategory = new HashMap<>();
        playerOffset = new HashMap<>();

        GroupComponent componentTree = event.getLayout().getTemplateComponentTree();
        GroupComponent tradeScreen = componentTree.locate("TS1", GroupComponent.class); // Trade Screen

        updateMainPage(componentTree, false, null);

        //

        ViewComponent backgroundView = tradeScreen.locate("bview1", ViewComponent.class);
        TextComponent mainText = tradeScreen.locate("maintext1", TextComponent.class);

        tradeScreen.getComponents().remove(backgroundView);
        tradeScreen.getComponents().remove(mainText);

        if (graphComponent == null)
            graphComponent = new GraphComponent("graph1", null, false,
                event.getLayout().getDefaultInteraction(), 11, 54, 362, 139, backgroundView, mainText);
        tradeScreen.locate("graph123", DummyComponent.class).setComponent(graphComponent);

        //

        GroupComponent slideComponents = tradeScreen.locate("SComp1", GroupComponent.class);

        tradeScreen.getComponents().remove(slideComponents);

        if (slideComponent == null) {
            RectComponent bar = slideComponents.locate("bar", RectComponent.class);
            RectComponent translucid = slideComponents.locate("translucid", RectComponent.class);
            ImageComponent up = slideComponents.locate("upgreen", ImageComponent.class);
            ImageComponent down = slideComponents.locate("downred", ImageComponent.class);
            TextComponent textslide = slideComponents.locate("textslide1", TextComponent.class);
            TextComponent timetext = slideComponents.locate("time1", TextComponent.class);
            TextComponent perslide = slideComponents.locate("perslide1", TextComponent.class);
            RectComponent divisor = slideComponents.locate("TIAYadch", RectComponent.class);

            slideComponent = new SlideComponent("slide1", null, false,
                    event.getLayout().getDefaultInteraction(), 11, 60, 362, 121, bar, translucid, up, down, textslide, timetext, perslide, divisor);
        }

        tradeScreen.locate("slide123", DummyComponent.class).setComponent(slideComponent);

        // Arrows
        componentTree.locate("ArrowUP").setClickAction((interaction, player, primaryTrigger) -> {
            changeCategory(player, interaction, -1);
        });

        componentTree.locate("ArrowDOWN").setClickAction((interaction, player, primaryTrigger) -> {
            changeCategory(player, interaction, 1);
        });

        ImageComponent arrowRight = componentTree.locate("ArrowRight", ImageComponent.class);
        ImageComponent arrowLeft = componentTree.locate("ArrowLeft", ImageComponent.class);

        arrowRight.setClickAction((interaction, playerAction, primaryTrigger) -> {

            playerOffset.put(playerAction, playerOffset.get(playerAction)+1);
            renderRow(interaction.getComponentTree(), playerCategory.get(playerAction), playerOffset.get(playerAction), 1);

        });
        arrowLeft.setClickAction((interaction, playerAction, primaryTrigger) -> {

            playerOffset.put(playerAction, playerOffset.get(playerAction)-1);
            renderRow(interaction.getComponentTree(), playerCategory.get(playerAction), playerOffset.get(playerAction), 1);

        });

        // Time Span selectors
        for (TimeSpan timeSpan : TimeSpan.values()) {
            componentTree.locate("timesel" + timeSpan.toString(), RectComponent.class).setClickAction((interaction, player, primaryTrigger) -> {
                interaction.getComponentTree().locate("graph1", GraphComponent.class).setTimeFrame(timeSpan);
            });
        }

        // Back button
        componentTree.locate("back").setClickAction((interaction, player, primaryTrigger) -> {
            interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("I4ztUi1d");
            updateMainPage(interaction.getComponentTree(), true, player);
        });

        // Buy/Sell buttons
        for (int j : Arrays.asList(1,16,64)) {
            componentTree.locate("buy" + j).setClickAction((interaction, player, primaryTrigger) -> {
                GraphComponent g = interaction.getComponentTree().locate("graph1", GraphComponent.class);
                g.getItem().buyItem(j, player.getUniqueId(), true, g.getMaterial());
                g.updateButtonPrice();
            });
            componentTree.locate("sell" + j).setClickAction((interaction, player, primaryTrigger) -> {
                GraphComponent g = interaction.getComponentTree().locate("graph1", GraphComponent.class);
                g.getItem().sellItem(j, player.getUniqueId(), true, g.getMaterial());
                g.updateButtonPrice();
            });
        }
        setLang(componentTree);
    }

    public void changeCategory(Player player, Interaction interaction, int rotateDirection) {

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
                if(indexOfCategory+1 != MarketManager.getInstance().getCategories().size()) {
                    playerCategory.put(player, MarketManager.getInstance().getCategories().get(indexOfCategory+1));
                } else {
                    playerCategory.put(player, MarketManager.getInstance().getCategories().get(0));
                }
                break;
        }

        playerOffset.put(player, 0);
        updateMainPage(interaction.getComponentTree(), false, player);
    }

    @EventHandler
    public void onInteractionStart(GuiInteractionBeginEvent event) {

        if (!event.getGuiInstance().getLayout().getName().equals("Nascraft")) return;

        playerCategory.put(event.getPlayer(), MarketManager.getInstance().getCategories().get(0));
        playerOffset.put(event.getPlayer(), 0);

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

            if(indexOfCategory + 2 < defaultCategories.size()) {

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

            if(i == 1) {
                playerOffset.putIfAbsent(player, 0);
                renderRow(cTree, category, playerOffset.get(player), i);
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

                Item item = category.getItemOfIndex(j-1+offset);

                componentTree.locate("t" + position + j + "1", TextComponent.class).setText(Formatter.format(item.getPrice().getValue(), Style.REDUCED_LENGTH));
                componentTree.locate("t" + position + j + "2", TextComponent.class).setText(Formatter.format(item.getPrice().getValue(), Style.REDUCED_LENGTH));

                ImageComponent ic = componentTree.locate("asdi" + position + "" + j, ImageComponent.class);
                ic.setImage(Images.getInstance().getImage(item, 32, 32, false));

                ic.setClickAction((interaction, p, primaryTrigger) -> {

                    interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("TS1");
                    interaction.getComponentTree().locate("graph1", GraphComponent.class).changeMat(item);

                });
            }
            componentTree.locate("asdi" + position + j, ImageComponent.class).setHidden(!(j <= numberOfItems));
            componentTree.locate("t" + position + j + "1", TextComponent.class).setHidden(!(j <= numberOfItems));
            componentTree.locate("t" + position + j + "2", TextComponent.class).setHidden(!(j <= numberOfItems));
        }

        if (position != 1) return;

        ImageComponent arrowRight = componentTree.locate("ArrowRight", ImageComponent.class);
        ImageComponent arrowLeft = componentTree.locate("ArrowLeft", ImageComponent.class);

        if(category.getItems().size() <= 6) {
            arrowRight.setHidden(true);
            arrowLeft.setHidden(true);
            return;
        }

        arrowRight.setHidden(offset >= (category.getItems().size()) - 6);

        arrowLeft.setHidden(offset < 1);
    }

    public void updateTrending(GroupComponent icTree) {
        Item trendingItem = null;

        for (Item item : MarketManager.getInstance().getAllItems()) {
            if ((trendingItem == null || trendingItem.getOperations() < item.getOperations()) && item.getOperations() > 10) trendingItem = item;
        }

        if (trendingItem != null) {
            icTree.locate("trend1", GroupComponent.class).setHidden(false);
            ImageComponent ic = icTree.locate("trend", ImageComponent.class);

            BufferedImage bi = (BufferedImage) ic.getImage();
            if (!Images.areEqual(Images.getInstance().getImage(trendingItem, 33, 33, false), bi)) {
                ic.setImage(Images.getInstance().getImage(trendingItem, 33, 33, false));
            }

            Item finalTrendingItem = trendingItem;
            ic.setClickAction((interaction, player, primaryTrigger) -> {
                interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("TS1");
                interaction.getComponentTree().locate("graph1", GraphComponent.class).changeMat(finalTrendingItem);
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

                float variation = RoundUtils.roundToOne(-100 + 100*(item.getPrice().getValue()/item.getPrices(TimeSpan.HOUR).get(0)));

                if (variation != 0) {
                    if (abs(variation) > abs(-100 + 100*(imax.getPrice().getValue()/imax.getPrices(TimeSpan.HOUR).get(0)))){
                        imax = item;
                    }
                }
            }
            items.remove(imax);

            ImageComponent ic = icTree.locate("top" + i, ImageComponent.class);

            BufferedImage bi = (BufferedImage) ic.getImage();
            if (!Images.areEqual(Images.getInstance().getImage(imax, 33, 33, false), bi)) {
                ic.setImage(Images.getInstance().getImage(imax, 33, 33, false));
            }

            Item finalImax = imax;
            icTree.locate("top" + i, ImageComponent.class).setClickAction((interaction, player, primaryTrigger) -> {
                interaction.getComponentTree().locate("mainView1", ViewComponent.class).setView("TS1");
                interaction.getComponentTree().locate("graph1", GraphComponent.class).changeMat(finalImax);
            });

            float fvar = RoundUtils.roundToOne(-100 + 100*(imax.getPrice().getValue()/imax.getPrices(TimeSpan.HOUR).get(0)));

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
        for (int i = 1; i <= 4 ; i++) { cTree.locate("title" + i, TextComponent.class).setText(Lang.get().message(Message.ADVANCEDGUI_TITLE)); }
        // Top Movers
        cTree.locate("w1omKKFS", TextComponent.class).setText(Lang.get().message(Message.ADVANCEDGUI_TOP_MOVERS));
        // Sub top movers
        cTree.locate("ypqwCPVb", TextComponent.class).setText(Lang.get().message(Message.ADVANCEDGUI_SUBTOP));
        // Buy
        cTree.locate("8mbDiOVM", TextComponent.class).setText(Lang.get().message(Message.ADVANCEDGUI_BUY));
        cTree.locate("rK8xOEwj", TextComponent.class).setText(Lang.get().message(Message.ADVANCEDGUI_BUY));
        // Sell
        cTree.locate("jEXxBLF2", TextComponent.class).setText(Lang.get().message(Message.ADVANCEDGUI_SELL));
        cTree.locate("ZakfQVQ0", TextComponent.class).setText(Lang.get().message(Message.ADVANCEDGUI_SELL));
        // Price text
        cTree.locate("EGgOeYza", TextComponent.class).setText(Lang.get().message(Message.ADVANCEDGUI_PRICE));
        cTree.locate("EfJrz4vo", TextComponent.class).setText(Lang.get().message(Message.ADVANCEDGUI_PRICE));
        // Amount
        cTree.locate("pfFe6Wjt", TextComponent.class).setText(Lang.get().message(Message.ADVANCEDGUI_AMOUNT_SELECTION));
        cTree.locate("ityZyfNt", TextComponent.class).setText(Lang.get().message(Message.ADVANCEDGUI_AMOUNT_SELECTION));
        // Trend
        cTree.locate("pfV4FIy1", TextComponent.class).setText(Lang.get().message(Message.ADVANCEDGUI_TREND));
    }
}
