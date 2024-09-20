package me.bounser.nascraft.inventorygui;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

public class CategoryMenu implements MenuPage {

    private final Player player;
    private final Category category;

    private Inventory gui;

    public CategoryMenu(Player player, Category category) {
        this.player = player;
        this.category = category;

        open();
    }

    @Override
    public void open() {

        Config config = Config.getInstance();

        gui = Bukkit.createInventory(null, config.getCategoriesMenuSize(), category.getFormattedDisplayName());

        // Main category item

        gui.setItem(
                config.getCategoryItemSlot(),
                MarketMenuManager.getInstance().generateItemStack(
                        category.getMaterial(),
                        category.getFormattedDisplayName()));

        // Fillers

        Component fillerComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_FILLERS_NAME));

        ItemStack filler = MarketMenuManager.getInstance().generateItemStack(
                config.getCategoryFillersMaterial(),
                BukkitComponentSerializer.legacy().serialize(fillerComponent)
        );

        for (int i : config.getCategoryFillersSlots())
            gui.setItem(i, filler);

        player.setMetadata("NascraftPage", new FixedMetadataValue(Nascraft.getInstance(), 0));
        update();
    }

    @Override
    public void close() {

    }

    @Override
    public void update() {

        Config config = Config.getInstance();

        int page = player.getMetadata("NascraftPage").get(0).asInt();

        // Items

        List<Item> items = category.getItems();

        int j = 0;

        List<Integer> categorySlots = config.getCategoryItemsSlots();

        for (int i : categorySlots) {

            if (j + page * categorySlots.size() < items.size()) {

                Item item = items.get(j + page * categorySlots.size());

                ItemStack itemStack = item.getItemStack();

                ItemMeta meta = itemStack.getItemMeta();

                meta.setDisplayName(item.getFormattedName());

                List<String> prevLore = meta.getLore();

                List<String> itemLore = MarketMenuManager.getInstance().getLoreFromItem(item, Lang.get().message(Message.GUI_CATEGORY_ITEM_LORE));

                if (meta.hasLore() && prevLore != null) {
                    itemLore.add("");
                    itemLore.addAll(prevLore);
                    meta.setLore(itemLore);
                } else {
                    meta.setLore(itemLore);
                }

                itemStack.setItemMeta(meta);

                gui.setItem(i, itemStack);

            } else {
                gui.setItem(i, new ItemStack(Material.AIR));
            }
            j++;
        }

        // Back button

        if (page == 0) {
            if (config.getCategoryBackEnabled()) {
                Component backComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_CATEGORY_BACK_NAME));

                gui.setItem(
                        config.getCategoryBackSlot(),
                        MarketMenuManager.getInstance().generateItemStack(
                                config.getCategoryBackMaterial(),
                                BukkitComponentSerializer.legacy().serialize(backComponent)
                        ));
            }
        } else {
            Component backComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_CATEGORY_PREVIOUS_NAME));

            gui.setItem(
                    config.getCategoryBackSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getCategoryBackMaterial(),
                            BukkitComponentSerializer.legacy().serialize(backComponent)
                    ));
        }

        // Next button

        if (category.getItems().size() > categorySlots.size() * (1 + page)) {
            Component backComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_CATEGORY_NEXT_NAME));

            gui.setItem(
                    config.getCategoryNextSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getCategoryNextMaterial(),
                            BukkitComponentSerializer.legacy().serialize(backComponent)
                    ));
        } else {
            Component fillerComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_FILLERS_NAME));

            ItemStack filler = MarketMenuManager.getInstance().generateItemStack(
                    config.getCategoryFillersMaterial(),
                    BukkitComponentSerializer.legacy().serialize(fillerComponent)
            );

            gui.setItem(
                    config.getCategoryNextSlot(),
                    filler
            );
        }

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "category-menu-" + category.getIdentifier()));
        player.setMetadata("NascraftPage", new FixedMetadataValue(Nascraft.getInstance(), page));
        MarketMenuManager.getInstance().setMenuOfPlayer(player, this);
    }
}
