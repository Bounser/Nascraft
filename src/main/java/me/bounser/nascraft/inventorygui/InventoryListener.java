package me.bounser.nascraft.inventorygui;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.discord.DiscordInventoryInGame;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.market.MarketManager;
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

            if (config.getAlertsBuySellSlot() == slot) {

                String userId = LinkManager.getInstance().getUserDiscordID(player.getUniqueId());

                if (userId == null) return;

                HashMap<Item, Float> alerts = DiscordAlerts.getInstance().getAlertsOfUUID(player.getUniqueId());

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
                                String floatString = matcher.group();

                                float value = Float.parseFloat(floatString);

                                switch (DiscordAlerts.getInstance().setAlert(userId, item.getIdentifier(), value)) {

                                    case NOT_VALID:
                                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.ANVIL_ALERT_INVALID)));
                                    case LIMIT_REACHED:
                                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.ANVIL_ALERT_LIMIT_REACHED)));
                                    case REPEATED:
                                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.ANVIL_ALERT_REPEATED)));

                                    case SUCCESS:
                                    default:
                                        return Arrays.asList(
                                                AnvilGUI.ResponseAction.close(),
                                                AnvilGUI.ResponseAction.run(() -> {
                                                    MarketMenuManager.getInstance().setMenuOfPlayer(player, new BuySellMenu(player, item));
                                                    player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(),"item-menu-" + item.getIdentifier()));
                                                })
                                        );
                                }

                            } else {
                                return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText(Lang.get().message(Message.ANVIL_ALERT_INVALID_PRICE)));
                            }

                        })
                        .text(Lang.get().message(Message.ANVIL_ALERT_TEXT))
                        .title(Lang.get().message(Message.ANVIL_ALERT_TITLE).replace("[ALIAS]", item.getName()))
                        .plugin(Nascraft.getInstance())
                        .open(player);

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

                if (config.getDiscordMarketMenuEnabled() && slot == config.getDiscordSlot()) {

                    if (LinkManager.getInstance().getUserDiscordID(player.getUniqueId()) == null) return;

                    Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.DISINV_TITLE));

                    Inventory inventory = Bukkit.createInventory(player, 45, BukkitComponentSerializer.legacy().serialize(title));
                    player.openInventory(inventory);
                    player.setMetadata("NascraftDiscordInventory", new FixedMetadataValue(Nascraft.getInstance(),true));

                    DiscordInventoryInGame.getInstance().updateDiscordInventory(player);

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
