package me.bounser.nascraft.commands.sell.sellinv;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.Command;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.inventorygui.MarketMenuManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.managers.currencies.Currency;
import me.bounser.nascraft.market.MarketManager;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class SellInvCommand extends Command {

    public SellInvCommand() {
        super(
                "sellmenu",
                new String[]{Config.getInstance().getCommandAlias("sell-menu")},
                "Sell items directly to the market",
                "nascraft.sellmenu"
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        Player player;

        if(!(sender instanceof Player)) {

            if (args.length != 1) {
                Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Wrong usage of command. /sell [playerName]");
                return;
            }

            player = Bukkit.getPlayer(args[0]);

            if (player == null) {
                Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Player not found");
                return;
            }

        } else {
            player = (Player) sender;

            if (!player.hasPermission("nascraft.sellmenu")) {
                Lang.get().message(player, Message.NO_PERMISSION);
                return;
            }

            if (!MarketManager.getInstance().getActive()) {
                Lang.get().message(player, Message.SHOP_CLOSED);
                return;
            }
        }

        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.SELL_TITLE));

        Inventory inventory = Bukkit.createInventory(null, Config.getInstance().getGetSellMenuSize(), BukkitComponentSerializer.legacy().serialize(title));

        insertFillingPanes(inventory);
        insertSellButton(inventory);
        insertCloseButton(inventory);
        insertHelpHead(inventory);

        player.openInventory(inventory);

        player.setMetadata("NascraftSell", new FixedMetadataValue(Nascraft.getInstance(), true));
    }

    public void insertFillingPanes(Inventory inventory) {

        ItemStack filler = new ItemStack(Config.getInstance().getFillerMaterial());
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = 0; i < 9 ; i++)
            inventory.setItem(i, filler);

        int size = Config.getInstance().getGetSellMenuSize();

        for (int i = (size-9); i < size; i++)
            inventory.setItem(i, filler);

    }

    public void insertSellButton(Inventory inventory) {

        ItemStack sellButton = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta meta = sellButton.getItemMeta();

        Component name = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.SELL_BUTTON_NAME));
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(name));

        List<String> lore = new ArrayList<>();

        for (String line : Lang.get().message(Message.SELL_BUTTON_LORE, "[WORTH-LIST]", "").split("\\n")) {
            Component loreLine = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(loreLine));
        }

        meta.setLore(lore);
        sellButton.setItemMeta(meta);

        inventory.setItem(40, sellButton);
    }

    public void insertCloseButton(Inventory inventory) {

        if (!Config.getInstance().getCloseButtonEnabled()) return;

        ItemStack closeButton = new ItemStack(Config.getInstance().getCloseButtonMaterial());
        ItemMeta meta = closeButton.getItemMeta();

        Component displayNameComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.SELL_CLOSE));
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(displayNameComponent));

        closeButton.setItemMeta(meta);

        inventory.setItem(8, closeButton);
    }

    public void insertHelpHead(Inventory inventory) {

        if (!Config.getInstance().getHelpEnabled()) return;

        String TEXTURE = Config.getInstance().getHelpTexture();

        PlayerProfile profile = getProfile(TEXTURE);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        Component displayNameComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.SELL_HELP_TITLE));
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(displayNameComponent));

        List<String> lore = new ArrayList<>();
        for (String line : Lang.get().message(Message.SELL_HELP_LORE).split("\\n")) {
            Component loreComponent = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
        }

        meta.setLore(lore);
        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);

        inventory.setItem(Config.getInstance().getHelpSlot(), head);
    }

    private static PlayerProfile getProfile(String texture) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();
        URL urlObject;
        try {
            urlObject = new URL("https://textures.minecraft.net/texture/" + texture);
        } catch (MalformedURLException exception) {
            throw new RuntimeException("Invalid URL", exception);
        }
        textures.setSkin(urlObject);
        profile.setTextures(textures);
        return profile;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
