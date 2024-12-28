package me.bounser.nascraft.inventorygui.Portfolio;

import me.bounser.nascraft.portfolio.Portfolio;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

public class ModeItem extends AbstractItem {

    private Portfolio portfolio;
    private PortfolioChartType type;

    public ModeItem(Portfolio portfolio) {
        this.portfolio = portfolio;
        this.type = PortfolioChartType.COMPOSITION;
    }

    @Override
    public ItemProvider getItemProvider() {
        return new ItemBuilder(new ItemStack(Material.BOOK));
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {

        /*
        if (type.equals(PortfolioChartType.COMPOSITION)) type = PortfolioChartType.EVOLUTION;
        else type = PortfolioChartType.COMPOSITION;

        CartographyWindow window = (CartographyWindow) getWindows().iterator().next();

        switch (type) {
            case COMPOSITION -> window.updateMap(InfoPortfolio.getMapPatchComposition(portfolio));
            case EVOLUTION -> window.updateMap(InfoPortfolio.getMapPatchEvolution(portfolio));
        }

        notifyWindows(); */
    }

    public PortfolioChartType getPortfolioChartType() {
        return type;
    }
}
