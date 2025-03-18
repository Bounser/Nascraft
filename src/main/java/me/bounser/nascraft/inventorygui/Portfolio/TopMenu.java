package me.bounser.nascraft.inventorygui.Portfolio;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.inventorygui.MarketMenuManager;
import me.bounser.nascraft.inventorygui.MenuPage;
import me.bounser.nascraft.managers.DebtManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.portfolio.Portfolio;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

public class TopMenu implements MenuPage {

    private Inventory gui;

    private final Player player;

    public TopMenu(Player player) {
        this.player = player;

        open();
    }

    @Override
    public void open() {

        Config config = Config.getInstance();

        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TOP_TITLE));

        gui = Bukkit.createInventory(null, config.getTopSize(), BukkitComponentSerializer.legacy().serialize(title));

        // Back button

        if (config.getTopBackEnabled()) {
            Component backComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TOP_BACK_NAME));

            gui.setItem(
                    config.getTopBackSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getTopBackMaterial(),
                            BukkitComponentSerializer.legacy().serialize(backComponent)
                    ));
        }

        // Fillers

        HashMap<Material, List<Integer>> fillers = config.getTopFillers();

        for (Material material : fillers.keySet()) {
            Component fillerComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_FILLERS_NAME));

            ItemStack filler = MarketMenuManager.getInstance().generateItemStack(
                    material,
                    BukkitComponentSerializer.legacy().serialize(fillerComponent)
            );

            for (int i : fillers.get(material))
                gui.setItem(i, filler);
        }

        // Positions

        HashMap<Integer, Integer> positions = Config.getInstance().getTopPositions();

        HashMap<UUID, Portfolio> top = DatabaseManager.get().getDatabase().getTopWorth(positions.size());

        int i = 1;
        for (UUID uuid : top.keySet()) {

            ItemStack item = new ItemStack(Material.PLAYER_HEAD);

            SkullMeta meta = (SkullMeta) item.getItemMeta();

            meta.setOwningPlayer(Bukkit.getPlayer(uuid));

            Component name;
            if (i == 1) {
                name = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TOP_NAME_1));
            } else if (i == 2) {
                name = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TOP_NAME_2));
            } else if (i == 3) {
                name = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TOP_NAME_3));
            } else {
                name = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TOP_NAME_OTHERS).replace("[POS]", String.valueOf(i)));
            }

            meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(name));

            List<String> lore = new ArrayList<>();

            double worth = top.get(uuid).getValueOfDefaultCurrency();
            double debt = DebtManager.getInstance().getDebtOfPlayer(uuid);

            String loreAsLine = Lang.get().message(Message.PORTFOLIO_TOP_LORE)
                    .replace("[OWNER]", DatabaseManager.get().getDatabase().getNameByUUID(uuid))
                    .replace("[WORTH]", Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), worth, Style.ROUND_BASIC))
                    .replace("[DEBT]", Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), debt, Style.ROUND_BASIC))
                    .replace("[NET]", Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), worth - debt, Style.ROUND_BASIC));

            HashMap<Item, Integer> content = top.get(uuid).getContent();
            HashMap<Item, Double> topThree = getTop3WeightedItems(content);

            List<Item> keys = new ArrayList<>(topThree.keySet());

            for (Item topItem : keys) {
                    loreAsLine = loreAsLine + Lang.get().message(Message.PORTFOLIO_TOP_LORE_LIST)
                            .replace("[PER]", String.valueOf(Formatter.roundToDecimals(topThree.get(topItem), 1)))
                            .replace("[NAME]", topItem.getTaggedName());
            }

            if (content.keySet().size() == 4) {
                loreAsLine = loreAsLine + Lang.get().message(Message.PORTFOLIO_TOP_LORE_EXTRA_SINGLE);
            } else if (content.keySet().size() > 4) {
                loreAsLine = loreAsLine + Lang.get().message(Message.PORTFOLIO_TOP_LORE_EXTRA)
                        .replace("[NUM-3]", String.valueOf(content.keySet().size() - 3));
            }

            for (String line : loreAsLine.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            gui.setItem(positions.get(i), item);
            i++;
        }

        update();

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "top"));
    }

    public static HashMap<Item, Double> getTop3WeightedItems(HashMap<Item, Integer> content) {

        LinkedHashMap<Item, Double> weightedItems = new LinkedHashMap<>();
        List<Item> sortedItems = new ArrayList<>();
        LinkedHashMap<Item, Double> result = new LinkedHashMap<>();
        double totalWeight = 0.0;

        for (Item item : content.keySet()) {
            int quantity = content.get(item);
            double value = item.getPrice().getValue();
            double weight = value * quantity;
            weightedItems.put(item, weight);
            totalWeight += weight;
        }

        sortedItems.addAll(weightedItems.keySet());
        sortedItems.sort((a, b) -> Double.compare(weightedItems.get(b), weightedItems.get(a)));

        List<Item> top3 = sortedItems.subList(0, Math.min(3, sortedItems.size()));

        for (Item item : top3) {
            double weight = weightedItems.get(item);
            double percentage = (weight / totalWeight) * 100;
            result.put(item, percentage);
        }

        return result;
    }

    @Override
    public void close() {

    }

    @Override
    public void update() {

    }
}

