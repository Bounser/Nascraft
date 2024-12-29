package me.bounser.nascraft.inventorygui;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.limitorders.LimitOrder;
import me.bounser.nascraft.market.limitorders.LimitOrdersManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainMenu implements MenuPage {

    private final Player player;

    private Inventory gui;

    public MainMenu(Player player) {
        this.player = player;

        open();
    }

    @Override
    public void open() {

        Config config = Config.getInstance();

        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_MAIN_MENU_TITLE));

        gui = Bukkit.createInventory(null, config.getMainMenuSize(), BukkitComponentSerializer.legacy().serialize(title));

        List<String> lore = new ArrayList<>();

        boolean linked = LinkManager.getInstance().getUserDiscordID(player.getUniqueId()) != null;

        // Alerts

        if (config.getAlertsMenuEnabled()) {

            Component alert = MiniMessage.miniMessage().deserialize(Lang.get().message(linked ? Message.GUI_ALERTS_NAME_LINKED : Message.GUI_ALERTS_NAME_NOT_LINKED));

            String alertLore = Lang.get().message(linked ? Message.GUI_ALERTS_LORE_LINKED : Message.GUI_ALERTS_LORE_NOT_LINKED);

            if (linked) {

                HashMap<Item, Double> alerts = DiscordAlerts.getInstance().getAlertsOfUUID(player.getUniqueId());

                if (alerts == null || alerts.isEmpty()) {
                    alertLore = alertLore.replace("[ALERTS]", "0");
                } else {
                    alertLore = alertLore.replace("[ALERTS]", String.valueOf(alerts.keySet().size()));
                }
            }

            for (String line : alertLore.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            gui.setItem(
                    config.getAlertsSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getAlertsMaterial(linked),
                            BukkitComponentSerializer.legacy().serialize(alert),
                            lore
                    ));
        }

        // Limit orders

        if (config.getLimitOrdersMenuEnabled()) {

            Component limit = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_LIMIT_ORDERS_NAME));

            String limitLore = Lang.get().message(Message.GUI_LIMIT_ORDERS_LORE);

            List<LimitOrder> limitOrders = LimitOrdersManager.getInstance().getPlayerLimitOrders(player.getUniqueId());

            if (limitOrders == null || limitOrders.isEmpty()) {
                limitLore = limitLore
                        .replace("[TOTAL]", "0")
                        .replace("[TO-FILL]", "0")
                        .replace("[FILLED]", "0")
                        .replace("[EXPIRED]", "0");
            } else {

                int tofill = 0, expired = 0;

                for (LimitOrder limitOrder : limitOrders) {
                    if (!limitOrder.isCompleted()) tofill++;
                    if (limitOrder.isExpired()) expired++;
                }

                limitLore = limitLore
                        .replace("[TOTAL]", String.valueOf(limitOrders.size()))
                        .replace("[TO-FILL]", String.valueOf(tofill))
                        .replace("[FILLED]", String.valueOf(limitOrders.size()-tofill))
                        .replace("[EXPIRED]", String.valueOf(expired));
            }

            lore.clear();

            for (String line : limitLore.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            gui.setItem(
                    config.getLimitOrdersSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getLimitOrdersMaterial(),
                            BukkitComponentSerializer.legacy().serialize(limit),
                            lore
                    ));
        }

        // Information

        if (config.getInformationMenuEnabled()) {
            Component information = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_INFORMATION_NAME));

            lore.clear();
            for (String line : Lang.get().message(Message.GUI_INFORMATION_LORE).split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            gui.setItem(
                    config.getInformationSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getInformationMaterial(),
                            BukkitComponentSerializer.legacy().serialize(information),
                            lore
                    ));
        }

        // Portfolio

        if (config.getPortfolioMarketMenuEnabled()) {

            Component portfolio = MiniMessage.miniMessage().deserialize(Lang.get().message(
                    linked ? Message.GUI_PORTFOLIO_NAME_LINKED : Message.GUI_PORTFOLIO_NAME_NOT_LINKED));

            lore.clear();
            for (String line : Lang.get().message(linked ? Message.GUI_PORTFOLIO_LORE_LINKED : Message.GUI_PORTFOLIO_LORE_NOT_LINKED).split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            gui.setItem(
                    config.getPortfolioSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getPortfolioMaterial(linked),
                            BukkitComponentSerializer.legacy().serialize(portfolio),
                            lore
                    ));
        }

        // Trends

        if (config.getTrendsEnabled()) {

            Component trends = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_TRENDS_NAME));

            List<Item> moreMoved = MarketManager.getInstance().getMostTraded(3);

            String trendsLore = Lang.get().message(Message.GUI_TRENDS_LORE)
                            .replace("[POPULAR]", MarketManager.getInstance().getMostTraded(1).get(0).getTaggedName());

            int i = 1;

            for (Item item : moreMoved) {
                float lastChange = item.getPrice().getValueChangeLastHour();
                String movement = Lang.get().message(lastChange > 0 ? Message.GUI_TRENDS_POSITIVE : Message.GUI_TRENDS_NEGATIVE);
                trendsLore = trendsLore.replace("[" + i + "]", movement
                        .replace("[CHANGE]", String.valueOf(lastChange))
                        .replace("[NAME]", item.getTaggedName()));
                i++;
            }

            lore.clear();
            for (String line : trendsLore.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            gui.setItem(
                    config.getTrendsSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getTrendsMaterial(),
                            BukkitComponentSerializer.legacy().serialize(trends),
                            lore
                    ));
        }

        // Fillers

        Component fillerComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_FILLERS_NAME));

        HashMap<Material, List<Integer>> fillers = config.getMainMenuFillers();

        for (Material material : fillers.keySet()) {

            ItemStack filler = MarketMenuManager.getInstance().generateItemStack(
                    material,
                    BukkitComponentSerializer.legacy().serialize(fillerComponent)
            );

            for (int i : fillers.get(material))
                gui.setItem(i, filler);

        }

        // Categories

        List<Category> categories = MarketManager.getInstance().getCategories();

        int j = 0;

        for (int i : config.getCategoriesSlots()) {

            if (j < categories.size()) {

                Category category = categories.get(j);

                List<String> categoryList = new ArrayList<>();

                if (config.getSetCategorySegments() && category.getItems().size() < 25) {

                    for (Item item : category.getItems()) {

                        Component loreSegment = MiniMessage.miniMessage().deserialize(
                                Lang.get().message(Message.GUI_CATEGORIES_LORE_SEGMENT)
                        );

                        categoryList.add(BukkitComponentSerializer.legacy().serialize(loreSegment).replace("[ALIAS]", item.getFormattedName()));
                    }
                }

                ItemStack categoryItemStack = MarketMenuManager.getInstance().generateItemStack(
                        categories.get(j).getMaterial(),
                        category.getFormattedDisplayName(),
                        categoryList
                );

                ItemMeta meta = categoryItemStack.getItemMeta();

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

                categoryItemStack.setItemMeta(meta);

                gui.setItem(i, categoryItemStack);
            }
            j++;
        }

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "main-menu"));
    }

    @Override
    public void close() {

        player.closeInventory();

        if (player.hasMetadata("NascraftMenu"))
            player.removeMetadata("NascraftMenu", Nascraft.getInstance());
    }

    @Override
    public void update() {

    }
}
