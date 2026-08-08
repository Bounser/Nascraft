package me.bounser.nascraft.inventorygui.MiniChart;

import me.bounser.nascraft.chart.price.ChartType;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import xyz.xenondevs.invui.Click;
import xyz.xenondevs.invui.item.AbstractItem;
import xyz.xenondevs.invui.item.ItemBuilder;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.window.CartographyWindow;

import java.util.ArrayList;
import java.util.List;

public class TimeFrameItem extends AbstractItem {

    private final Item item;
    private final StatsItem statsItem;
    private ChartType chartType;
    private int index = 0;
    private CartographyWindow window;

    public TimeFrameItem(Item item, StatsItem statsItem) {
        this.chartType = ChartType.DAY;
        this.item = item;
        this.statsItem = statsItem;
    }

    public void setWindow(CartographyWindow window) {
        this.window = window;
    }

    @Override
    public ItemProvider getItemProvider(Player viewer) {
        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_INFO_TIMEFRAME_NAME));

        List<String> lore = new ArrayList<>();

        for (String line : Lang.get().message(Message.GUI_INFO_TIMEFRAME_LORE_BEFORE).split("\\n")) {
            Component componentLine = MiniMessage.miniMessage().deserialize(line);
            lore.add(LegacyComponentSerializer.legacySection().serialize(componentLine));
        }

        StringBuilder segments = new StringBuilder();
        for (ChartType type : ChartType.values()) {
            String segment;
            if (type.ordinal() == index) {
                segment = Lang.get().message(Message.GUI_INFO_TIMEFRAME_LORE_SELECTED_SEGMENT)
                        .replace("[OPTION]", Lang.get().message(Message.valueOf("GUI_INFO_TIMEFRAME_OPTION_" + (type.ordinal() + 1))));
            } else {
                segment = Lang.get().message(Message.GUI_INFO_TIMEFRAME_LORE_UNSELECTED_SEGMENT)
                        .replace("[OPTION]", Lang.get().message(Message.valueOf("GUI_INFO_TIMEFRAME_OPTION_" + (type.ordinal() + 1))));
            }
            segments.append(segment);
        }

        for (String line : segments.toString().split("\\n")) {
            Component componentLine = MiniMessage.miniMessage().deserialize(line);
            lore.add(LegacyComponentSerializer.legacySection().serialize(componentLine));
        }

        for (String line : Lang.get().message(Message.GUI_INFO_TIMEFRAME_LORE_AFTER).split("\\n")) {
            Component componentLine = MiniMessage.miniMessage().deserialize(line);
            lore.add(LegacyComponentSerializer.legacySection().serialize(componentLine));
        }

        return new ItemBuilder(Material.CLOCK)
                .setName(title)
                .setLegacyLore(lore);
    }

    @Override
    public void handleClick(ClickType clickType, Player player, Click click) {
        index = (index + 1) % ChartType.values().length;
        chartType = ChartType.values()[index];

        if (window != null) {
            window.applyPatch(0, 0, InfoMenu.getMapImage(item, chartType));
        }

        statsItem.setChartType(chartType);
        notifyWindows();
    }

    public ChartType getChartType() {
        return chartType;
    }
}
