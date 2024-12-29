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
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.MapPalette;
import xyz.xenondevs.inventoryaccess.map.MapPatch;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.window.CartographyWindow;

import java.awt.image.BufferedImage;
import java.util.Arrays;

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
                "I C")
                .addIngredient('I', statsItem)
                .addIngredient('C', timeFrameItem);

        Gui gui = Gui.normal()
                .setStructure(structure)
                .build();

        CartographyWindow window = CartographyWindow.single()
                .setViewer(player)
                .setTitle(BukkitComponentSerializer.legacy().serialize(title))
                .setGui(gui)
                .build();

        window.setCloseHandlers(Arrays.asList(new Runnable() {
            @Override
            public void run() {
                MarketMenuManager.getInstance().setMenuOfPlayer(player, new BuySellMenu(player, item));
                Bukkit.getScheduler().runTaskLater(Nascraft.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        MarketMenuManager.getInstance().setMenuOfPlayer(player, new BuySellMenu(player, item));
                    }
                }, 1);
            }
        }));

        window.updateMap(getMapPatch(item, ChartType.DAY));

        window.open();
    }

    @Override
    public void close() {

    }

    @Override
    public void update() {

    }

    public static MapPatch getMapPatch(Item item, ChartType type) {

        BufferedImage graphImage = ItemChartReduced.getImage(item, type);

        return new MapPatch(0, 0, 128, 128, MapPalette.imageToBytes(graphImage));
    }

}
