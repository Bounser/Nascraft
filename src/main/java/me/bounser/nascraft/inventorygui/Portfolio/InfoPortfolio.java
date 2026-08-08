package me.bounser.nascraft.inventorygui.Portfolio;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.chart.portfolio.PortfolioCompositionChart;
import me.bounser.nascraft.chart.portfolio.PortfolioEvolutionChart;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.inventorygui.MenuPage;
import me.bounser.nascraft.portfolio.Portfolio;
import me.bounser.nascraft.scheduler.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.gui.Structure;
import xyz.xenondevs.invui.window.CartographyWindow;

import java.awt.image.BufferedImage;

public class InfoPortfolio implements MenuPage {

    private final Player player;
    private final Portfolio portfolio;
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
                "I",
                "C")
                .addIngredient('I', modeItem)
                .addIngredient('C', stats);

        Gui gui = Gui.builder()
                .setStructure(structure)
                .build();

        CartographyWindow window = CartographyWindow.builder()
                .setViewer(player)
                .setTitle(title)
                .setInputGui(gui)
                .setMap(getCompositionImage(portfolio))
                .build();

        window.addCloseHandler(reason -> {
            Component portfolioTitle = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TITLE));

            Inventory inventory = Bukkit.createInventory(player, 45, LegacyComponentSerializer.legacySection().serialize(portfolioTitle));
            player.openInventory(inventory);
            player.setMetadata("NascraftPortfolio", new FixedMetadataValue(Nascraft.getInstance(), false));
            PortfolioInventory.getInstance().updatePortfolioInventory(player);

            FoliaScheduler.runAtEntityLater(Nascraft.getInstance(), player, () -> {
                Component delayedTitle = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TITLE));
                Inventory delayedInventory = Bukkit.createInventory(player, 45, LegacyComponentSerializer.legacySection().serialize(delayedTitle));
                player.openInventory(delayedInventory);
                player.setMetadata("NascraftPortfolio", new FixedMetadataValue(Nascraft.getInstance(), false));
                PortfolioInventory.getInstance().updatePortfolioInventory(player);
            }, 1L);
        });

        window.open();
    }

    @Override
    public void close() {
    }

    @Override
    public void update() {
    }

    public static BufferedImage getCompositionImage(Portfolio portfolio) {
        return PortfolioCompositionChart.getImage(portfolio, 128, 128);
    }

    public static BufferedImage getEvolutionImage(Portfolio portfolio) {
        return PortfolioEvolutionChart.getImage(portfolio, 128, 128);
    }
}
