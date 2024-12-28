package me.bounser.nascraft.inventorygui;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.limitorders.LimitOrder;
import me.bounser.nascraft.market.limitorders.LimitOrdersManager;
import me.bounser.nascraft.market.limitorders.OrderType;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LimitOrdersMenu implements MenuPage {

    private Inventory gui;

    private final Player player;

    public LimitOrdersMenu(Player player) {
        this.player = player;

        open();
    }

    @Override
    public void open() {

        Config config = Config.getInstance();

        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_LIMIT_ORDERS_TITLE));

        gui = Bukkit.createInventory(null, config.getLimitOrdersMenuSize(), BukkitComponentSerializer.legacy().serialize(title));

        // Back button

        if (config.getLimitOrdersMenuBackEnabled()) {
            Component backComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_LIMIT_ORDERS_BACK_NAME));

            gui.setItem(
                    config.getLimitOrdersMenuBackSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getLimitOrdersMenuBackMaterial(),
                            BukkitComponentSerializer.legacy().serialize(backComponent)
                    ));
        }

        // Fillers

        Component fillerComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_FILLERS_NAME));

        ItemStack filler = MarketMenuManager.getInstance().generateItemStack(
                config.getLimitOrdersMenuFillersMaterial(),
                BukkitComponentSerializer.legacy().serialize(fillerComponent)
        );

        for (int i : config.getLimitOrdersMenuFillersSlots())
            gui.setItem(i, filler);


        // Orders

        setLimitOrders();

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "limitorders"));

    }

    @Override
    public void close() {

    }

    @Override
    public void update() {

        Config config = Config.getInstance();

        for (int slot : config.getAlertsMenuSlots())
            gui.clear(slot);

        setLimitOrders();

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "limitorders"));
        MarketMenuManager.getInstance().setMenuOfPlayer(player, this);
    }

    public void setLimitOrders() {

        List<LimitOrder> orders = LimitOrdersManager.getInstance().getPlayerLimitOrders(player.getUniqueId());

        if (orders == null) return;

        List<Integer> slots = Config.getInstance().getLimitOrdersMenuSlots();

        int i = 0;
        for (LimitOrder order : orders) {

            if (i > slots.size()) break;

            String lore;
            String name = Lang.get().message(order.getOrderType().equals(OrderType.LIMIT_BUY) ? Message.GUI_LIMIT_ORDERS_NAME_BUY : Message.GUI_LIMIT_ORDERS_NAME_SELL)
                    .replace("[ITEM-NAME]", order.getItem().getFormattedName());

            if (order.isCompleted()) {
                lore = Lang.get().message(order.getOrderType().equals(OrderType.LIMIT_BUY) ? Message.GUI_LIMIT_ORDERS_LORE_FILLED_BUY : Message.GUI_LIMIT_ORDERS_LORE_FILLED_SELL)
                        .replace("[PRICE-ITEM]", Formatter.format(order.getItem().getCurrency(), order.getCost()/order.getCompleted(), Style.ROUND_BASIC));
            } else if (order.isExpired()) {
                lore = Lang.get().message(order.getOrderType().equals(OrderType.LIMIT_BUY) ? Message.GUI_LIMIT_ORDERS_LORE_EXPIRED_BUY : Message.GUI_LIMIT_ORDERS_LORE_EXPIRED_SELL)
                        .replace("[PRICE-ITEM]", Formatter.format(order.getItem().getCurrency(), order.getCost()/order.getCompleted(), Style.ROUND_BASIC));
            } else {
                lore = Lang.get().message(order.getOrderType().equals(OrderType.LIMIT_BUY) ? Message.GUI_LIMIT_ORDERS_LORE_UNFILLED_BUY : Message.GUI_LIMIT_ORDERS_LORE_UNFILLED_SELL);
            }

            lore = lore
                    .replace("[NAME]", order.getItem().getTaggedName())
                    .replace("[FILLED]", String.valueOf(order.getCompleted()))
                    .replace("[TO-FILL]", String.valueOf(order.getToComplete()))
                    .replace("[PRICE]", Formatter.format(order.getItem().getCurrency(), order.getPrice(), Style.ROUND_BASIC));

            float change = 0;

            if (order.getOrderType().equals(OrderType.LIMIT_BUY)) {
                change = RoundUtils.roundToTwo(-100 + (float) (order.getPrice() * 100 / order.getItem().getPrice().getBuyPrice()));
                lore = lore
                        .replace("[CURRENT-PRICE]", Formatter.format(order.getItem().getCurrency(), order.getItem().getPrice().getBuyPrice(), Style.ROUND_BASIC))
                        .replace("[QUANTITY]", String.valueOf(order.getCompleted()))
                        .replace("[COMPENSATION]", Formatter.format(order.getItem().getCurrency(), ((order.getToComplete()) * order.getPrice()) - order.getCost(), Style.ROUND_BASIC));
            } else {
                change = RoundUtils.roundToTwo(-100 + (float) (order.getPrice() * 100 / order.getItem().getPrice().getSellPrice()));
                lore = lore
                        .replace("[CURRENT-PRICE]", Formatter.format(order.getItem().getCurrency(), order.getItem().getPrice().getSellPrice(), Style.ROUND_BASIC))
                        .replace("[QUANTITY]", String.valueOf(order.getToComplete() - order.getCompleted()))
                        .replace("[COMPENSATION]", Formatter.format(order.getItem().getCurrency(), order.getCost(), Style.ROUND_BASIC));
            }

            if (!order.isExpired() && !order.isCompleted()) {
                lore = lore
                        .replace("[CHANGE]", (change > 0 ? Lang.get().message(Message.GUI_LIMIT_ORDERS_CHANGE_POSITIVE) : Lang.get().message(Message.GUI_LIMIT_ORDERS_CHANGE_NEGATIVE)).replace("[CHANGE]", String.valueOf(change)))
                        .replace("[EXPIRATION]", getFormattedTime(order.getExpiration()));
            }

            List<String> itemLore = new ArrayList<>();

            for (String line : lore.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                itemLore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            ItemStack limit = MarketMenuManager.getInstance().generateItemStack(
                    order.getItem().getItemStack().getType(),
                    name,
                    itemLore
            );

            gui.setItem(slots.get(i), limit);

            i++;
        }
    }

    public static String getFormattedTime(LocalDateTime date) {

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(now, date);

        long days = duration.toDays();
        duration = duration.minusDays(days);

        long hours = duration.toHours();
        duration = duration.minusHours(hours);

        long minutes = duration.toMinutes();
        duration = duration.minusMinutes(minutes);

        long seconds = duration.getSeconds();

        StringBuilder result = new StringBuilder();
        Lang lang = Lang.get();
        int timeUnitsAdded = 0;

        if (days > 0) {
            result.append(days).append(" ").append(days != 1 ? lang.message(Message.GUI_LIMIT_ORDER_DAYS) : lang.message(Message.GUI_LIMIT_ORDER_DAY));
            timeUnitsAdded++;
            result.append(", ");
        }

        if (hours > 0) {
            result.append(hours).append(" ").append(hours != 1 ? lang.message(Message.GUI_LIMIT_ORDER_HOURS) : lang.message(Message.GUI_LIMIT_ORDER_HOUR));
            timeUnitsAdded++;
            if (timeUnitsAdded < 2) result.append(", ");
        }

        if (minutes > 0 && timeUnitsAdded < 2) {
            result.append(minutes).append(" ").append(minutes != 1 ? lang.message(Message.GUI_LIMIT_ORDER_MINUTES) : lang.message(Message.GUI_LIMIT_ORDER_MINUTE));
            timeUnitsAdded++;
            if (timeUnitsAdded < 2) result.append(", ");
        }

        if (seconds > 0 && timeUnitsAdded < 2) {
            result.append(seconds).append(" ").append(seconds != 1 ? lang.message(Message.GUI_LIMIT_ORDER_SECONDS) : lang.message(Message.GUI_LIMIT_ORDER_SECOND));
            timeUnitsAdded++;
        }

        if (timeUnitsAdded == 0) {
            result.append(lang.message(Message.GUI_LIMIT_ORDER_NOW));
        }

        return result.toString();
    }


}
