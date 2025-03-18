package me.bounser.nascraft.inventorygui;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.inventorygui.Portfolio.PortfolioInventory;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.inventorygui.MiniChart.InfoMenu;
import me.bounser.nascraft.managers.DebtManager;
import me.bounser.nascraft.managers.InventoryManager;
import me.bounser.nascraft.managers.MoneyManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.limitorders.LimitOrder;
import me.bounser.nascraft.market.limitorders.LimitOrdersManager;
import me.bounser.nascraft.market.limitorders.OrderType;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InventoryListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        Player player = (Player) event.getWhoClicked();

        if (!player.hasMetadata("NascraftMenu")) return;

        event.setCancelled(true);

        if (!player.hasPermission("nascraft.market")) return;

        int slot = event.getRawSlot();
        Config config = Config.getInstance();

        String metadata = player.getMetadata("NascraftMenu").get(0).asString();

        if (metadata.startsWith("category-menu-")) {

            Category category = MarketManager.getInstance().getCategoryFromIdentifier(metadata.substring(14));

            if (category == null) return;

            int page;

            if (player.hasMetadata("NascraftPage")) {
                page = player.getMetadata("NascraftPage").get(0).asInt();
            } else {
                page = 0;
            }

            if (config.getCategoryBackEnabled() && slot == config.getCategoryBackSlot()) {

                if (page == 0) {

                    MarketMenuManager.getInstance().setMenuOfPlayer(player, new MainMenu(player));

                } else if (category.getItems().size() > config.getCategoryItemsSlots().size()) {

                    player.setMetadata("NascraftPage", new FixedMetadataValue(Nascraft.getInstance(), player.getMetadata("NascraftPage").get(0).asInt() - 1));
                    MarketMenuManager.getInstance().getMenuFromPlayer(player).update();

                }
                return;
            }

            if (category.getItems().size() > config.getCategoryItemsSlots().size() * (1 + page) && slot == config.getCategoryNextSlot()) {

                player.setMetadata("NascraftPage", new FixedMetadataValue(Nascraft.getInstance(), player.getMetadata("NascraftPage").get(0).asInt() + 1));
                MarketMenuManager.getInstance().getMenuFromPlayer(player).update();
                return;
            }

            Item item = null;

            List<Item> items = category.getItems();

            List<Integer> itemSlots = config.getCategoryItemsSlots();

            if (itemSlots.contains(event.getRawSlot()) && items.size() > (itemSlots.indexOf(event.getRawSlot()) + page * itemSlots.size())) {
                item = items.get(itemSlots.indexOf(event.getRawSlot()) + page * itemSlots.size());
            }

            if (item == null) return;

            MarketMenuManager.getInstance().setMenuOfPlayer(player, new BuySellMenu(player, item));
            return;
        }

        if (metadata.startsWith("item-menu-")) {

            Item item = MarketManager.getInstance().getItem(metadata.substring(10));

            if (item == null) return;

            if (config.getBuySellBackEnabled() && slot == config.getBuySellBackSlot()) {
                MarketMenuManager.getInstance().setMenuOfPlayer(player, new CategoryMenu(player, item.getCategory()));
                return;
            }

            HashMap<Integer, Integer> buyButtons = config.getBuySellBuySlots();

            if (buyButtons.containsValue(slot)) {

                int amount = 0;

                for (int value : buyButtons.keySet())
                    if (buyButtons.get(value) == slot)
                        amount = value;

                item.buy(amount, player.getUniqueId(), true);

                MarketMenuManager.getInstance().getMenuFromPlayer(player).update();

                return;
            }

            HashMap<Integer, Integer> sellButtons = config.getBuySellSellSlots();

            if (sellButtons.containsValue(slot)) {

                int amount = 0;

                for (int value : sellButtons.keySet())
                    if (sellButtons.get(value) == slot)
                        amount = value;

                item.sell(amount, player.getUniqueId(), true);

                MarketMenuManager.getInstance().getMenuFromPlayer(player).update();

                return;
            }

            if (config.getInfoBuySellEnabled() && config.getInfoBuySellSlot() == slot) {

                Item itemToPlot = MarketManager.getInstance().getItem(metadata.substring(10));

                MarketMenuManager.getInstance().setMenuOfPlayer(player, new InfoMenu(player, itemToPlot));

                return;
            }

            if (config.getAlertsBuySellSlot() == slot) {

                String userId = LinkManager.getInstance().getUserDiscordID(player.getUniqueId());

                if (userId == null) return;

                HashMap<Item, Double> alerts = DiscordAlerts.getInstance().getAlertsOfUUID(player.getUniqueId());

                if (alerts != null && alerts.containsKey(item)) {

                    DiscordAlerts.getInstance().removeAlert(userId, item);

                    MarketMenuManager.getInstance().getMenuFromPlayer(player).update();

                    return;
                }

                new AnvilGUI.Builder()
                        .onClick((anvilSlot, stateSnapshot) -> {

                            Pattern pattern = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+");
                            Matcher matcher = pattern.matcher(stateSnapshot.getText());

                            if (matcher.find()) {
                                String doubleString = matcher.group();

                                double value = Double.parseDouble(doubleString);

                                return switch (DiscordAlerts.getInstance().setAlert(userId, item.getIdentifier(), value)) {
                                    case NOT_VALID ->
                                            List.of(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.ANVIL_ALERT_INVALID)));
                                    case LIMIT_REACHED ->
                                            List.of(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.ANVIL_ALERT_LIMIT_REACHED)));
                                    case REPEATED ->
                                            List.of(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.ANVIL_ALERT_REPEATED)));
                                    default ->
                                            Arrays.asList(
                                            AnvilGUI.ResponseAction.close(),
                                            AnvilGUI.ResponseAction.run(() -> {
                                                MarketMenuManager.getInstance().setMenuOfPlayer(player, new BuySellMenu(player, item));
                                                player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "item-menu-" + item.getIdentifier()));
                                            })
                                    );
                                };

                            } else {
                                return List.of(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.ANVIL_ALERT_INVALID_PRICE)));
                            }

                        })
                        .text(Lang.get().message(Message.ANVIL_ALERT_TEXT))
                        .title(Lang.get().message(Message.ANVIL_ALERT_TITLE).replace("[ALIAS]", item.getName()))
                        .plugin(Nascraft.getInstance())
                        .open(player);

                return;
            }

            if (config.getLimitOrdersEnabled() && config.getLimitOrdersBuySellEnabled() && config.getLimitOrdersBuySellSlot() == slot) {

                Item limitItem = MarketManager.getInstance().getItem(metadata.substring(10));

                List<LimitOrder> orders = LimitOrdersManager.getInstance().getPlayerLimitOrders(player.getUniqueId());

                LimitOrder order = null;

                for (LimitOrder limitOrder : orders)
                    if (limitOrder.getItem().equals(limitItem)) order = limitOrder;

                if (order == null)  {
                    MarketMenuManager.getInstance().setMenuOfPlayer(player, new SetLimitOrderMenu(player, limitItem));
                    return;
                }

                if (order.getOrderType().equals(OrderType.LIMIT_BUY)) {

                    double compensation = ((order.getToComplete()) * order.getPrice()) - order.getCost();

                    if (order.getCompleted() > 0) {

                        if (!InventoryManager.checkInventory(player, true, order.getItem().getItemStack(), order.getCompleted())) return;

                        InventoryManager.addItemsToInventory(player, order.getItem().getItemStack(), order.getCompleted());

                        if (compensation == 0) {

                            Lang.get().message(player, Lang.get().message(Message.GUI_LIMIT_ORDERS_RECEIVED_ITEMS)
                                    .replace("[AMOUNT]", String.valueOf(order.getCompleted()))
                                    .replace("[NAME]", order.getItem().getTaggedName())
                            );

                        } else {

                            Lang.get().message(player, Lang.get().message(Message.GUI_LIMIT_ORDERS_RECEIVED_MONEY_ITEMS)
                                    .replace("[AMOUNT]", String.valueOf(order.getCompleted()))
                                    .replace("[NAME]", order.getItem().getTaggedName())
                                    .replace("[MONEY]", Formatter.format(order.getItem().getCurrency(), ((order.getToComplete()) * order.getPrice()) - order.getCost(), Style.ROUND_BASIC))
                            );

                            MoneyManager.getInstance().deposit(player, order.getItem().getCurrency(), ((order.getToComplete()) * order.getPrice()) - order.getCost(), 0);
                        }
                    } else {

                        Lang.get().message(player, Lang.get().message(Message.GUI_LIMIT_ORDERS_RECEIVED_MONEY)
                                .replace("[MONEY]", Formatter.format(order.getItem().getCurrency(), ((order.getToComplete()) * order.getPrice()) - order.getCost(), Style.ROUND_BASIC))
                        );

                        MoneyManager.getInstance().deposit(player, order.getItem().getCurrency(), ((order.getToComplete()) * order.getPrice()) - order.getCost(), 0);
                    }

                } else {

                    if (order.getCompleted() != order.getToComplete()) {

                        if (!InventoryManager.checkInventory(player, true, order.getItem().getItemStack(), order.getToComplete() - order.getCompleted()))
                            return;

                        InventoryManager.addItemsToInventory(player, order.getItem().getItemStack(), order.getToComplete() - order.getCompleted());

                        if (order.getCompleted() == 0) {

                            Lang.get().message(player, Lang.get().message(Message.GUI_LIMIT_ORDERS_RECEIVED_ITEMS)
                                    .replace("[AMOUNT]", String.valueOf(order.getToComplete()))
                                    .replace("[NAME]", order.getItem().getTaggedName())
                            );

                        } else {

                            Lang.get().message(player, Lang.get().message(Message.GUI_LIMIT_ORDERS_RECEIVED_MONEY_ITEMS)
                                    .replace("[AMOUNT]", String.valueOf(order.getToComplete() - order.getCompleted()))
                                    .replace("[NAME]", order.getItem().getTaggedName())
                                    .replace("[MONEY]", Formatter.format(order.getItem().getCurrency(), order.getCost(), Style.ROUND_BASIC))
                            );

                            MoneyManager.getInstance().deposit(player, order.getItem().getCurrency(), order.getCost(), 0);
                        }

                    } else {

                        Lang.get().message(player, Lang.get().message(Message.GUI_LIMIT_ORDERS_RECEIVED_MONEY)
                                .replace("[MONEY]", Formatter.format(order.getItem().getCurrency(), order.getCost(), Style.ROUND_BASIC))
                        );

                        MoneyManager.getInstance().deposit(player, order.getItem().getCurrency(), order.getCost(), 0);
                    }
                }

                LimitOrdersManager.getInstance().deleteLimitOrder(order);

                MarketMenuManager.getInstance().setMenuOfPlayer(player, new BuySellMenu(player, item));

                return;
            }

            return;
        }

        if (metadata.startsWith("set-limit-order-")) {

            Item item = MarketManager.getInstance().getItem(metadata.substring(16));

            SetLimitOrderMenu menu = (SetLimitOrderMenu) MarketMenuManager.getInstance().getMenuFromPlayer(player);

            if (config.getSetLimitOrderMenuBackEnabled() && config.getSetLimitOrderMenuBackSlot() == slot) {
                MarketMenuManager.getInstance().setMenuOfPlayer(player, new BuySellMenu(player, item));
                return;
            }

            if (config.getSetLimitOrderMenuTimeSlot() == slot) {
                menu.nextDuration();
                menu.update();
                return;
            }

            if (config.getSetLimitOrderMenuPriceSlot() == slot) {

                new AnvilGUI.Builder()
                        .onClick((anvilSlot, stateSnapshot) -> {

                            Pattern pattern = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+");
                            Matcher matcher = pattern.matcher(stateSnapshot.getText());

                            if (matcher.find()) {
                                String doubleString = matcher.group();

                                double value = Math.abs(Double.parseDouble(doubleString));

                                value = Math.min(value, item.getCurrency().getTopLimit());
                                value = Math.max(value, item.getCurrency().getLowLimit());

                                menu.setPrice(value);

                                return Arrays.asList(
                                        AnvilGUI.ResponseAction.close(),
                                        AnvilGUI.ResponseAction.run(menu::open)
                                );

                            } else {
                                return List.of(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.ANVIL_LIMIT_PRICE_INVALID)));
                            }

                        })
                        .text(Lang.get().message(Message.ANVIL_LIMIT_PRICE_TEXT))
                        .title(Lang.get().message(Message.ANVIL_LIMIT_PRICE_TITLE).replace("[NAME]", item.getName()))
                        .plugin(Nascraft.getInstance())
                        .open(player);
                return;
            }

            if (config.getSetLimitOrderMenuQuantitySlot() == slot) {

                new AnvilGUI.Builder()
                        .onClick((anvilSlot, stateSnapshot) -> {

                            Pattern pattern = Pattern.compile("[-+]?\\d+");
                            Matcher matcher = pattern.matcher(stateSnapshot.getText());

                            if (matcher.find()) {
                                String intString = matcher.group();

                                int value = Math.abs(Integer.parseInt(intString));

                                if (value == 0 || value > config.getMaxLimitOrderSize())
                                    return List.of(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.ANVIL_LIMIT_QUANTITY_MAX_REACHED)
                                            .replace("[MAX]", String.valueOf(config.getMaxLimitOrderSize()))));

                                menu.setQuantity(value);

                                return Arrays.asList(
                                        AnvilGUI.ResponseAction.close(),
                                        AnvilGUI.ResponseAction.run(menu::open)
                                );

                            } else {
                                return List.of(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.ANVIL_LIMIT_QUANTITY_INVALID)));
                            }

                        })
                        .text(Lang.get().message(Message.ANVIL_LIMIT_QUANTITY_TEXT))
                        .title(Lang.get().message(Message.ANVIL_LIMIT_QUANTITY_TITLE).replace("[NAME]", item.getName()))
                        .plugin(Nascraft.getInstance())
                        .open(player);
                return;
            }

            if (config.getSetLimitOrderMenuConfirmBuySlot() == slot) {

                if (menu.everythingSet()) {

                    if (LimitOrdersManager.getInstance().getPlayerLimitOrders(player.getUniqueId()).size() >= config.getMaxLimitOrdersPerPlayer()) {
                        Lang.get().message(player, Message.LIMIT_MAX_ORDERS_REACHED);
                        return;
                    }

                    double price = menu.getPrice() * menu.getQuantity() + (Math.max(menu.getPrice() * menu.getQuantity() * menu.getDuration().getFee(), menu.getDuration().getMinimumFee()));

                    if (!MoneyManager.getInstance().hasEnoughMoney(player, item.getCurrency(), price)) {
                        Lang.get().message(player, Message.NOT_ENOUGH_MONEY);
                        return;
                    }

                    MoneyManager.getInstance().withdraw(player, item.getCurrency(), menu.getPrice() * menu.getQuantity(), 0);

                    MoneyManager.getInstance().withdraw(player, item.getCurrency(), price - menu.getPrice() * menu.getQuantity(), 1);

                    LimitOrdersManager.getInstance().registerNewLimitOrder(
                            player.getUniqueId(),
                            LocalDateTime.now().plusDays(menu.getDuration().getDurationInDays()),
                            item,
                            1,
                            menu.getPrice(),
                            menu.getQuantity()
                    );

                    Lang.get().message(Message.LIMIT_BUY_MESSAGE, Formatter.format(item.getCurrency(), menu.getPrice(), Style.ROUND_BASIC), String.valueOf(menu.getQuantity()), item.getName());

                    MarketMenuManager.getInstance().setMenuOfPlayer(player, new BuySellMenu(player, item));
                    return;
                }
                return;
            }

            if (config.getSetLimitOrderMenuConfirmSellSlot() == slot) {

                if (menu.everythingSet()) {

                    if (LimitOrdersManager.getInstance().getPlayerLimitOrders(player.getUniqueId()).size() >= config.getMaxLimitOrdersPerPlayer()) {
                        Lang.get().message(player, Message.LIMIT_MAX_ORDERS_REACHED);
                        return;
                    }

                    if (!InventoryManager.containsAtLeast(player, item.getItemStack(), menu.getQuantity())) {
                        Lang.get().message(player, Message.NOT_ENOUGH_ITEMS);
                        return;
                    }

                    double fee = Math.max(menu.getPrice() * menu.getQuantity() * menu.getDuration().getFee(), menu.getDuration().getMinimumFee());

                    if (!MoneyManager.getInstance().hasEnoughMoney(player, item.getCurrency(), fee)) {
                        Lang.get().message(player, Message.NOT_ENOUGH_MONEY);
                        return;
                    }

                    MoneyManager.getInstance().withdraw(player, item.getCurrency(), fee, 1);

                    InventoryManager.removeItems(player, item.getItemStack(), menu.getQuantity());

                    LimitOrdersManager.getInstance().registerNewLimitOrder(
                            player.getUniqueId(),
                            LocalDateTime.now().plusDays(menu.getDuration().getDurationInDays()),
                            item,
                            0,
                            menu.getPrice(),
                            menu.getQuantity()
                    );

                    Lang.get().message(Message.LIMIT_SELL_MESSAGE, Formatter.format(item.getCurrency(), menu.getPrice(), Style.ROUND_BASIC), String.valueOf(menu.getQuantity()), item.getName());

                    MarketMenuManager.getInstance().setMenuOfPlayer(player, new BuySellMenu(player, item));
                    return;
                }
                return;
            }
            return;
        }

        switch (metadata) {

            case "main-menu":

                if (config.getAlertsMenuEnabled() && slot == config.getAlertsSlot()) {

                    if (LinkManager.getInstance().getUserDiscordID(player.getUniqueId()) == null) return;

                    MarketMenuManager.getInstance().setMenuOfPlayer(player, new AlertsMenu(player));
                    return;
                }

                if (config.getLimitOrdersMenuEnabled() && slot == config.getLimitOrdersSlot()) {
                    MarketMenuManager.getInstance().setMenuOfPlayer(player, new LimitOrdersMenu(player));
                    return;
                }

                if (config.getPortfolioMarketMenuEnabled() && slot == config.getPortfolioSlot()) {

                    Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TITLE));

                    Inventory inventory = Bukkit.createInventory(player, 45, BukkitComponentSerializer.legacy().serialize(title));
                    player.openInventory(inventory);
                    player.setMetadata("NascraftPortfolio", new FixedMetadataValue(Nascraft.getInstance(),false));

                    PortfolioInventory.getInstance().updatePortfolioInventory(player);

                    return;
                }

                List<Category> categories = MarketManager.getInstance().getCategories();

                List<Integer> categorySlots = Config.getInstance().getCategoriesSlots();

                if (!categorySlots.contains(slot)) return;

                if (!categorySlots.contains(event.getRawSlot())) return;

                int catIndex = categorySlots.indexOf(event.getRawSlot());

                if (categories.size() <= catIndex) return;

                Category category = categories.get(catIndex);

                if (category == null) return;

                MarketMenuManager.getInstance().setMenuOfPlayer(player, new CategoryMenu(player, category));

                return;

            case "alerts":

                if (config.getAlertsMenuBackEnabled() && config.getAlertsMenuBackSlot() == slot) {
                    MarketMenuManager.getInstance().setMenuOfPlayer(player, new MainMenu(player));
                    return;
                }

                if (config.getAlertsMenuSlots().contains(slot)) {

                    int index = config.getAlertsMenuSlots().indexOf(slot);

                    String userId = LinkManager.getInstance().getUserDiscordID(player.getUniqueId());

                    if (userId == null) return;

                    List<Item> items = new ArrayList<>(DiscordAlerts.getInstance().getAlerts().get(userId).keySet());

                    if (items.get(index) == null) return;

                    DiscordAlerts.getInstance().removeAlert(userId, items.get(index));

                    MenuPage menu = MarketMenuManager.getInstance().getMenuFromPlayer(player);

                    if (menu != null) menu.update();
                }

            case "limitorders":

                if (config.getLimitOrdersMenuBackEnabled() && config.getLimitOrdersMenuBackSlot() == slot) {
                    MarketMenuManager.getInstance().setMenuOfPlayer(player, new MainMenu(player));
                    return;
                }

                if (config.getLimitOrdersMenuSlots().contains(slot)) {

                    int index = config.getLimitOrdersMenuSlots().indexOf(slot);

                    List<LimitOrder> orders = LimitOrdersManager.getInstance().getPlayerLimitOrders(player.getUniqueId());

                    if (orders == null || orders.isEmpty()) return;

                    LimitOrder order = orders.get(index);

                    if (order == null) return;

                    if (order.getOrderType().equals(OrderType.LIMIT_BUY)) {

                        double compensation = ((order.getToComplete()) * order.getPrice()) - order.getCost();

                        if (order.getCompleted() > 0) {

                            if (!InventoryManager.checkInventory(player, true, order.getItem().getItemStack(), order.getCompleted())) return;

                            InventoryManager.addItemsToInventory(player, order.getItem().getItemStack(), order.getCompleted());

                            if (compensation == 0) {

                                Lang.get().message(player, Lang.get().message(Message.GUI_LIMIT_ORDERS_RECEIVED_ITEMS)
                                        .replace("[AMOUNT]", String.valueOf(order.getCompleted()))
                                        .replace("[NAME]", order.getItem().getTaggedName())
                                );

                            } else {

                                Lang.get().message(player, Lang.get().message(Message.GUI_LIMIT_ORDERS_RECEIVED_MONEY_ITEMS)
                                        .replace("[AMOUNT]", String.valueOf(order.getCompleted()))
                                        .replace("[NAME]", order.getItem().getTaggedName())
                                        .replace("[MONEY]", Formatter.format(order.getItem().getCurrency(), ((order.getToComplete()) * order.getPrice()) - order.getCost(), Style.ROUND_BASIC))
                                );

                                MoneyManager.getInstance().deposit(player, order.getItem().getCurrency(), ((order.getToComplete()) * order.getPrice()) - order.getCost(), 0);
                            }
                        } else {

                            Lang.get().message(player, Lang.get().message(Message.GUI_LIMIT_ORDERS_RECEIVED_MONEY)
                                    .replace("[MONEY]", Formatter.format(order.getItem().getCurrency(), ((order.getToComplete()) * order.getPrice()) - order.getCost(), Style.ROUND_BASIC))
                            );

                            MoneyManager.getInstance().deposit(player, order.getItem().getCurrency(), ((order.getToComplete()) * order.getPrice()) - order.getCost(), 0);
                        }

                    } else {

                        if (order.getCompleted() != order.getToComplete()) {

                            if (!InventoryManager.checkInventory(player, true, order.getItem().getItemStack(), order.getToComplete() - order.getCompleted()))
                                return;

                            InventoryManager.addItemsToInventory(player, order.getItem().getItemStack(), order.getToComplete() - order.getCompleted());

                            if (order.getCompleted() == 0) {

                                Lang.get().message(player, Lang.get().message(Message.GUI_LIMIT_ORDERS_RECEIVED_ITEMS)
                                        .replace("[AMOUNT]", String.valueOf(order.getToComplete()))
                                        .replace("[NAME]", order.getItem().getTaggedName())
                                );

                            } else {

                                Lang.get().message(player, Lang.get().message(Message.GUI_LIMIT_ORDERS_RECEIVED_MONEY_ITEMS)
                                        .replace("[AMOUNT]", String.valueOf(order.getToComplete() - order.getCompleted()))
                                        .replace("[NAME]", order.getItem().getTaggedName())
                                        .replace("[MONEY]", Formatter.format(order.getItem().getCurrency(), order.getCost(), Style.ROUND_BASIC))
                                );

                                MoneyManager.getInstance().deposit(player, order.getItem().getCurrency(), order.getCost(), 0);
                            }

                        } else {

                            Lang.get().message(player, Lang.get().message(Message.GUI_LIMIT_ORDERS_RECEIVED_MONEY)
                                    .replace("[MONEY]", Formatter.format(order.getItem().getCurrency(), order.getCost(), Style.ROUND_BASIC))
                            );

                            MoneyManager.getInstance().deposit(player, order.getItem().getCurrency(), order.getCost(), 0);
                        }
                    }

                    LimitOrdersManager.getInstance().deleteLimitOrder(order);

                    MenuPage menu = MarketMenuManager.getInstance().getMenuFromPlayer(player);

                    if (menu != null) menu.update();
                }

            case "debt":

                if (config.getDebtBackEnabled() && config.getDebtBackSlot() == slot) {
                    Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TITLE));

                    Inventory inventory = Bukkit.createInventory(player, 45, BukkitComponentSerializer.legacy().serialize(title));
                    player.openInventory(inventory);
                    player.setMetadata("NascraftPortfolio", new FixedMetadataValue(Nascraft.getInstance(),false));

                    PortfolioInventory.getInstance().updatePortfolioInventory(player);
                    return;
                }

                double debt = DebtManager.getInstance().getDebtOfPlayer(player.getUniqueId());

                if (config.getDebtRepayAllEnabled() && config.getDebtRepayAllSlot() == slot) {

                    if (MoneyManager.getInstance().hasEnoughMoney(player, CurrenciesManager.getInstance().getDefaultCurrency(), debt)) {

                        MoneyManager.getInstance().simpleWithdraw(player, CurrenciesManager.getInstance().getDefaultCurrency(), debt);
                        DebtManager.getInstance().decreaseDebt(player.getUniqueId(), debt);

                        MarketMenuManager.getInstance().getMenuFromPlayer(player).update();

                        Lang.get().message(player, Lang.get().message(Message.PORTFOLIO_DEBT_REPAYED_ALL)
                                .replace("[AMOUNT]", Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), debt, Style.ROUND_BASIC)));

                    } else {
                        Lang.get().message(player, Lang.get().message(Message.NOT_ENOUGH_MONEY));
                    }
                    return;
                }

                if (config.getDebtRepayEnabled() && config.getDebtRepaySlot() == slot) {

                    new AnvilGUI.Builder()
                            .onClick((anvilSlot, stateSnapshot) -> {

                                Pattern pattern = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+");
                                Matcher matcher = pattern.matcher(stateSnapshot.getText());

                                if (matcher.find()) {
                                    String intString = matcher.group();

                                    double value = Math.abs(Double.parseDouble(intString));

                                    if (value == 0 || value > debt)
                                        return List.of(AnvilGUI.ResponseAction.replaceInputText(String.valueOf(Formatter.roundToDecimals(debt, CurrenciesManager.getInstance().getDefaultCurrency().getDecimalPrecission()))));

                                    if (!MoneyManager.getInstance().hasEnoughMoney(player, CurrenciesManager.getInstance().getDefaultCurrency(), value))
                                        return List.of(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.PORTFOLIO_DEBT_ANVIL_NOT_ENOUGH)));

                                    MoneyManager.getInstance().simpleWithdraw(player, CurrenciesManager.getInstance().getDefaultCurrency(), value);
                                    DebtManager.getInstance().decreaseDebt(player.getUniqueId(), value);

                                    Lang.get().message(player, Lang.get().message(Message.PORTFOLIO_DEBT_REPAYED)
                                            .replace("[AMOUNT]", Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), value, Style.ROUND_BASIC))
                                            .replace("[DEBT]", Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), debt-value, Style.ROUND_BASIC)));

                                    return Arrays.asList(AnvilGUI.ResponseAction.close());

                                } else {
                                    return List.of(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.PORTFOLIO_DEBT_ANVIL_INVALID)));
                                }

                            })
                            .text(Lang.get().message(Message.PORTFOLIO_DEBT_ANVIL_REPAY))
                            .title(Lang.get().message(Message.PORTFOLIO_DEBT_ANVIL_REPAY_TITLE))
                            .plugin(Nascraft.getInstance())
                            .open(player);
                    return;
                }

                if (config.getDebtMaxLoanEnabled() && config.getDebtMaxLoanSlot() == slot) {

                    double loan = Math.min(DebtManager.getInstance().getMaximumLoan(player.getUniqueId()) * 0.9, config.getLoansMaxSize());

                    loan -= debt;

                    if (loan > 0) {

                        if (loan < config.getLoansMinSize()) {
                            Lang.get().message(player, Lang.get().message(Message.PORTFOLIO_DEBT_MIN_LOAN)
                                    .replace("[AMOUNT]", Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), config.getLoansMinSize(), Style.ROUND_BASIC)));
                            return;
                        }

                        DebtManager.getInstance().increaseDebt(player.getUniqueId(), loan);
                        MoneyManager.getInstance().simpleDeposit(player, CurrenciesManager.getInstance().getDefaultCurrency(), loan);

                        MarketMenuManager.getInstance().getMenuFromPlayer(player).update();

                        Lang.get().message(player, Lang.get().message(Message.PORTFOLIO_DEBT_TAKE_LOAN)
                                .replace("[AMOUNT]", Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), loan, Style.ROUND_BASIC))
                                .replace("[DEBT]", Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), loan + debt , Style.ROUND_BASIC)));

                    } else {
                        Lang.get().message(player, Lang.get().message(Message.PORTFOLIO_DEBT_NO_COLLATERAL));
                    }
                    return;
                }

                if (config.getDebtCustomEnabled() && config.getDebtCustomSlot() == slot) {

                    new AnvilGUI.Builder()
                            .onClick((anvilSlot, stateSnapshot) -> {

                                Pattern pattern = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+");
                                Matcher matcher = pattern.matcher(stateSnapshot.getText());

                                if (matcher.find()) {
                                    String intString = matcher.group();

                                    double value = Math.abs(Double.parseDouble(intString));
                                    double maxLoan = Math.min(DebtManager.getInstance().getMaximumLoan(player.getUniqueId()), config.getLoansMaxSize());
                                    double minLoan = config.getLoansMinSize();

                                    if ((value + debt) < minLoan)
                                        return List.of(AnvilGUI.ResponseAction.replaceInputText(String.valueOf(Formatter.roundToDecimals(minLoan, CurrenciesManager.getInstance().getDefaultCurrency().getDecimalPrecission()))));

                                    if (value == 0 || (value + debt) > maxLoan)
                                        return List.of(AnvilGUI.ResponseAction.replaceInputText(String.valueOf(Formatter.roundToDecimals(maxLoan-debt, CurrenciesManager.getInstance().getDefaultCurrency().getDecimalPrecission()))));

                                    MoneyManager.getInstance().simpleDeposit(player, CurrenciesManager.getInstance().getDefaultCurrency(), value);
                                    DebtManager.getInstance().increaseDebt(player.getUniqueId(), value);

                                    Lang.get().message(player, Lang.get().message(Message.PORTFOLIO_DEBT_TAKE_LOAN)
                                            .replace("[AMOUNT]", Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), value, Style.ROUND_BASIC))
                                            .replace("[DEBT]", Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), value + debt, Style.ROUND_BASIC)));

                                    return Arrays.asList(AnvilGUI.ResponseAction.close());

                                } else {
                                    return List.of(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.PORTFOLIO_DEBT_ANVIL_INVALID)));
                                }

                            })
                            .text(Lang.get().message(Message.PORTFOLIO_DEBT_ANVIL_CUSTOM))
                            .title(Lang.get().message(Message.PORTFOLIO_DEBT_ANVIL_CUSTOM_TITLE))
                            .plugin(Nascraft.getInstance())
                            .open(player);
                    return;
                }

            case "top":

                if (config.getTopBackEnabled() && config.getTopBackSlot() == slot) {
                    Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TITLE));

                    Inventory inventory = Bukkit.createInventory(player, 45, BukkitComponentSerializer.legacy().serialize(title));
                    player.openInventory(inventory);
                    player.setMetadata("NascraftPortfolio", new FixedMetadataValue(Nascraft.getInstance(),false));

                    PortfolioInventory.getInstance().updatePortfolioInventory(player);
                    return;
                }

                return;
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {

        Player player = (Player) event.getPlayer();

        MarketMenuManager.getInstance().removeMenuFromPlayer(player);

        if (player.hasMetadata("NascraftMenu")) {
            player.removeMetadata("NascraftMenu", Nascraft.getInstance());
        }

        if (player.hasMetadata("NascraftQuantity")) {
            player.removeMetadata("NascraftQuantity", Nascraft.getInstance());
        }

        if (player.hasMetadata("NascraftPage")) {
            player.removeMetadata("NascraftPage", Nascraft.getInstance());
        }
    }
}
