package me.bounser.nascraft.inventorygui;


import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AlertsMenu implements MenuPage {

    private Inventory gui;

    private final Player player;

    public AlertsMenu(Player player) {
        this.player = player;

        open();
    }

    @Override
    public void open() {

        Config config = Config.getInstance();

        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_ALERTS_TITLE));

        gui = Bukkit.createInventory(null, config.getAlertsMenuSize(), BukkitComponentSerializer.legacy().serialize(title));

        // Back button

        if (config.getAlertsMenuBackEnabled()) {
            Component backComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_ALERTS_BACK_NAME));

            gui.setItem(
                    config.getAlertsMenuBackSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getAlertsMenuBackMaterial(),
                            BukkitComponentSerializer.legacy().serialize(backComponent)
                    ));
        }

        // Fillers

        Component fillerComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_FILLERS_NAME));

        ItemStack filler = MarketMenuManager.getInstance().generateItemStack(
                config.getAlertsMenuFillersMaterial(),
                BukkitComponentSerializer.legacy().serialize(fillerComponent)
        );

        for (int i : config.getAlertsMenuFillersSlots())
            gui.setItem(i, filler);


        // Alerts

        setAlerts();

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "alerts"));

    }

    @Override
    public void close() {

    }

    @Override
    public void update() {

        Config config = Config.getInstance();

        for (int slot : config.getAlertsMenuSlots())
            gui.clear(slot);

        setAlerts();

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "alerts"));
        MarketMenuManager.getInstance().setMenuOfPlayer(player, this);
    }

    public void setAlerts() {

        HashMap<Item, Double> alerts = DiscordAlerts.getInstance().getAlertsOfUUID(player.getUniqueId());

        if (alerts == null) return;

        List<Integer> slots = Config.getInstance().getAlertsMenuSlots();

        int i = 0;
        for (Item item : alerts.keySet()) {

            if (i > alerts.size()) break;

            List<String> itemLore = item.getItemStack().getItemMeta().getLore();

            if (itemLore == null) itemLore = new ArrayList<>();

            String buyLore = Lang.get().message(Message.GUI_ALERTS_LORE)
                    .replace("[PRICE]", Formatter.format(item.getCurrency(), Math.abs(alerts.get(item)), Style.ROUND_BASIC));

            for (String line : buyLore.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                itemLore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            ItemStack alert = MarketMenuManager.getInstance().generateItemStack(
                    item.getItemStack().getType(),
                    item.getFormattedName(),
                    itemLore
            );

            gui.setItem(slots.get(i), alert);

            i++;
        }
    }
}
