package me.bounser.nascraft.inventorygui.Portfolio;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.inventorygui.MarketMenuManager;
import me.bounser.nascraft.managers.DebtManager;
import me.bounser.nascraft.managers.currencies.Currency;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.managers.MoneyManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.portfolio.Portfolio;
import me.bounser.nascraft.portfolio.PortfoliosManager;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class PortfolioInventory implements Listener {

    private static PortfolioInventory instance = null;

    public static PortfolioInventory getInstance() { return instance == null ? new PortfolioInventory() : instance; }

    @EventHandler
    public void onClickInventory(InventoryClickEvent event) {

        if (!event.getWhoClicked().hasMetadata("NascraftPortfolio")) return;

        event.setCancelled(true);

        if (event.getView().getTopInventory().getSize() != 45) { return; }
        if (event.getClickedInventory() == null) { return; }

        Portfolio portfolio = PortfoliosManager.getInstance().getPortfolio(event.getWhoClicked().getUniqueId());

        Player player = (Player) event.getWhoClicked();

        if (event.getClickedInventory().getSize() == 45 && Config.getInstance().getPortfolioMenuBackEnabled() && event.getSlot() == Config.getInstance().getPortfolioMenuBackSlot()) {
            MarketMenuManager.getInstance().openMenu(player);
            return;
        }

        if (event.getClickedInventory().getSize() == 45 && Config.getInstance().getPortfolioInfoEnabled() && event.getSlot() == Config.getInstance().getPortfolioInfoSlot()) {
            MarketMenuManager.getInstance().setMenuOfPlayer(player, new InfoPortfolio(PortfoliosManager.getInstance().getPortfolio(event.getWhoClicked().getUniqueId()), player));
            return;
        }

        if (event.getClickedInventory().getSize() == 45 && Config.getInstance().getPortfolioDebtEnabled() && event.getSlot() == Config.getInstance().getPortfolioDebtSlot()) {
            MarketMenuManager.getInstance().setMenuOfPlayer(player, new DebtMenu(player));
            return;
        }

        if (event.getClickedInventory().getSize() == 45 && Config.getInstance().getPortfolioTopEnabled() && event.getSlot() == Config.getInstance().getPortfolioTopSlot()) {
            MarketMenuManager.getInstance().setMenuOfPlayer(player, new TopMenu(player));
            return;
        }

        // EXPANSION
        if (event.getClickedInventory().getSize() == 45 && event.getCurrentItem() != null && event.getRawSlot() > 8 + portfolio.getCapacity() && event.getRawSlot() <= 35) {
            if (MoneyManager.getInstance().hasEnoughMoney((OfflinePlayer) event.getWhoClicked(), CurrenciesManager.getInstance().getVaultCurrency(), portfolio.getNextSlotPrice())) {
                Nascraft.getEconomy().withdrawPlayer((OfflinePlayer) event.getView().getPlayer(), portfolio.getNextSlotPrice());
                portfolio.increaseCapacity();
            } else {
                Lang.get().message(player, Message.PORTFOLIO_CANT_AFFORD_EXPANSION);
            }
            return;
        }

        // RETRIEVE ITEM
        if (event.getClickedInventory().getSize() == 45 &&
                event.getRawSlot() > 8 &&
                event.getRawSlot() <= 35) {

            List<Item> items = new ArrayList<>(portfolio.getContent().keySet());

            if (event.getRawSlot() - 9 > items.size()-1) return;

            Item item = items.get(event.getRawSlot()-9);

            if (item == null) return;

            if (DebtManager.getInstance().getDebtOfPlayer(player.getUniqueId()) != 0) {
                Lang.get().message(player, Message.PORTFOLIO_DEBT_PORTFOLIO_LOCKED);
                return;
            }

            int quantity = portfolio.getContent().get(item);

            quantity = Math.min(quantity, item.getItemStack().getMaxStackSize());

            if (!checkInventory((Player) event.getWhoClicked(), item, quantity)) { return; }

            portfolio.removeItem(item, quantity);
            event.getWhoClicked().getInventory().addItem(item.getItemStack(quantity));

            PortfoliosManager.getInstance().savePortfolioOfPlayer(player);
            return;
        }


        // DROP ITEM
        if (event.getClickedInventory().getSize() != 45 && event.getCurrentItem() != null) {

            if (!MarketManager.getInstance().isAValidParentItem(event.getCurrentItem())) {
                Lang.get().message((Player) event.getWhoClicked(), Message.PORTFOLIO_INVALID);
                return;
            }

            Item item = MarketManager.getInstance().getItem(event.getCurrentItem());

            if (item == null) { return; }

            int amount = event.getCurrentItem().getAmount();

            if (!portfolio.hasSpace(item, amount)) {
                Lang.get().message((Player) event.getWhoClicked(), Message.PORTFOLIO_NO_STORAGE);
                return;
            }

            event.getCurrentItem().setAmount(0);

            portfolio.addItem(item, amount);

            PortfoliosManager.getInstance().savePortfolioOfPlayer(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked().hasMetadata("NascraftPortfolio")) {  event.setCancelled(true); }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer().hasMetadata("NascraftPortfolio")) {
            event.getPlayer().removeMetadata("NascraftPortfolio", Nascraft.getInstance());
        }
    }

    public void updatePortfolioInventory(Player player) {

        if (!player.hasMetadata("NascraftPortfolio")) return;

        Inventory inventory = player.getOpenInventory().getTopInventory();

        inventory.clear();

        insertFillers(inventory);
        insertLockedSpaces(inventory, player.getUniqueId());
        insertPortfolioContent(inventory, player.getUniqueId());

        if (Config.getInstance().getPortfolioMenuBackEnabled() && !player.getMetadata("NascraftPortfolio").get(0).asBoolean()) {
            insetBackButton(inventory);
        }

        if (Config.getInstance().getPortfolioInfoEnabled()) {
            insertInfoButton(inventory, player.getUniqueId());
        }

        if (Config.getInstance().getPortfolioDebtEnabled()) {
            insertDebtButton(inventory);
        }

        if (Config.getInstance().getPortfolioTopEnabled()) {
            insertTopButton(inventory);
        }
    }

    public void insertFillers(Inventory inventory) {

        ItemStack filler = new ItemStack(Config.getInstance().getPortfolioFillerMaterial());
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for(int i : new int[]{0, 1, 2, 3, 5, 6, 7, 36, 37, 38, 39, 40, 41, 42, 43, 44}) {
            inventory.setItem(i, filler);
        }
    }

    public void insertInfoButton(Inventory inventory, UUID uuid) {

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getPlayer(uuid));
        
        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_INFO_TITLE));
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(title));

        String worth = "";

        HashMap<Currency, Double> value = PortfoliosManager.getInstance().getPortfolio(uuid).getInventoryValuePerCurrency();

        for (Currency currency : value.keySet())
            worth += "\n" + Formatter.format(currency, value.get(currency), Style.ROUND_BASIC);

        List<String> lore = new ArrayList<>();
        for (String line : Lang.get().message(Message.PORTFOLIO_INFO_LORE, "[WORTH]", worth).split("\\n")) {
            Component loreComponent = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
        }

        meta.setLore(lore);

        item.setItemMeta(meta);

        inventory.setItem(Config.getInstance().getPortfolioInfoSlot(), item);
    }

    public void insertLockedSpaces(Inventory inventory, UUID uuid) {

        ItemStack filler = new ItemStack(Config.getInstance().getPortfolioLockedMaterial());
        ItemMeta meta = filler.getItemMeta();

        Component lockName = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_LOCKED_TITLE));
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(lockName));

        List<String> lore = new ArrayList<>();
        for (String line : Lang.get().message(Message.PORTFOLIO_LOCKED_LORE, "0", Formatter.format(CurrenciesManager.getInstance().getVaultCurrency(), PortfoliosManager.getInstance().getPortfolio(uuid).getNextSlotPrice(), Style.ROUND_BASIC), "0").split("\\n")) {
            Component loreComponent = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
        }

        meta.setLore(lore);
        filler.setItemMeta(meta);

        for(int i = 9 + DatabaseManager.get().getDatabase().retrieveCapacity(uuid); i <= 35 ; i++) {
            inventory.setItem(i, filler);
        }
    }

    public void insertPortfolioContent(Inventory inventory, UUID uuid) {

        HashMap<Item, Integer> content = PortfoliosManager.getInstance().getPortfolio(uuid).getContent();

        int i = 9;
        for (Item item : content.keySet()) {
            if (item == null) {
                i++; continue;
            }
            ItemStack itemStack = item.getItemStack();
            ItemMeta meta = itemStack.getItemMeta();

            String extraLore = Lang.get().message(Message.PORTFOLIO_AMOUNT)
                    .replace("[AMOUNT]", String.valueOf(content.get(item)))
                    .replace("[WORTH]", Formatter.format(item.getCurrency(), content.get(item) * item.getPrice().getValue(), Style.ROUND_BASIC));

            List<String> lore = new ArrayList<>();

            if (meta.hasLore()) {
                lore = meta.getLore();
                lore.add("");
            }

            for (String line : extraLore.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            meta.setLore(lore);

            itemStack.setItemMeta(meta);
            itemStack.setAmount(Math.min(itemStack.getMaxStackSize(), content.get(item)));
            inventory.setItem(i, itemStack);
            i++;
        }
    }

    public void insetBackButton(Inventory inventory) {
        ItemStack back = new ItemStack(Config.getInstance().getPortfolioMenuBackMaterial());
        ItemMeta meta = back.getItemMeta();
        Component backName = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_BACK_NAME));
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(backName));
        back.setItemMeta(meta);

        inventory.setItem(Config.getInstance().getPortfolioMenuBackSlot(), back);
    }

    public void insertDebtButton(Inventory inventory) {
        ItemStack debt = new ItemStack(Config.getInstance().getPortfolioDebtMaterial());
        ItemMeta meta = debt.getItemMeta();
        Component backName = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_DEBT_NAME));

        List<String> lore = new ArrayList<>();
        for (String line : Lang.get().message(Message.PORTFOLIO_DEBT_LORE).split("\\n")) {
            Component loreComponent = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
        }

        meta.setLore(lore);
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(backName));
        debt.setItemMeta(meta);

        inventory.setItem(Config.getInstance().getPortfolioDebtSlot(), debt);
    }

    public void insertTopButton(Inventory inventory) {
        ItemStack debt = new ItemStack(Config.getInstance().getPortfolioTopMaterial());
        ItemMeta meta = debt.getItemMeta();
        Component backName = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TOP_NAME));

        List<String> lore = new ArrayList<>();
        for (String line : Lang.get().message(Message.PORTFOLIO_TOP_BUTTON_LORE).split("\\n")) {
            Component loreComponent = MiniMessage.miniMessage().deserialize(line);
            lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
        }

        meta.setLore(lore);
        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(backName));
        debt.setItemMeta(meta);

        inventory.setItem(Config.getInstance().getPortfolioTopSlot(), debt);
    }

    public boolean checkInventory(Player player, Item item, int amount) {

        if (player == null) return true;

        ItemStack itemStack = item.getItemStack();

        if (player.getInventory().firstEmpty() == -1) {

            int untilFull = 0;

            for (ItemStack is : player.getInventory()) {
                if(is != null && is.isSimilar(itemStack)) {
                    untilFull += itemStack.getType().getMaxStackSize() - is.getAmount();
                }
            }
            if (untilFull < amount) {
                Lang.get().message(player, Message.PORTFOLIO_NO_SPACE);
                return false;
            }

        } else {
            int slotsUsed = 0;

            for (ItemStack content : player.getInventory().getStorageContents())
                if (content != null && !content.getType().equals(Material.AIR)) slotsUsed++;

            if ((36 - slotsUsed) < (amount/itemStack.getType().getMaxStackSize())) {
                Lang.get().message(player, Message.PORTFOLIO_NO_SPACE);
                return false;
            }
        }
        return true;
    }
}