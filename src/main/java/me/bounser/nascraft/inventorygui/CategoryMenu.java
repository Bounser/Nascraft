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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
                        category.getFormattedDisplayName()
                ));

        // Back button

        if (config.getCategoryBackEnabled()) {
            Component backComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_CATEGORY_BACK_NAME));

            gui.setItem(
                    config.getCategoryBackSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getCategoryBackMaterial(),
                            BukkitComponentSerializer.legacy().serialize(backComponent)
                    ));
        }

        // Fillers

        Component fillerComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_FILLERS_NAME));

        ItemStack filler = MarketMenuManager.getInstance().generateItemStack(
                config.getCategoryFillersMaterial(),
                BukkitComponentSerializer.legacy().serialize(fillerComponent)
        );

        for (int i : config.getCategoryFillersSlots())
            gui.setItem(i, filler);

        // Items

        List<Item> items = category.getItems();

        int j = 0;

        for (int i : config.getCategoryItemsSlots()) {

            if (j < items.size()) {

                Item item = items.get(j);

                gui.setItem(i,
                        MarketMenuManager.getInstance().generateItemStack(
                                item.getItemStack().getType(),
                                item.getFormattedName(),
                                MarketMenuManager.getInstance().getLoreFromItem(item, Lang.get().message(Message.GUI_CATEGORY_ITEM_LORE))
                        )
                );
            }
            j++;
        }

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "category-menu-" + category.getIdentifier()));
    }

    @Override
    public void close() {

    }

    @Override
    public void update() {

    }
}
