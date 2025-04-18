package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.advancedgui.Images;
import me.bounser.nascraft.api.events.Action;
import me.bounser.nascraft.api.events.TransactionCompletedEvent;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.api.events.BuyItemEvent;
import me.bounser.nascraft.api.events.SellItemEvent;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.discord.DiscordLog;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.managers.InventoryManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.managers.currencies.Currency;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.managers.MoneyManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.unit.stats.Instant;
import me.bounser.nascraft.market.unit.stats.ItemStats;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.*;

public class Item {

    private ItemStack itemStack;
    private final String identifier;
    private String alias;
    private String taggedAlias;
    private String formattedAlias;
    private final BufferedImage icon;
    private Category category;

    private final Price price;
    private Currency currency;

    private int operations;

    private int volume;

    private float collectedTaxes;

    private ItemStats itemStats;

    private final float multiplier;

    private final Item parent;

    private List<Item> childs = new ArrayList<>();

    boolean restricted;

    public Item (ItemStack itemStack, String identifier, String alias, Category category, BufferedImage image) {

        itemStack.setAmount(1);

        this.itemStack = itemStack;
        this.identifier = identifier;

        setupAlias(alias);

        this.currency = CurrenciesManager.getInstance().getCurrency(
                Config.getInstance().getCurrency(identifier)
        );

        if (currency == null)
            Nascraft.getInstance().getLogger().severe("Item: " + identifier + " doesn't have a valid currency.");

        this.price = new Price(
                this,
                Config.getInstance().getInitialPrice(identifier),
                Config.getInstance().getElasticity(identifier),
                Config.getInstance().getSupport(identifier),
                Config.getInstance().getResistance(identifier),
                Config.getInstance().getNoiseIntensity(identifier));

        this.icon = image;
        this.restricted = Config.getInstance().getRestricted(identifier);

        price.initializeHourValues(DatabaseManager.get().getDatabase().retrieveLastPrice(this));

        this.category = category;
        operations = 0;
        multiplier = 1;
        parent = null;

        itemStats = new ItemStats(this);
    }

    public Item(Item parent, float multiplier, ItemStack itemStack, String identifier, String alias, Currency currency){

        this.currency = parent.getCurrency();

        itemStack.setAmount(1);

        this.parent = parent;
        this.itemStack = itemStack;
        this.multiplier = multiplier;
        this.identifier = identifier;
        this.price = parent.getPrice();
        this.icon = Images.getInstance().getImage(itemStack.getType());

        setupAlias(alias);
    }

    public void setupAlias(String alias) {

        taggedAlias = alias;

        Component miniMessageAlias = MiniMessage.miniMessage().deserialize(alias);

        this.formattedAlias = BukkitComponentSerializer.legacy().serialize(miniMessageAlias);

        this.alias = extractPlainText(miniMessageAlias);

        if (alias.equals(formattedAlias)) {
            taggedAlias = Lang.get().message(Message.DEFAULT_ITEM_FORMAT).replace("[ALIAS]", alias);
            Component defaultMiniMessageAlias = MiniMessage.miniMessage().deserialize(taggedAlias);
            formattedAlias = BukkitComponentSerializer.legacy().serialize(defaultMiniMessageAlias);
        }
    }

    public String extractPlainText(Component component) {
        StringBuilder plainText = new StringBuilder();

        if (component instanceof TextComponent) {
            plainText.append(((TextComponent) component).content());
        }

        for (Component child : component.children()) {
            plainText.append(extractPlainText(child));
        }

        return plainText.toString();
    }

    public void addChildItem(Item item) {
        childs.add(item);
    }

    public void removeChildItem(Item item) {
        childs.remove(item);
    }

    public List<Item> getChilds() {
        return childs;
    }

    public String getName() { return alias; }

    public String getTaggedName() { return taggedAlias; }

    public String getFormattedName() { return formattedAlias; }

    public double buyPrice(int amount) {
        return price.getProjectedCost(-amount*multiplier, price.getBuyTaxMultiplier());
    }

    public double sellPrice(int amount) {
        return price.getProjectedCost(amount*multiplier, price.getSellTaxMultiplier());
    }

    public double buy(int amount, UUID uuid, boolean feedback) {

        Player player = Bukkit.getPlayer(uuid);
        Player offlinePlayer = Bukkit.getPlayer(uuid);

        boolean limitReached = !price.canStockChange(amount, true);

        if (limitReached && restricted) {
            if (player != null && feedback) Lang.get().message(player, Message.TOP_LIMIT_REACHED);
            return 0;
        }

        BuyItemEvent event = new BuyItemEvent(player, this, amount);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return 0;

        if(!MarketManager.getInstance().getActive()) { Lang.get().message(player, Message.SHOP_CLOSED); return 0; }

        double worth = price.getProjectedCost(-amount*multiplier, price.getBuyTaxMultiplier());

        if (!checkBalance(player, feedback, worth)) return 0;
        if (!InventoryManager.checkInventory(player, feedback, itemStack, amount)) return 0;

        if (player != null && feedback) {
            InventoryManager.addItemsToInventory(player, itemStack, amount);
        }

        MoneyManager.getInstance().withdraw(offlinePlayer, currency, worth, (1-price.getBuyTaxMultiplier()));

        if (player != null && feedback) Lang.get().message(player, Message.BUY_MESSAGE, Formatter.format(currency, worth, Style.ROUND_BASIC), String.valueOf(amount), taggedAlias);

        if (!limitReached) {
            if (parent != null)
                parent.updateInternalValues(amount,
                        amount*price.getValue(),
                        -amount*multiplier,
                        price.getValue()*(1-price.getBuyTaxMultiplier())*amount*multiplier);
            else
                updateInternalValues(amount,
                        amount*price.getValue(),
                        -amount*multiplier,
                        price.getValue()*(1-price.getBuyTaxMultiplier())*amount*multiplier);
        }

        Trade trade = new Trade(this, LocalDateTime.now(), worth, amount, true, false, uuid);

        DatabaseManager.get().getDatabase().saveTrade(trade);

        if (Config.getInstance().getDiscordEnabled() && Config.getInstance().getLogChannelEnabled())
            DiscordLog.getInstance().sendTradeLog(trade);

        MarketManager.getInstance().addOperation();

        TransactionCompletedEvent transactionEvent = new TransactionCompletedEvent(player, this, amount, Action.BUY, worth);
        Bukkit.getPluginManager().callEvent(transactionEvent);

        return worth;
    }

    public boolean checkBalance(Player player, boolean feedback, double money) {
        if (!MoneyManager.getInstance().hasEnoughMoney(player, currency, money)) {
            if (player != null && feedback) Lang.get().message(player, currency.getNotEnoughMessage());
            return false;
        }
        return true;
    }

    public double sell(int amount, UUID uuid, boolean feedback) {

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        Player player = Bukkit.getPlayer(uuid);

        boolean limitReached = !price.canStockChange(amount, false);

        if (limitReached && restricted) {
            if (player != null && feedback) Lang.get().message(player, Message.BOTTOM_LIMIT_REACHED);
            return -1;
        }

        SellItemEvent event = new SellItemEvent(player, this, amount);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return -1;

        if (!MarketManager.getInstance().getActive()) {
            if (player != null && feedback) Lang.get().message(player, Message.SHOP_CLOSED);
            return -1;
        }

        ItemStack operationItemStack = itemStack.clone();

        operationItemStack.setAmount(1);

        if (player != null && feedback && !player.getInventory().containsAtLeast(operationItemStack, amount)) {
            Lang.get().message(player, Message.NOT_ENOUGH_ITEMS);
            return -1;
        }

        double worth = price.getProjectedCost(amount*multiplier, price.getSellTaxMultiplier());

        if (player != null && feedback) {
            operationItemStack.setAmount(amount);
            player.getInventory().removeItem(operationItemStack);
        }

        if (!limitReached) {
            if (parent != null)
                parent.updateInternalValues(amount,
                        amount*price.getValue(),
                        amount*multiplier,
                        price.getValue()*(1-price.getBuyTaxMultiplier())*amount*multiplier);
            else
                updateInternalValues(amount,
                        amount*price.getValue(),
                        amount*multiplier,
                        price.getValue()*(1-price.getBuyTaxMultiplier())*amount*multiplier);
        }

        MoneyManager.getInstance().deposit(offlinePlayer, currency, worth, price.getSellTaxMultiplier());

        worth = RoundUtils.round(worth);

        if (player != null && feedback) Lang.get().message(player, Message.SELL_MESSAGE, Formatter.format(currency, worth, Style.ROUND_BASIC), String.valueOf(amount), taggedAlias);

        Trade trade = new Trade(this, LocalDateTime.now(), worth, amount, false, false, uuid);

        DatabaseManager.get().getDatabase().saveTrade(trade);
        if (Config.getInstance().getDiscordEnabled() && Config.getInstance().getLogChannelEnabled())
            DiscordLog.getInstance().sendTradeLog(trade);
        MarketManager.getInstance().addOperation();

        TransactionCompletedEvent transactionEvent = new TransactionCompletedEvent(player, this, amount, Action.SELL, worth);
        Bukkit.getPluginManager().callEvent(transactionEvent);

        return worth;
    }

    public List<Double> getValuesPastHour() {
        return price.getValuesPastHour();
    }

    public void ghostBuyItem(int amount) {
        updateInternalValues(-amount, amount, -amount,price.getValue()*price.getBuyTaxMultiplier());
        MarketManager.getInstance().addOperation();
    }

    public void ghostSellItem(int amount) {
        updateInternalValues(amount, amount, amount, price.getValue()*price.getSellTaxMultiplier());
        MarketManager.getInstance().addOperation();
    }

    private void updateInternalValues(int operations, double volume, float stockChange, double taxes) {
        this.operations += operations;
        this.volume += volume;
        this.price.changeStock(stockChange);
        this.collectedTaxes += taxes;
    }

    public String getIdentifier() { return identifier; }

    public List<Material> getParentAndChildsMaterials() {
        List<Material> materials = new ArrayList<>();

        materials.add(itemStack.getType());

        for (Item item : childs)
            materials.add(item.getItemStack().getType());

        return materials;
    }

    public boolean isParent() { return parent == null; }

    public Item getParent() { return parent; }

    public float getMultiplier() { return multiplier; }

    public Price getPrice() { return price; }

    public Currency getCurrency() { return currency; }

    public void setCurrency(Currency currency) { this.currency = currency; }

    public int getOperations() { return operations; }

    public void lowerOperations() {
        if (operations > 10) {
            operations -= Math.round((float) operations/60f);
            operations -= 3;
        } else if (operations > 1){
            operations -= 1;
        }
    }

    public int getVolume() { return volume; }

    public float getCollectedTaxes() { return collectedTaxes; }

    public void setCollectedTaxes(float newCollectedTaxes) { collectedTaxes = newCollectedTaxes; }

    public void addVolume(int volume) { this.volume += volume; }

    public void restartVolume() { volume = 0; }

    public ItemStats getItemStats() { return itemStats; }

    public Category getCategory() { return category; }

    public void setCategory(Category category) { this.category = category; }

    public void changeProperties(double initialPrice, String alias, float elasticity, float noiseSensibility, double support, double resistance) {

        price.setInitialValue(initialPrice)
                .setElasticity(elasticity)
                .setNoiseIntensity(noiseSensibility)
                .setSupport(support)
                .setResistance(resistance);

        this.alias = alias;
    }

    public ItemStack getItemStack() { return itemStack.clone(); }

    public ItemStack getItemStack(int quantity) {
        ItemStack clonedItemStack = itemStack.clone();
        clonedItemStack.setAmount(quantity);
        return clonedItemStack;
    }

    public void setItemStack(ItemStack itemStack) { this.itemStack = itemStack; }

    public BufferedImage getIcon() { return icon; }

    public boolean isPriceRestricted() { return restricted; }

    public double getChangeLastDay() {
        Instant firstInstant = DatabaseManager.get().getDatabase().getDayPrices(this).get(0);

        double firstValue = firstInstant.getPrice();

        if (firstValue == 0) return 0;

        return ((price.getValue() - firstInstant.getPrice()) / firstInstant.getPrice());
    }

}
