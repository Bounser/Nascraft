package me.bounser.nascraft.inventorygui.MiniChart;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.chart.price.ChartType;
import me.bounser.nascraft.chart.price.ItemChartReduced;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.inventorygui.BuySellMenu;
import me.bounser.nascraft.inventorygui.MarketMenuManager;
import me.bounser.nascraft.inventorygui.MenuPage;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.scheduler.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.Structure;
import xyz.xenondevs.invui.window.CartographyWindow;

import java.awt.image.BufferedImage;

public class InfoMenu implements MenuPage {

    private final Player player;
    private final Item item;

    public InfoMenu(Player player, Item item) {
        this.player = player;
        this.item = item;
        open();
    }

    @Override
    public void open() {
        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_INFO_TITLE));

        StatsItem statsItem = new StatsItem(item, ChartType.DAY);
        TimeFrameItem timeFrameItem = new TimeFrameItem(item, statsItem);

        Structure structure = new Structure(
                "I",
                "C")
                .addIngredient('I', statsItem)
                .addIngredient('C', timeFrameItem);

        Gui gui = Gui.builder()
                .setStructure(structure)
                .build();

        CartographyWindow window = CartographyWindow.builder()
                .setViewer(player)
                .setTitle(title)
                .setInputGui(gui)
                .setMap(getMapImage(item, ChartType.DAY))
                .build();

        timeFrameItem.setWindow(window);

        window.addCloseHandler(reason -> {
            MarketMenuManager.getInstance().setMenuOfPlayer(player, new BuySellMenu(player, item));
            FoliaScheduler.runAtEntityLater(Nascraft.getInstance(), player, () ->
                    MarketMenuManager.getInstance().setMenuOfPlayer(player, new BuySellMenu(player, item)), 1L);
        });

        window.open();
    }

    @Override
    public void close() {
    }

    @Override
    public void update() {
    }

    public static BufferedImage getMapImage(Item item, ChartType type) {
        return ItemChartReduced.getImage(item, type);
    }
}
