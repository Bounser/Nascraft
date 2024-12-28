package me.bounser.nascraft.inventorygui;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.limitorders.Duration;
import me.bounser.nascraft.market.limitorders.LimitOrdersManager;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SetLimitOrderMenu implements MenuPage {

    private Inventory gui;

    private final Player player;
    private final Item item;

    private Duration duration;
    private double price = 0;
    private int quantity = 0;

    public SetLimitOrderMenu(Player player, Item item) {
        this.player = player;
        this.item = item;
        this.duration = LimitOrdersManager.getInstance().getDurationOptions().get(0);

        open();
    }

    @Override
    public void open() {

        Config config = Config.getInstance();

        gui = Bukkit.createInventory(null, config.getBuySellMenuSize(), item.getFormattedName());

        // Back button

        if (config.getSetLimitOrderMenuBackEnabled()) {
            Component backComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_SET_LIMIT_ORDER_BACK_NAME));

            gui.setItem(
                    config.getBuySellBackSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getSetLimitOrderMenuBackMaterial(),
                            BukkitComponentSerializer.legacy().serialize(backComponent)
                    ));
        }

        // Fillers

        Component fillerComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_FILLERS_NAME));

        ItemStack filler = MarketMenuManager.getInstance().generateItemStack(
                config.getSetLimitOrdersMenuFillersMaterial(),
                BukkitComponentSerializer.legacy().serialize(fillerComponent)
        );

        for (int i : config.getSetLimitOrdersMenuFillersSlots())
            gui.setItem(i, filler);

        update();
    }

    @Override
    public void close() {

    }

    @Override
    public void update() {

        Config config = Config.getInstance();

        // Time

        Component timeComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_SET_LIMIT_ORDER_TIME_NAME));

        List<String> lore = new ArrayList<>();

        String segments = "";
        for (Duration duration : LimitOrdersManager.getInstance().getDurationOptions()) {

            if (this.duration.equals(duration)) {
                segments += Lang.get().message(Message.GUI_SET_LIMIT_ORDER_DURATION_SEGMENT_SELECTED)
                        .replace("[DURATION]", String.valueOf(duration.getDisplay()))
                        .replace("[FEE]", String.valueOf(duration.getFee() * 100))
                        .replace("[MIN]", String.valueOf(duration.getMinimumFee()));
            } else {
                segments += Lang.get().message(Message.GUI_SET_LIMIT_ORDER_DURATION_SEGMENT_NOT_SELECTED)
                        .replace("[DURATION]", String.valueOf(duration.getDisplay()))
                        .replace("[FEE]", String.valueOf(duration.getFee() * 100))
                        .replace("[MIN]", String.valueOf(duration.getMinimumFee()));
            }
        }

        for (String line : Lang.get().message(Message.GUI_SET_LIMIT_ORDER_TIME_LORE).replace("[DURATIONS]", segments.replace(".0", "")).split("\\n")) {
            Component componentLine = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(componentLine));
        }

        gui.setItem(
                config.getSetLimitOrderMenuTimeSlot(),
                MarketMenuManager.getInstance().generateItemStack(
                        config.getSetLimitOrderMenuTimeMaterial(),
                        BukkitComponentSerializer.legacy().serialize(timeComponent),
                        lore
                ));

        // Price

        Component priceComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_SET_LIMIT_ORDER_PRICE_NAME));

        lore.clear();

        String priceLore = Lang.get().message(price == 0 ? Message.GUI_SET_LIMIT_ORDER_PRICE_LORE_NOT_SET : Message.GUI_SET_LIMIT_ORDER_PRICE_LORE_SET)
                .replace("[PRICE]", Formatter.format(item.getCurrency(), price, Style.ROUND_BASIC));

        for (String line : priceLore.split("\\n")) {
            Component componentLine = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(componentLine));
        }

        gui.setItem(
                config.getSetLimitOrderMenuPriceSlot(),
                MarketMenuManager.getInstance().generateItemStack(
                        config.getSetLimitOrderMenuPriceMaterial(),
                        BukkitComponentSerializer.legacy().serialize(priceComponent),
                        lore
                ));

        // Quantity

        Component quantityComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_SET_LIMIT_ORDER_QUANTITY_NAME));

        lore.clear();

        String quantityLore = Lang.get().message(quantity == 0 ? Message.GUI_SET_LIMIT_ORDER_QUANTITY_LORE_NOT_SET : Message.GUI_SET_LIMIT_ORDER_QUANTITY_LORE_SET)
                .replace("[AMOUNT]", String.valueOf(quantity));

        for (String line : quantityLore.split("\\n")) {
            Component componentLine = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(componentLine));
        }

        gui.setItem(
                config.getSetLimitOrderMenuQuantitySlot(),
                MarketMenuManager.getInstance().generateItemStack(
                        config.getSetLimitOrderMenuQuantityMaterial(),
                        BukkitComponentSerializer.legacy().serialize(quantityComponent),
                        lore
                ));

        // Item (info)

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
                config.getSetLimitOrderMenuItemSlot(),
                itemStack
        );

        // Buy button

        Component buyComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_SET_LIMIT_ORDER_BUY_NAME));

        lore.clear();

        String buyLore;

        if (everythingSet()) {
            buyLore = Lang.get().message(Message.GUI_SET_LIMIT_ORDER_BUY_LORE)
                    .replace("[NAME]", item.getTaggedName())
                    .replace("[PRICE]", Formatter.format(item.getCurrency(), price, Style.ROUND_BASIC))
                    .replace("[AMOUNT]", String.valueOf(quantity))
                    .replace("[DURATION]", duration.getDisplay())
                    .replace("[DATE]", LocalDateTime.now().plusDays(duration.getDurationInDays()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .replace("[FEE]", Formatter.format(item.getCurrency(), Math.max(price * quantity * duration.getFee(), duration.getMinimumFee()), Style.ROUND_BASIC))
                    .replace("[HELD]", Formatter.format(item.getCurrency(), price * quantity, Style.ROUND_BASIC));
        } else {
            buyLore = Lang.get().message(Message.GUI_SET_LIMIT_ORDER_BUY_LORE_UNFILLED);
        }

        if (price > item.getPrice().getBuyPrice()) {
            buyLore = buyLore.replace("[CAUTION]", Lang.get().message(Message.GUI_SET_LIMIT_ORDER_BUY_CAUTION))
                    .replace("[PRICE]", Formatter.format(item.getCurrency(), item.getPrice().getBuyPrice(), Style.ROUND_BASIC));
        } else {
            buyLore = buyLore.replace("[CAUTION]", "");
        }

        for (String line : buyLore.split("\\n")) {
            Component componentLine = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(componentLine));
        }

        gui.setItem(
                config.getSetLimitOrderMenuConfirmBuySlot(),
                MarketMenuManager.getInstance().generateItemStack(
                        config.getSetLimitOrderMenuConfirmBuyMaterial(),
                        BukkitComponentSerializer.legacy().serialize(buyComponent),
                        lore
                ));

        // Sell button

        Component sellComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_SET_LIMIT_ORDER_SELL_NAME));

        lore.clear();

        String sellLore;

        if (everythingSet()) {
            sellLore = Lang.get().message(Message.GUI_SET_LIMIT_ORDER_SELL_LORE)
                    .replace("[NAME]", item.getTaggedName())
                    .replace("[PRICE]", Formatter.format(item.getCurrency(), price, Style.ROUND_BASIC))
                    .replace("[AMOUNT]", String.valueOf(quantity))
                    .replace("[DURATION]", duration.getDisplay())
                    .replace("[DATE]", LocalDateTime.now().plusDays(duration.getDurationInDays()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .replace("[FEE]", Formatter.format(item.getCurrency(), Math.max(price * quantity * duration.getFee(), duration.getMinimumFee()), Style.ROUND_BASIC));
        } else {
            sellLore = Lang.get().message(Message.GUI_SET_LIMIT_ORDER_SELL_LORE_UNFILLED);
        }

        if (price < item.getPrice().getSellPrice()) {
            sellLore = sellLore.replace("[CAUTION]", Lang.get().message(Message.GUI_SET_LIMIT_ORDER_SELL_CAUTION)
                    .replace("[PRICE]", Formatter.format(item.getCurrency(), item.getPrice().getSellPrice(), Style.ROUND_BASIC)));
        } else {
            sellLore = sellLore.replace("[CAUTION]", "");
        }

        for (String line : sellLore.split("\\n")) {
            Component componentLine = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(componentLine));
        }

        gui.setItem(
                config.getSetLimitOrderMenuConfirmSellSlot(),
                MarketMenuManager.getInstance().generateItemStack(
                        config.getSetLimitOrderMenuConfirmSellMaterial(),
                        BukkitComponentSerializer.legacy().serialize(sellComponent),
                        lore
                ));

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "set-limit-order-" + item.getIdentifier()));
        MarketMenuManager.getInstance().setMenuOfPlayer(player, this);
    }

    public void nextDuration() {

        List<Duration> durations = LimitOrdersManager.getInstance().getDurationOptions();

        int index = durations.indexOf(duration);

        if (index < durations.size() - 1) {
            duration = durations.get(index + 1);
        } else {
            duration = durations.get(0);
        }
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean everythingSet() {
        return (quantity != 0 && price != 0);
    }

    public Duration getDuration() { return duration; }

    public double getPrice() { return price; }

    public int getQuantity() { return quantity; }

}