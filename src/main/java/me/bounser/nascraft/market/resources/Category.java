package me.bounser.nascraft.market.resources;

import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.config.Config;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;


public class Category {


    private String identifier;

    private String displayName;

    private String formattedDisplayName;

    private Material material;

    private List<Item> items = new ArrayList<>();

    public Category(String identifier) {
        this.identifier = identifier;

        Component miniMessageDisplayName = MiniMessage.miniMessage().deserialize(Config.getInstance().getDisplayName(this));

        this.formattedDisplayName = BukkitComponentSerializer.legacy().serialize(miniMessageDisplayName);
        this.displayName = PlainTextComponentSerializer.plainText().serialize(miniMessageDisplayName);
        this.material = Config.getInstance().getMaterialOfCategory(this);
    }

    public void addItem(Item item) { items.add(item); }

    public void removeItem(Item item) { items.remove(item); }

    //

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public void setDisplayMaterial(Material material) { this.material = material; }

    //

    public int getNumberOfItems() { return items.size(); }

    public Item getItemOfIndex(int index) { return items.get(index); }

    public String getIdentifier() { return identifier; }

    public String getDisplayName() { return displayName; }

    public String getFormattedDisplayName() { return formattedDisplayName; }

    public Material getMaterial() { return material; }

    public List<Item> getItems() { return items; }

    public List<String> getItemsIdentifiers() {

        List<String> itemsIdentifiers = new ArrayList<>();

        for (Item item : items)
            itemsIdentifiers.add(item.getIdentifier());

        return itemsIdentifiers;
    }

    public void setItems(List<Item> items) { this.items = items; }

}
