package me.bounser.nascraft.inventorygui.Portfolio;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.chart.portfolio.PortfolioCompositionChart;
import me.bounser.nascraft.chart.portfolio.PortfolioEvolutionChart;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.inventorygui.MenuPage;
import me.bounser.nascraft.portfolio.Portfolio;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.map.MapPalette;
import org.bukkit.metadata.FixedMetadataValue;
import xyz.xenondevs.inventoryaccess.map.MapPatch;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.structure.Structure;
import xyz.xenondevs.invui.window.CartographyWindow;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class InfoPortfolio implements MenuPage {

    private Player player;
    private Portfolio portfolio;
    private ModeItem modeItem;

    public InfoPortfolio(Portfolio portfolio, Player player) {
        this.portfolio = portfolio;
        this.player = player;

        open();
    }

    @Override
    public void open() {
        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_COMPOSITION_TITLE));

        this.modeItem = new ModeItem(portfolio);
        PortfolioStatsItem stats = new PortfolioStatsItem(portfolio, player, modeItem);

        Structure structure = new Structure(
                "I C")
                .addIngredient('I', modeItem)
                .addIngredient('C', stats);

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
                Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TITLE));

                Inventory inventory = Bukkit.createInventory(player, 45, BukkitComponentSerializer.legacy().serialize(title));
                player.openInventory(inventory);
                player.setMetadata("NascraftPortfolio", new FixedMetadataValue(Nascraft.getInstance(),false));

                PortfolioInventory.getInstance().updatePortfolioInventory(player);

                Bukkit.getScheduler().runTaskLater(Nascraft.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TITLE));

                        Inventory inventory = Bukkit.createInventory(player, 45, BukkitComponentSerializer.legacy().serialize(title));
                        player.openInventory(inventory);
                        player.setMetadata("NascraftPortfolio", new FixedMetadataValue(Nascraft.getInstance(),false));

                        PortfolioInventory.getInstance().updatePortfolioInventory(player);

                    }
                }, 1);
            }
        }));

        window.updateMap(getMapPatchComposition(portfolio));

        window.open();
    }

    @Override
    public void close() {

    }

    @Override
    public void update() {

    }

    public static MapPatch getMapPatchComposition(Portfolio portfolio) {

        BufferedImage graphImage = PortfolioCompositionChart.getImage(portfolio, 128, 128);
        
        return new MapPatch(0, 0, 128, 128, MapPalette.imageToBytes(graphImage));
    }

    public static MapPatch getMapPatchEvolution(Portfolio portfolio) {

        BufferedImage graphImage = PortfolioEvolutionChart.getImage(portfolio, 128, 128);

        return new MapPatch(0, 0, 128, 128, MapPalette.imageToBytes(graphImage));
    }
}
