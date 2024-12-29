package me.bounser.nascraft.inventorygui;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.limitorders.LimitOrder;
import me.bounser.nascraft.market.limitorders.LimitOrdersManager;
import me.bounser.nascraft.market.limitorders.OrderType;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static me.bounser.nascraft.inventorygui.LimitOrdersMenu.getFormattedTime;

public class BuySellMenu implements MenuPage{

    private Inventory gui;

    private final Player player;
    private final Item item;


    public BuySellMenu(Player player, Item item) {
        this.player = player;
        this.item = item;

        open();
    }

    @Override
    public void open() {

        Config config = Config.getInstance();

        gui = Bukkit.createInventory(null, config.getBuySellMenuSize(), item.getFormattedName());

        // Item

        ItemStack itemStack = item.getItemStack();

        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(item.getFormattedName());

        List<String> prevLore = meta.getLore();

        List<String> itemLore = MarketMenuManager.getInstance().getLoreFromItem(item, Lang.get().message(Message.GUI_BUYSELL_ITEM_LORE));

        if (meta.hasLore() && prevLore != null) {
            itemLore.add("");
            itemLore.addAll(prevLore);
            meta.setLore(itemLore);
        } else {
            meta.setLore(itemLore);
        }

        itemStack.setItemMeta(meta);

        gui.setItem(
                config.getBuySellMenuItemSlot(),
                itemStack
        );

        // Back button

        if (config.getBuySellBackEnabled()) {
            Component backComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_CATEGORY_BACK_NAME));

            gui.setItem(
                    config.getBuySellBackSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getBuySellBackMaterial(),
                            BukkitComponentSerializer.legacy().serialize(backComponent)
                    ));
        }

        // Alerts

        List<String> lore = new ArrayList<>();

        boolean linked = LinkManager.getInstance().getUserDiscordID(player.getUniqueId()) != null;

        if (config.getAlertsBuySellEnabled()) {

            HashMap<Item, Double> alerts = DiscordAlerts.getInstance().getAlertsOfUUID(player.getUniqueId());

            if (alerts != null && alerts.containsKey(item)) {

                Component alert = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_BUYSELL_ALERTS_NAME_SETUP));

                for (String line : Lang.get().message(Message.GUI_BUYSELL_ALERTS_LORE_SETUP)
                        .replace("[PRICE]", Formatter.format(item.getCurrency(), Math.abs(alerts.get(item)), Style.ROUND_BASIC)).split("\\n")) {
                    Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                    lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
                }

                gui.setItem(
                        config.getAlertsBuySellSlot(),
                        MarketMenuManager.getInstance().generateItemStack(
                                config.getAlertsBuySellMaterial(),
                                BukkitComponentSerializer.legacy().serialize(alert),
                                lore
                        ));

            } else {
                Component alert = MiniMessage.miniMessage().deserialize(Lang.get().message(linked ? Message.GUI_BUYSELL_ALERTS_NAME_LINKED : Message.GUI_BUYSELL_ALERTS_NAME_NOT_LINKED));

                for (String line : Lang.get().message(linked ? Message.GUI_BUYSELL_ALERTS_LORE_LINKED : Message.GUI_BUYSELL_ALERTS_LORE_NOT_LINKED).split("\\n")) {
                    Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                    lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
                }

                gui.setItem(
                        config.getAlertsBuySellSlot(),
                        MarketMenuManager.getInstance().generateItemStack(
                                config.getAlertsBuySellMaterial(),
                                BukkitComponentSerializer.legacy().serialize(alert),
                                lore
                        ));
            }
        }

        // Limit Orders

        lore.clear();

        if (config.getLimitOrdersBuySellEnabled()) {

            List<LimitOrder> orders = LimitOrdersManager.getInstance().getPlayerLimitOrders(player.getUniqueId());

            LimitOrder order = null;

            for (LimitOrder limitOrder : orders)
                if (limitOrder.getItem().equals(item)) order = limitOrder;

            Component limitComponent;

            if (order != null) {

                String limitLore;

                if (order.isCompleted()) {
                    limitLore = Lang.get().message(order.getOrderType().equals(OrderType.LIMIT_BUY) ? Message.GUI_LIMIT_ORDERS_LORE_FILLED_BUY : Message.GUI_LIMIT_ORDERS_LORE_FILLED_SELL)
                            .replace("[PRICE-ITEM]", Formatter.format(order.getItem().getCurrency(), order.getCost()/order.getCompleted(), Style.ROUND_BASIC));
                } else if (order.isExpired()) {
                    limitLore = Lang.get().message(order.getOrderType().equals(OrderType.LIMIT_BUY) ? Message.GUI_LIMIT_ORDERS_LORE_EXPIRED_BUY : Message.GUI_LIMIT_ORDERS_LORE_EXPIRED_SELL)
                            .replace("[PRICE-ITEM]", Formatter.format(order.getItem().getCurrency(), order.getCost()/order.getCompleted(), Style.ROUND_BASIC));
                } else {
                    limitLore = Lang.get().message(order.getOrderType().equals(OrderType.LIMIT_BUY) ? Message.GUI_LIMIT_ORDERS_LORE_UNFILLED_BUY : Message.GUI_LIMIT_ORDERS_LORE_UNFILLED_SELL);
                }

                limitLore = limitLore
                        .replace("[NAME]", order.getItem().getTaggedName())
                        .replace("[FILLED]", String.valueOf(order.getCompleted()))
                        .replace("[TO-FILL]", String.valueOf(order.getToComplete()))
                        .replace("[PRICE]", Formatter.format(order.getItem().getCurrency(), order.getPrice(), Style.ROUND_BASIC));

                float change = 0;

                if (order.getOrderType().equals(OrderType.LIMIT_BUY)) {
                    change = RoundUtils.roundToTwo(-100 + (float) (order.getPrice() * 100 / order.getItem().getPrice().getBuyPrice()));
                    limitLore = limitLore
                            .replace("[CURRENT-PRICE]", Formatter.format(order.getItem().getCurrency(), order.getItem().getPrice().getBuyPrice(), Style.ROUND_BASIC))
                            .replace("[QUANTITY]", String.valueOf(order.getCompleted()))
                            .replace("[COMPENSATION]", Formatter.format(order.getItem().getCurrency(), ((order.getToComplete()) * order.getPrice()) - order.getCost(), Style.ROUND_BASIC));
                } else {
                    change = RoundUtils.roundToTwo(-100 + (float) (order.getPrice() * 100 / order.getItem().getPrice().getSellPrice()));
                    limitLore = limitLore
                            .replace("[CURRENT-PRICE]", Formatter.format(order.getItem().getCurrency(), order.getItem().getPrice().getSellPrice(), Style.ROUND_BASIC))
                            .replace("[QUANTITY]", String.valueOf(order.getToComplete() - order.getCompleted()))
                            .replace("[COMPENSATION]", Formatter.format(order.getItem().getCurrency(), order.getCost(), Style.ROUND_BASIC));
                }

                if (!order.isExpired() && !order.isCompleted()) {
                    limitLore = limitLore
                            .replace("[CHANGE]", (change > 0 ? Lang.get().message(Message.GUI_LIMIT_ORDERS_CHANGE_POSITIVE) : Lang.get().message(Message.GUI_LIMIT_ORDERS_CHANGE_NEGATIVE)).replace("[CHANGE]", String.valueOf(change)))
                            .replace("[EXPIRATION]", getFormattedTime(order.getExpiration()));
                }

                lore.clear();

                for (String line : limitLore.split("\\n")) {
                    Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                    lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
                }

                limitComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_BUYSELL_LIMIT_NAME_UNSET));

            } else {

                limitComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_BUYSELL_LIMIT_NAME_SET));

                for (String line : Lang.get().message(Message.GUI_BUYSELL_LIMIT_LORE_SET).split("\\n")) {
                    Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                    lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
                }
            }

            gui.setItem(
                    config.getLimitOrdersBuySellSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getLimitOrdersBuySellMaterial(),
                            BukkitComponentSerializer.legacy().serialize(limitComponent),
                            lore
                    ));
        }

        // Information

        if (config.getInfoBuySellEnabled()) {

            Component information = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_BUYSELL_INFO_NAME));

            lore.clear();
            for (String line : Lang.get().message(Message.GUI_BUYSELL_INFO_LORE).split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            gui.setItem(
                    config.getInfoBuySellSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getInfoBuySellMaterial(),
                            BukkitComponentSerializer.legacy().serialize(information),
                            lore
                    ));
        }

        // Fillers

        Component fillerComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_FILLERS_NAME));

        ItemStack filler = MarketMenuManager.getInstance().generateItemStack(
                config.getBuySellFillersMaterial(),
                BukkitComponentSerializer.legacy().serialize(fillerComponent)
        );

        for (int i : config.getBuySellFillersSlots())
            gui.setItem(i, filler);


        // Buy button

        HashMap<Integer, Integer> buyButtons = config.getBuySellBuySlots();

        for (int amount : buyButtons.keySet()) {

            Component buyComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_BUYSELL_BUY_BUTTONS_NAME)
                    .replace("[AMOUNT]", String.valueOf(amount)));

            lore.clear();
            String buyLore = Lang.get().message(Message.GUI_BUYSELL_BUY_BUTTONS_LORE)
                    .replace("[AMOUNT]", String.valueOf(amount))
                    .replace("[WORTH]", String.valueOf(Formatter.format(item.getCurrency(), item.getPrice().getProjectedCost(-amount, item.getPrice().getBuyTaxMultiplier()), Style.ROUND_BASIC)));

            for (String line : buyLore.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            ItemStack buyButton = MarketMenuManager.getInstance().generateItemStack(
                    config.getBuySellBuyMaterial(),
                    BukkitComponentSerializer.legacy().serialize(buyComponent),
                    lore
            );

            buyButton.setAmount(amount);

            gui.setItem(buyButtons.get(amount), buyButton);

        }

        // Sell button

        HashMap<Integer, Integer> sellButtons = config.getBuySellSellSlots();

        for (int amount : sellButtons.keySet()) {

            Component buyComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_BUYSELL_SELL_BUTTONS_NAME)
                    .replace("[AMOUNT]", String.valueOf(amount)));

            lore.clear();
            String sellLore = Lang.get().message(Message.GUI_BUYSELL_SELL_BUTTONS_LORE)
                    .replace("[AMOUNT]", String.valueOf(amount))
                    .replace("[WORTH]", String.valueOf(Formatter.format(item.getCurrency(), item.getPrice().getProjectedCost(amount, item.getPrice().getSellTaxMultiplier()), Style.ROUND_BASIC)));

            for (String line : sellLore.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            ItemStack sellButton = MarketMenuManager.getInstance().generateItemStack(
                    config.getBuySellSellMaterial(),
                    BukkitComponentSerializer.legacy().serialize(buyComponent),
                    lore
            );

            sellButton.setAmount(amount);

            gui.setItem(sellButtons.get(amount), sellButton);
        }

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "item-menu-" + item.getIdentifier()));

    }

    @Override
    public void close() {

    }

    @Override
    public void update() {

        Config config = Config.getInstance();

        // Item

        gui.setItem(
                config.getBuySellMenuItemSlot(),
                MarketMenuManager.getInstance().generateItemStack(
                        item.getItemStack().getType(),
                        item.getFormattedName(),
                        MarketMenuManager.getInstance().getLoreFromItem(item, Lang.get().message(Message.GUI_BUYSELL_ITEM_LORE))
                )
        );

        // Alerts

        List<String> lore = new ArrayList<>();

        boolean linked = LinkManager.getInstance().getUserDiscordID(player.getUniqueId()) != null;

        if (config.getAlertsBuySellEnabled()) {

            HashMap<Item, Double> alerts = DiscordAlerts.getInstance().getAlertsOfUUID(player.getUniqueId());

            if (alerts != null && alerts.containsKey(item)) {

                Component alert = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_BUYSELL_ALERTS_NAME_SETUP));

                for (String line : Lang.get().message(Message.GUI_BUYSELL_ALERTS_LORE_SETUP)
                        .replace("[PRICE]", Formatter.format(item.getCurrency(), Math.abs(alerts.get(item)), Style.ROUND_BASIC)).split("\\n")) {
                    Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                    lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
                }

                gui.setItem(
                        config.getAlertsBuySellSlot(),
                        MarketMenuManager.getInstance().generateItemStack(
                                config.getAlertsBuySellMaterial(),
                                BukkitComponentSerializer.legacy().serialize(alert),
                                lore
                        ));

            } else {
                Component alert = MiniMessage.miniMessage().deserialize(Lang.get().message(linked ? Message.GUI_BUYSELL_ALERTS_NAME_LINKED : Message.GUI_BUYSELL_ALERTS_NAME_NOT_LINKED));

                for (String line : Lang.get().message(linked ? Message.GUI_BUYSELL_ALERTS_LORE_LINKED : Message.GUI_BUYSELL_ALERTS_LORE_NOT_LINKED).split("\\n")) {
                    Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                    lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
                }

                gui.setItem(
                        config.getAlertsBuySellSlot(),
                        MarketMenuManager.getInstance().generateItemStack(
                                config.getAlertsBuySellMaterial(),
                                BukkitComponentSerializer.legacy().serialize(alert),
                                lore
                        ));
            }
        }

        // Buy button

        HashMap<Integer, Integer> buyButtons = config.getBuySellBuySlots();

        for (int amount : buyButtons.keySet()) {

            Component buyComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_BUYSELL_BUY_BUTTONS_NAME)
                    .replace("[AMOUNT]", String.valueOf(amount)));

            lore.clear();
            String buyLore = Lang.get().message(Message.GUI_BUYSELL_BUY_BUTTONS_LORE)
                    .replace("[AMOUNT]", String.valueOf(amount))
                    .replace("[WORTH]", String.valueOf(Formatter.format(item.getCurrency(), item.getPrice().getProjectedCost(-amount, item.getPrice().getBuyTaxMultiplier()), Style.ROUND_BASIC)));

            for (String line : buyLore.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            ItemStack buyButton = MarketMenuManager.getInstance().generateItemStack(
                    config.getBuySellBuyMaterial(),
                    BukkitComponentSerializer.legacy().serialize(buyComponent),
                    lore
            );

            buyButton.setAmount(amount);

            gui.setItem(buyButtons.get(amount), buyButton);

        }

        // Sell button

        HashMap<Integer, Integer> sellButtons = config.getBuySellSellSlots();

        for (int amount : sellButtons.keySet()) {

            Component buyComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_BUYSELL_SELL_BUTTONS_NAME)
                    .replace("[AMOUNT]", String.valueOf(amount)));

            lore.clear();
            String sellLore = Lang.get().message(Message.GUI_BUYSELL_SELL_BUTTONS_LORE)
                    .replace("[AMOUNT]", String.valueOf(amount))
                    .replace("[WORTH]", String.valueOf(Formatter.format(item.getCurrency(), item.getPrice().getProjectedCost(amount, item.getPrice().getSellTaxMultiplier()), Style.ROUND_BASIC)));

            for (String line : sellLore.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            ItemStack sellButton = MarketMenuManager.getInstance().generateItemStack(
                    config.getBuySellSellMaterial(),
                    BukkitComponentSerializer.legacy().serialize(buyComponent),
                    lore
            );

            sellButton.setAmount(amount);

            gui.setItem(sellButtons.get(amount), sellButton);
        }

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "item-menu-" + item.getIdentifier()));
        MarketMenuManager.getInstance().setMenuOfPlayer(player, this);
    }

}
