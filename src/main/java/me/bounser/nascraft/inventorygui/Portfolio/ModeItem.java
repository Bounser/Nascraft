package me.bounser.nascraft.inventorygui.Portfolio;

import me.bounser.nascraft.portfolio.Portfolio;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;

public class ModeItem extends AbstractItem {

    private final Portfolio portfolio;
    private PortfolioChartType type;

    public ModeItem(Portfolio portfolio) {
        this.portfolio = portfolio;
        this.type = PortfolioChartType.COMPOSITION;
    }

    @Override
    public ItemProvider getItemProvider(Player viewer) {
        return new ItemBuilder(new ItemStack(Material.BOOK));
    }

    @Override
    public void handleClick(ClickType clickType, Player player, Click click) {
        // Mode switching is intentionally disabled in the original implementation.
    }

    public PortfolioChartType getPortfolioChartType() {
        return type;
    }
}
