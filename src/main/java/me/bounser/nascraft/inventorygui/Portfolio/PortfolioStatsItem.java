package me.bounser.nascraft.inventorygui.Portfolio;

import me.bounser.nascraft.chart.portfolio.PortfolioCompositionChart;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.portfolio.Portfolio;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.AbstractItem;

import java.awt.*;
import java.util.*;
import java.util.List;

public class PortfolioStatsItem extends AbstractItem {

    private final Portfolio portfolio;
    private final Player player;
    private final ModeItem modeItem;

    public PortfolioStatsItem(Portfolio portfolio, Player player, ModeItem modeItem) {
        this.portfolio = portfolio;
        this.player = player;
        this.modeItem = modeItem;
    }

    @Override
    public ItemProvider getItemProvider() {

        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_COMPOSITION_STATS_NAME));

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        skullMeta.setOwningPlayer(player);
        item.setItemMeta(skullMeta);

        String loreString = Lang.get().message(Message.PORTFOLIO_COMPOSITION_STATS_LORE)
                .replace("[COMPOSITION]", getLoreComposition());

        List<String> lore = new ArrayList<>();

        for (String line : loreString.split("\\n")) {
            Component componentLine = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(componentLine));
        }

        return new ItemBuilder(item)
                .setDisplayName(BukkitComponentSerializer.legacy().serialize(title))
                .setLegacyLore(lore);
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent inventoryClickEvent) {

    }

    public String getLoreComposition() {

        String lore = "";

        HashMap<Item, Integer> content = portfolio.getContent();

        if (content.isEmpty())
            return Lang.get().message(Message.PORTFOLIO_COMPOSITION_EMPTY);

        double totalWorth = 0;
        List<Map.Entry<Item, Double>> itemWorthList = new ArrayList<>();

        for (Item item : content.keySet()) {
            double worth = item.getPrice().getValue() * content.get(item);
            itemWorthList.add(new AbstractMap.SimpleEntry<>(item, worth));
            totalWorth += worth;
        }

        itemWorthList.sort((entry1, entry2) -> Double.compare(entry2.getValue(), entry1.getValue()));

        double threshold = 0.03 * totalWorth;
        int i = 0;

        Color[] colors = PortfolioCompositionChart.colorPalette;

        for (Map.Entry<Item, Double> entry : itemWorthList) {
            Color color = i <= 6 ? colors[i] : new Color(209,206,236);
            double worth = entry.getValue();
            if (worth >= threshold) {
                lore += Lang.get().message(Message.PORTFOLIO_COMPOSITION_STATS_SEGMENT)
                        .replace("[COLOR]", "<color:" + String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue())  + ">")
                        .replace("[NAME]", entry.getKey().getTaggedName())
                        .replace("[PERCENTAGE]", RoundUtils.roundTo(worth*100/totalWorth, 2) + "%")
                        .replace("[WORTH]",Formatter.format(entry.getKey().getCurrency(), worth, Style.ROUND_BASIC));
            } else {
                lore += Lang.get().message(Message.PORTFOLIO_COMPOSITION_STATS_SEGMENT)
                        .replace("[COLOR]", "<color:" + String.format("#%02X%02X%02X", new Color(209,206,236).getRed(), new Color(209,206,236).getGreen(), new Color(209,206,236).getBlue())  + ">")
                        .replace("[NAME]", entry.getKey().getTaggedName())
                        .replace("[PERCENTAGE]", RoundUtils.roundTo(worth*100/totalWorth, 2) + "%")
                        .replace("[WORTH]",Formatter.format(entry.getKey().getCurrency(), worth, Style.ROUND_BASIC));
            }
            i++;
        }

        return lore;
    }

}
