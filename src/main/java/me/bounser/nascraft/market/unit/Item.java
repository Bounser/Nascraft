package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.advancedgui.Images;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.api.events.BuyTradableEvent;
import me.bounser.nascraft.api.events.SellTradableEvent;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.discord.DiscordLog;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.managers.MoneyManager;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.unit.stats.ItemStats;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
    private String formatedAlias;
    private final BufferedImage icon;
    private Category category;

    private final Price price;

    private int operations;

    private int volume;

    private float collectedTaxes;

    private ItemStats itemStats;

    private final float multiplier;

    private final Item parent;

    private List<Item> childs = new ArrayList<>();

    public Item (ItemStack itemStack, String identifier, String alias, Category category, BufferedImage image) {

        itemStack.setAmount(1);

        this.itemStack = itemStack;
        this.identifier = identifier;

        setupAlias(alias);

        this.price = new Price(
                this,
                Config.getInstance().getInitialPrice(identifier),
                Config.getInstance().getElasticity(identifier),
                Config.getInstance().getSupport(identifier),
                Config.getInstance().getResistance(identifier),
                Config.getInstance().getNoiseIntensity(identifier));

        this.icon = image;

        price.initializeHourValues(DatabaseManager.get().getDatabase().retrieveLastPrice(this));

        this.category = category;
        operations = 0;
        multiplier = 1;
        parent = null;

        itemStats = new ItemStats(this);
    }

    public Item(Item parent, float multiplier, ItemStack itemStack, String identifier, String alias){

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

        this.alias = PlainTextComponentSerializer.plainText().serialize(miniMessageAlias);;
        this.formatedAlias = BukkitComponentSerializer.legacy().serialize(miniMessageAlias);

        if (alias.equals(formatedAlias)) {
            taggedAlias = Lang.get().message(Message.DEFAULT_ITEM_FORMAT).replace("[ALIAS]", alias);
            Component defaultMiniMessageAlias = MiniMessage.miniMessage().deserialize(taggedAlias);
            formatedAlias = BukkitComponentSerializer.legacy().serialize(defaultMiniMessageAlias);
        }
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

    public String getFormattedName() { return formatedAlias; }


    public float buyPrice(int amount) {
        return price.getProjectedCost(-amount*multiplier, price.getBuyTaxMultiplier());
    }

    public float sellPrice(int amount) {
        return price.getProjectedCost(amount*multiplier, price.getSellTaxMultiplier());
    }

    public void buy(int amount, UUID uuid, boolean feedback) {

        Player player = Bukkit.getPlayer(uuid);
        Player offlinePlayer = Bukkit.getPlayer(uuid);

        BuyTradableEvent event = new BuyTradableEvent(player, this, amount);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) return;

        if(!MarketManager.getInstance().getActive()) { Lang.get().message(player, Message.SHOP_CLOSED); return; }

        float worth = price.getProjectedCost(-amount*multiplier, price.getBuyTaxMultiplier());

        if (!checkBalance(player, feedback, worth)) return;
        if (!checkInventory(player, feedback, amount)) return;

        if (player != null && feedback) {
            ItemStack operationItemStack = itemStack.clone();

            int stacks = amount / itemStack.getMaxStackSize();

            operationItemStack.setAmount(itemStack.getMaxStackSize());

            for (int i = 0; i < stacks; i++)
                player.getInventory().addItem(operationItemStack);

            operationItemStack.setAmount(amount - stacks * itemStack.getMaxStackSize());

            player.getInventory().addItem(operationItemStack);
        }

        MoneyManager.getInstance().withdraw(offlinePlayer, worth, price.getBuyTaxMultiplier());

        if (player != null && feedback) Lang.get().message(player, Message.BUY_MESSAGE, Formatter.format(worth, Style.ROUND_BASIC), String.valueOf(amount), taggedAlias);

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

        Trade trade = new Trade(this, LocalDateTime.now(), worth, amount, true, false, uuid);

        DatabaseManager.get().getDatabase().saveTrade(trade);

        if (Config.getInstance().getDiscordEnabled() && Config.getInstance().getLogChannelEnabled())
            DiscordLog.getInstance().sendTradeLog(trade);

        MarketManager.getInstance().addOperation();
    }

    public boolean checkBalance(Player player, boolean feedback, float money) {
        if (!MoneyManager.getInstance().hasEnoughMoney(player, money)) {
            if (player != null && feedback) Lang.get().message(player, Message.NOT_ENOUGH_MONEY);
            return false;
        }
        return true;
    }

    public boolean checkInventory(Player player, boolean feedback, int amount) {

        if (player == null) return true;

        if (player.getInventory().firstEmpty() == -1) {

            int untilFull = 0;

            for (ItemStack is : player.getInventory()) {
                if(is != null && is.isSimilar(itemStack)) {
                    untilFull += itemStack.getType().getMaxStackSize() - is.getAmount();
                }
            }
            if (untilFull < amount) {
                if (feedback) Lang.get().message(player, Message.NOT_ENOUGH_SPACE);
                return false;
            }

        } else {
            int slotsUsed = 0;

            for (ItemStack content : player.getInventory().getStorageContents())
                if (content != null && !content.getType().equals(Material.AIR)) slotsUsed++;

            if ((36 - slotsUsed) < (amount/itemStack.getType().getMaxStackSize())) {
                if (feedback) Lang.get().message(player, Message.NOT_ENOUGH_SPACE);
                return false;
            }
        }

        return true;
    }

    public float sell(int amount, UUID uuid, boolean feedback) {

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        Player player = Bukkit.getPlayer(uuid);

        SellTradableEvent event = new SellTradableEvent(player, this, amount);
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

        float worth = price.getProjectedCost(amount*multiplier, price.getSellTaxMultiplier());

        if (player != null && feedback) {
            operationItemStack.setAmount(amount);
            player.getInventory().removeItem(operationItemStack);
        }

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


        MoneyManager.getInstance().deposit(offlinePlayer, worth, price.getSellTaxMultiplier());

        worth = RoundUtils.round(worth);

        if (player != null && feedback) Lang.get().message(player, Message.SELL_MESSAGE, Formatter.format(worth, Style.ROUND_BASIC), String.valueOf(amount), taggedAlias);

        Trade trade = new Trade(this, LocalDateTime.now(), worth, amount, false, false, uuid);

        DatabaseManager.get().getDatabase().saveTrade(trade);
        if (Config.getInstance().getDiscordEnabled() && Config.getInstance().getLogChannelEnabled())
            DiscordLog.getInstance().sendTradeLog(trade);
        MarketManager.getInstance().addOperation();

        return worth;
    }

    public List<Float> getValuesPastHour() {
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

    private void updateInternalValues(int operations, float volume, float stockChange, float taxes) {
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

    public void changeProperties(float initialPrice, String alias, float elasticity, float noiseSensibility, float support, float resistance) {

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

}
