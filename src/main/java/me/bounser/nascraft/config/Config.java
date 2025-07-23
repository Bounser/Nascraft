/*
 * Decompiled with CFR 0.153-SNAPSHOT (d6f6758-dirty).
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.Material
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.event.block.Action
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package me.bounser.nascraft.config;

import java.io.File;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.DatabaseType;
import me.bounser.nascraft.discord.linking.LinkingMethod;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.managers.currencies.Currency;
import me.bounser.nascraft.managers.currencies.CurrencyType;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.limitorders.Duration;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.sellwand.Wand;
import de.tr7zw.changeme.nbtapi.NBT;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class Config {
    private FileConfiguration config;
    private FileConfiguration items;
    private FileConfiguration categories;
    private FileConfiguration inventorygui;
    private FileConfiguration investments;
    private static Config instance;
    private Nascraft main = Nascraft.getInstance();
    private String redisHost;
    private int redisPort;
    private String redisPassword;
    private String redisUsername;
    private int redisDatabase;
    private int redisMaxConnections;
    private int redisMaxIdle;
    private int redisMinIdle;
    private List<String> redisPriorityItems;
    private int redisSyncInterval;
    private boolean redisUseFallback;
    private String redisFailoverMode;
    private boolean foliaEnabled;
    private int foliaMaxRegionTasks;
    private String foliaDefaultRegion;
    private String foliaLoadBalance;
    private boolean foliaAsyncDbOperations;

    public static Config getInstance() {
        return instance == null ? (instance = new Config()) : instance;
    }

    private Config() {
        this.main.saveDefaultConfig();
        this.config = Nascraft.getInstance().getConfig();
        this.items = this.setupFile("items.yml");
        this.categories = this.setupFile("categories.yml");
        this.inventorygui = this.setupFile("inventorygui.yml");
        this.loadRedisConfig();
        this.loadFoliaConfig();
    }

    public YamlConfiguration setupFile(String name) {
        File file = new File(this.main.getDataFolder(), name);
        if (!file.exists()) {
            this.main.saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration((File)file);
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration((File)new File(this.main.getDataFolder(), "config.yml"));
        DatabaseManager.get().getDatabase().saveEverything();
        this.items = this.setupFile("items.yml");
        this.categories = this.setupFile("categories.yml");
        this.loadRedisConfig();
        this.loadFoliaConfig();
        MarketManager.getInstance().reload();
    }

    private void loadRedisConfig() {
        this.redisHost = this.config.getString("database.redis.host", "localhost");
        this.redisPort = this.config.getInt("database.redis.port", 6379);
        this.redisPassword = this.config.getString("database.redis.password", "");
        this.redisUsername = this.config.getString("database.redis.username", "");
        this.redisDatabase = this.config.getInt("database.redis.database", 0);
        this.redisMaxConnections = this.config.getInt("database.redis.max-connections", 8);
        this.redisMaxIdle = this.config.getInt("database.redis.max-idle", 8);
        this.redisMinIdle = this.config.getInt("database.redis.min-idle", 0);
        this.redisPriorityItems = this.config.getStringList("database.redis.sync.priority-items");
        this.redisSyncInterval = this.config.getInt("database.redis.sync.interval-ms", 100);
        this.redisUseFallback = this.config.getBoolean("database.redis.sync.use-fallback", true);
        this.redisFailoverMode = this.config.getString("database.redis.sync.failover-mode", "writelocal");
    }

    private void loadFoliaConfig() {
        this.foliaEnabled = this.config.getBoolean("folia.enabled", true);
        this.foliaMaxRegionTasks = this.config.getInt("folia.max-region-tasks", 5);
        this.foliaDefaultRegion = this.config.getString("folia.default-region", "global");
        this.foliaLoadBalance = this.config.getString("folia.load-balance", "round-robin");
        this.foliaAsyncDbOperations = this.config.getBoolean("folia.async-db-operations", true);
    }

    public FileConfiguration getItemsFileConfiguration() {
        return this.items;
    }

    public File getItemsFile() {
        return new File(this.main.getDataFolder(), "items.yml");
    }

    public FileConfiguration getCategoriesFileConfiguration() {
        return this.categories;
    }

    public File getCategoriesFile() {
        return new File(this.main.getDataFolder(), "categories.yml");
    }

    public Boolean getWebEnabled() {
        if (this.config.contains("web.enabled")) {
            return this.config.getBoolean("web.enabled");
        }
        return false;
    }

    public int getWebPort() {
        if (this.config.contains("web.port")) {
            return this.config.getInt("web.port");
        }
        return 8080;
    }

    public String getDiscordId() {
        if (this.config.contains("web.discord-client-id")) {
            return this.config.getString("web.discord-client-id");
        }
        return "";
    }

    public String getDiscordSecret() {
        if (this.config.contains("web.discord-client-secret")) {
            return this.config.getString("web.discord-client-secret");
        }
        return "";
    }

    public int getWebTimeout() {
        if (this.config.contains("web.timeout")) {
            return this.config.getInt("web.timeout");
        }
        return 0;
    }

    public int getWebCodeExpiration() {
        if (this.config.contains("web.code-expiration")) {
            return this.config.getInt("web.code-expiration");
        }
        return 0;
    }

    public Boolean getCheckResources() {
        return this.config.getBoolean("auto-resources-injection");
    }

    public List<String> getIgnoredKeys() {
        return this.config.getStringList("ignored-keys");
    }

    public float getLayoutCooldown() {
        if (this.config.contains("layout-cooldown")) {
            return (float)this.config.getDouble("layout-cooldown");
        }
        return 1.0f;
    }

    public DatabaseType getDatabaseType() {
        DatabaseType databaseType = null;
        try {
            databaseType = DatabaseType.valueOf(this.config.getString("database.type").toUpperCase());
        } catch (IllegalArgumentException e) {
            Nascraft.getInstance().getLogger().warning("Error trying to recognize database type: " + this.config.getString("database.type").toUpperCase());
            Nascraft.getInstance().getLogger().warning("Is it a valid type of database?");
            e.printStackTrace();
            Nascraft.getInstance().getPluginLoader().disablePlugin((Plugin)Nascraft.getInstance());
        }
        if (databaseType == null) {
            Nascraft.getInstance().getLogger().warning("DatabaseManager type not recognized: " + this.config.getString("database.type").toUpperCase());
            Nascraft.getInstance().getPluginLoader().disablePlugin((Plugin)Nascraft.getInstance());
        }
        return databaseType;
    }

    public String getHost() {
        return this.config.getString("database.mysql.host");
    }

    public int getPort() {
        return this.config.getInt("database.mysql.port");
    }

    public String getDatabase() {
        return this.config.getString("database.mysql.database");
    }

    public String getUser() {
        return this.config.getString("database.mysql.user");
    }

    public String getPassword() {
        return this.config.getString("database.mysql.password");
    }

    public int getDatabasePurgeDays() {
        return this.config.getInt("database.days-until-history-removed");
    }

    public String getSelectedLanguage() {
        return this.config.getString("language");
    }

    public List<Currency> getCurrencies() {
        ArrayList<Currency> currencies = new ArrayList<Currency>();
        for (String supplier : this.config.getConfigurationSection("currencies.suppliers").getKeys(false)) {
            if (supplier.equals("vault")) {
                currencies.add(new Currency("vault", CurrencyType.VAULT, null, null, this.config.getString("currencies.suppliers.vault.not-enough"), this.config.getString("currencies.suppliers.vault.format"), null, this.config.getInt("currencies.suppliers.vault.decimal-positions"), this.config.getDouble("currencies.suppliers.vault.top-limit"), this.config.getDouble("currencies.suppliers.vault.low-limit")));
                continue;
            }
            currencies.add(new Currency(supplier, CurrencyType.CUSTOM, this.config.getString("currencies.suppliers." + supplier + ".deposit-cmd"), this.config.getString("currencies.suppliers." + supplier + ".withdraw-cmd"), this.config.getString("currencies.suppliers." + supplier + ".not-enough"), this.config.getString("currencies.suppliers." + supplier + ".format"), this.config.getString("currencies.suppliers." + supplier + ".balance-placeholder"), this.config.getInt("currencies.suppliers." + supplier + ".decimal-positions"), this.config.getDouble("currencies.suppliers." + supplier + ".top-limit"), this.config.getDouble("currencies.suppliers." + supplier + ".low-limit")));
        }
        return currencies;
    }

    public String getDefaultCurrencyIdentifier() {
        return this.config.getString("currencies.default-currency");
    }

    public String getCurrency(String identifier) {
        if (this.items.contains("items." + identifier + ".currency")) {
            return this.items.getString("items." + identifier + ".currency");
        }
        return this.config.getString("currencies.default-currency");
    }

    public float[] getLimits() {
        float[] limit = new float[]{(float)this.config.getDouble("price-options.limits.low"), (float)this.config.getDouble("price-options.limits.high")};
        return limit;
    }

    public boolean getPriceNoise() {
        return this.config.getBoolean("price-options.noise.enabled");
    }

    public int getNoiseTime() {
        if (!this.config.contains("price-options.noise.time")) {
            return 60;
        }
        return this.config.getInt("price-options.noise.time");
    }

    public boolean isMarketClosed() {
        if (!this.config.contains("market-control.closed")) {
            return false;
        }
        return this.config.getBoolean("market-control.closed");
    }

    public void setMarketOpen() {
        this.config.set("market-control.closed", (Object)true);
    }

    public void setMarketClosed() {
        this.config.set("market-control.closed", (Object)false);
    }

    public float getTaxBuy(String identifier) {
        if (this.items.contains("items." + identifier + ".tax.buy")) {
            return 1.0f + (float)this.items.getDouble("items." + identifier + ".tax.buy");
        }
        return 1.0f + (float)this.config.getDouble("market-control.taxation.buy");
    }

    public Double getTaxBuyPercentage(String identifier) {
        if (this.items.contains("items." + identifier + ".tax.buy")) {
            return this.items.getDouble("items." + identifier + ".tax.buy");
        }
        return this.config.getDouble("market-control.taxation.buy");
    }

    public float getTaxSell(String identifier) {
        if (this.items.contains("items." + identifier + ".tax.sell")) {
            return 1.0f - (float)this.items.getDouble("items." + identifier + ".tax.sell");
        }
        return 1.0f - (float)this.config.getDouble("market-control.taxation.sell");
    }

    public Double getTaxSellPercentage(String identifier) {
        if (this.items.contains("items." + identifier + ".tax.sell")) {
            return this.items.getDouble("items." + identifier + ".tax.sell");
        }
        return this.config.getDouble("market-control.taxation.sell");
    }

    public boolean takeIntoAccountTax() {
        if (!this.config.contains("market-control.taxation.take-into-account-taxes")) {
            return false;
        }
        return this.config.getBoolean("market-control.taxation.take-into-account-taxes");
    }

    public boolean getMarketPermissionRequirement() {
        return this.config.getBoolean("market-control.market-permission");
    }

    public String getCommandAlias(String command) {
        return this.config.getString("commands." + command + ".alias");
    }

    public boolean isCommandEnabled(String command) {
        return this.config.getBoolean("commands." + command + ".enabled");
    }

    public int getGetSellMenuSize() {
        return this.config.getInt("commands.sell-menu.size");
    }

    public boolean getHelpEnabled() {
        return this.config.getBoolean("commands.sell-menu.help.enabled");
    }

    public int getHelpSlot() {
        return this.config.getInt("commands.sell-menu.help.slot");
    }

    public String getHelpTexture() {
        return this.config.getString("commands.sell-menu.help.texture");
    }

    public Material getFillerMaterial() {
        return Material.getMaterial((String)this.config.getString("commands.sell-menu.filler.material").toUpperCase());
    }

    public int getSellButtonSlot() {
        return this.config.getInt("commands.sell-menu.sell-button.slot");
    }

    public Material getSellButtonMaterial() {
        return Material.getMaterial((String)this.config.getString("commands.sell-menu.sell-button.material").toUpperCase());
    }

    public boolean getCloseButtonEnabled() {
        return this.config.getBoolean("commands.sell-menu.close-button.enabled");
    }

    public int getCloseButtonSlot() {
        return this.config.getInt("commands.sell-menu.close-button.slot");
    }

    public Material getCloseButtonMaterial() {
        return Material.getMaterial((String)this.config.getString("commands.sell-menu.close-button.material").toUpperCase());
    }

    public boolean getSellWandsEnabled() {
        return this.config.getBoolean("sell-wands.enabled");
    }

    public boolean getSellWandsPermissionNeeded(String wandName) {
        return this.config.contains("sell-wands." + wandName + ".permission");
    }

    public String getSellWandPermission(String wandName) {
        return this.config.getString("sell-wands.wands." + wandName + ".permission");
    }

    public List<Wand> getWands() {
        ArrayList<Wand> wands = new ArrayList<Wand>();
        for (String name : this.config.getConfigurationSection("sell-wands.wands").getKeys(false)) {
            float multiplier = 1.0f;
            if (this.config.contains("sell-wands.wands." + name + ".multiplier")) {
                multiplier = (float)this.config.getDouble("sell-wands.wands." + name + ".multiplier");
            }
            int uses = -1;
            if (this.config.contains("sell-wands.wands." + name + ".uses")) {
                uses = this.config.getInt("sell-wands.wands." + name + ".uses");
            }
            float maxProfit = -1.0f;
            if (this.config.contains("sell-wands.wands." + name + ".max-profit")) {
                maxProfit = (float)this.config.getDouble("sell-wands.wands." + name + ".max-profit");
            }
            boolean enchanted = false;
            if (this.config.contains("sell-wands.wands." + name + ".enchanted")) {
                enchanted = this.config.getBoolean("sell-wands.wands." + name + ".enchanted");
            }
            int cooldown = 3;
            if (this.config.contains("sell-wands.wands." + name + ".cooldown")) {
                cooldown = this.config.getInt("sell-wands.wands." + name + ".cooldown");
            }
            String permission = null;
            if (this.config.contains("sell-wands.wands." + name + ".permission")) {
                permission = this.config.getString("sell-wands.wands." + name + ".permission");
            }
            Action sell = Action.LEFT_CLICK_BLOCK;
            if (this.config.contains("sell-wands.wands." + name + ".sell")) {
                switch (this.config.getString("sell-wands.wands." + name + ".sell").toLowerCase()) {
                    case "left": {
                        sell = Action.LEFT_CLICK_BLOCK;
                        break;
                    }
                    case "right": {
                        sell = Action.RIGHT_CLICK_BLOCK;
                        break;
                    }
                    case "none": {
                        sell = null;
                    }
                }
            }
            Action estimate = Action.RIGHT_CLICK_BLOCK;
            if (this.config.contains("sell-wands.wands." + name + ".estimate")) {
                switch (this.config.getString("sell-wands.wands." + name + ".estimate").toLowerCase()) {
                    case "left": {
                        estimate = Action.LEFT_CLICK_BLOCK;
                        break;
                    }
                    case "right": {
                        estimate = Action.RIGHT_CLICK_BLOCK;
                        break;
                    }
                    case "none": {
                        estimate = null;
                    }
                }
            }
            ArrayList<Currency> currencies = new ArrayList<Currency>();
            if (this.config.contains("sell-wands.wands." + name + ".currencies")) {
                for (String currencyIdentifier : this.config.getStringList("sell-wands.wands." + name + ".currencies")) {
                    currencies.add(CurrenciesManager.getInstance().getCurrency(currencyIdentifier.toLowerCase()));
                }
            } else {
                currencies.addAll(CurrenciesManager.getInstance().getCurrencies());
            }
            wands.add(new Wand(name, Material.getMaterial((String)this.config.getString("sell-wands.wands." + name + ".material").toUpperCase()), this.config.getString("sell-wands.wands." + name + ".display-name"), this.config.getStringList("sell-wands.wands." + name + ".lore"), uses, multiplier, maxProfit, cooldown, enchanted, permission, sell, estimate, currencies));
        }
        return wands;
    }

    public boolean getDiscordEnabled() {
        return this.config.getBoolean("discord-bot.enabled");
    }

    public LinkingMethod getLinkingMethod() {
        return LinkingMethod.valueOf(this.config.getString("discord-bot.main-menu.link-method").toUpperCase());
    }

    public String getToken() {
        return this.config.getString("discord-bot.token");
    }

    public boolean getLogChannelEnabled() {
        return this.config.getBoolean("discord-bot.log-trades.enabled");
    }

    public String getLogChannel() {
        return this.config.getString("discord-bot.log-trades.channel");
    }

    public boolean getDiscordMenuEnabled() {
        return this.config.getBoolean("discord-bot.main-menu.enabled");
    }

    public int getUpdateTime() {
        if (!this.config.contains("discord-bot.main-menu.options.update-time")) {
            return 60;
        }
        return this.config.getInt("discord-bot.main-menu.options.update-time");
    }

    public boolean getOptionWikiEnabled() {
        return this.config.getBoolean("discord-bot.main-menu.options.wiki.enabled");
    }

    public boolean getOptionAlertEnabled() {
        return this.config.getBoolean("discord-bot.main-menu.options.alerts.enabled");
    }

    public int getAlertsDaysUntilExpired() {
        return this.config.getInt("discord-bot.main-menu.options.alerts.expiration");
    }

    public boolean getOptionGraphsEnabled() {
        return this.config.getBoolean("discord-bot.main-menu.options.detailed-graphs.enabled");
    }

    public boolean getOptionPersonalLogEnabled() {
        return this.config.getBoolean("discord-bot.main-menu.options.personal-log.enabled");
    }

    public boolean getOptionSelectionEnabled() {
        return this.config.getBoolean("discord-bot.main-menu.options.selection-bar.enabled");
    }

    public boolean getOptionCPIEnabled() {
        return this.config.getBoolean("discord-bot.main-menu.options.cpi.enabled");
    }

    public boolean getOptionCPIComparisonEnabled() {
        if (!this.config.contains("discord-bot.main-menu.options.cpi-comparison.enabled")) {
            return true;
        }
        return this.config.getBoolean("discord-bot.main-menu.options.cpi-comparison.enabled");
    }

    public boolean getOptionFlowsEnabled() {
        if (!this.config.contains("discord-bot.main-menu.options.flows.enabled")) {
            return true;
        }
        return this.config.getBoolean("discord-bot.main-menu.options.flows.enabled");
    }

    public String getChannel() {
        return this.config.getString("discord-bot.main-menu.channel");
    }

    public String getAdminRoleID() {
        return this.config.getString("discord-bot.admin-role-id");
    }

    public int getDefaultSlots() {
        return this.config.getInt("portfolio.default-size");
    }

    public int getPortfolioMaxStorage() {
        return this.config.getInt("portfolio.storage-limit");
    }

    public float getSlotPriceFactor() {
        return (float)this.config.getDouble("portfolio.slot-price-factor");
    }

    public float getSlotPriceBase() {
        return (float)this.config.getDouble("portfolio.slot-price-base");
    }

    public Material getPortfolioFillerMaterial() {
        return Material.getMaterial((String)this.config.getString("portfolio.in-game-gui.fillers.material").toUpperCase());
    }

    public Material getPortfolioLockedMaterial() {
        return Material.getMaterial((String)this.config.getString("portfolio.in-game-gui.locked.material").toUpperCase());
    }

    public boolean getLimitOrdersEnabled() {
        return this.config.getBoolean("limit-orders.enabled");
    }

    public int getMaxLimitOrdersPerPlayer() {
        return this.config.getInt("limit-orders.max-per-player");
    }

    public int getMaxLimitOrderSize() {
        return this.config.getInt("limit-orders.order-max-size");
    }

    public int getCheckingPeriod() {
        return this.config.getInt("limit-orders.checking-period");
    }

    public List<Duration> getDurations() {
        ArrayList<Duration> durations = new ArrayList<Duration>();
        for (String duration : this.config.getConfigurationSection("limit-orders.durations.").getKeys(false)) {
            durations.add(new Duration(this.config.getInt("limit-orders.durations." + duration + ".duration"), this.config.getString("limit-orders.durations." + duration + ".display"), (float)this.config.getDouble("limit-orders.durations." + duration + ".fee"), (float)this.config.getDouble("limit-orders.durations." + duration + ".min-fee")));
        }
        return durations;
    }

    public float getDiscordBuyTax() {
        if (this.config.getBoolean("discord-bot.main-menu.slot-price.taxation.override")) {
            return (float)(1.0 + this.config.getDouble("discord-bot.main-menu.slot-price.taxation.buy"));
        }
        return 1.0f + (float)this.config.getDouble("market-control.taxation.buy");
    }

    public float getDiscordSellTax() {
        if (this.config.getBoolean("discord-bot.main-menu.slot-price.taxation.override")) {
            return (float)(1.0 - this.config.getDouble("discord-bot.main-menu.slot-price.taxation.sell"));
        }
        return 1.0f - (float)this.config.getDouble("market-control.taxation.sell");
    }

    public Set<String> getAllMaterials() {
        return this.items.getConfigurationSection("items.").getKeys(false);
    }

    public float getInitialPrice(String identifier) {
        for (String item : this.getAllMaterials()) {
            if (!identifier.equalsIgnoreCase(item)) continue;
            return (float)this.items.getDouble("items." + item + ".initial-price");
        }
        return 1.0f;
    }

    public boolean includeInCPI(Item item) {
        if (this.items.contains("items." + item.getIdentifier() + ".exclude-from-cpi")) {
            return !this.items.getBoolean("items." + item.getIdentifier() + ".exclude-from-cpi");
        }
        return true;
    }

    public List<Item> getChilds(String identifier) {
        ArrayList<Item> childs = new ArrayList<Item>();
        Set<String> section = null;
        if (this.items.getConfigurationSection("items." + identifier + ".child.") != null) {
            section = this.items.getConfigurationSection("items." + identifier + ".child.").getKeys(false);
        }
        Item parent = MarketManager.getInstance().getItem(identifier);
        if (section == null || section.isEmpty()) {
            return childs;
        }
        for (String childIdentifier : section) {
            ItemStack itemStack = this.getItemStackOfChild(identifier, childIdentifier);
            float multiplier = (float)this.items.getDouble("items." + identifier + ".child." + childIdentifier + ".multiplier");
            String alias = childIdentifier;
            alias = this.items.contains("items." + identifier + ".child." + childIdentifier + ".alias") ? this.items.getString("items." + identifier + ".child." + childIdentifier + ".alias") : (Character.toUpperCase(alias.charAt(0)) + alias.substring(1)).replace("_", " ");
            childs.add(new Item(parent, multiplier, itemStack, childIdentifier, alias, parent.getCurrency()));
        }
        return childs;
    }

    public ItemStack getItemStackOfItem(String identifier) {
        ItemStack itemStack = null;
        if (this.items.contains("items." + identifier + ".item-stack")) {
            itemStack = (ItemStack)this.items.getSerializable("items." + identifier + ".item-stack", ItemStack.class);
        }
        if (itemStack == null) {
            try {
                itemStack = new ItemStack(Material.getMaterial(identifier.replaceAll("\\d", "").toUpperCase()));
            } catch (IllegalArgumentException e) {
                Nascraft.getInstance().getLogger().severe("Couldn't load item with identifier: " + identifier);
                Nascraft.getInstance().getLogger().severe("Reason: Material " + identifier.replaceAll("\\d", "").toUpperCase() + " is not valid!");
                Nascraft.getInstance().getLogger().severe("Does the item exist in the version of your server?");
                Nascraft.getInstance().getLogger().warning("Skipping item '" + identifier + "' - plugin will continue loading other items.");
                return null;
            }
        }
        for (String ignoredKey : this.getIgnoredKeys()) {
            NBT.modify(itemStack, (nbt) -> {
                nbt.removeKey(ignoredKey);
                return null;
            });
        }
        return itemStack;
    }

    public ItemStack getItemStackOfChild(String identifier, String childIdentifier) {
        ItemStack itemStack = null;
        if (this.items.contains("items." + identifier + ".childs." + childIdentifier + "item-stack")) {
            itemStack = (ItemStack)this.items.getSerializable("items." + identifier + ".childs." + childIdentifier + "item-stack", ItemStack.class);
        }
        if (itemStack == null) {
            try {
                itemStack = new ItemStack(Material.getMaterial((String)childIdentifier.replaceAll("\\d", "").toUpperCase()));
            } catch (IllegalArgumentException e) {
                Nascraft.getInstance().getLogger().severe("Couldn't load child item with identifier: " + childIdentifier + " for parent: " + identifier);
                Nascraft.getInstance().getLogger().severe("Reason: Material " + childIdentifier.replaceAll("\\d", "").toUpperCase() + " is not valid!");
                Nascraft.getInstance().getLogger().severe("Does the item exist in the version of your server?");
                Nascraft.getInstance().getLogger().warning("Skipping child item '" + childIdentifier + "' - plugin will continue loading other items.");
                return null;
            }
        }
        return itemStack;
    }

    public boolean hasAlias(String identifier) {
        return this.items.contains("items." + identifier + ".alias");
    }

    public String getAlias(String identifier) {
        if (!this.items.contains("items." + identifier + ".alias")) {
            return (Character.toUpperCase(identifier.charAt(0)) + identifier.substring(1)).replace("_", " ");
        }
        return this.items.getString("items." + identifier + ".alias");
    }

    public float getSupport(String identifier) {
        if (this.items.contains("items." + identifier + ".support")) {
            return (float)this.items.getDouble("items." + identifier + ".support");
        }
        return 0.0f;
    }

    public float getResistance(String identifier) {
        if (this.items.contains("items." + identifier + ".resistance")) {
            return (float)this.items.getDouble("items." + identifier + ".resistance");
        }
        return 0.0f;
    }

    public float getElasticity(String identifier) {
        if (this.items.contains("items." + identifier + ".elasticity")) {
            return (float)this.items.getDouble("items." + identifier + ".elasticity");
        }
        return (float)this.config.getDouble("price-options.default-elasticity");
    }

    public float getNoiseIntensity(String identifier) {
        if (this.items.contains("items." + identifier + ".noise-intensity")) {
            return (float)this.items.getDouble("items." + identifier + ".noise-intensity");
        }
        return (float)this.config.getDouble("price-options.noise.default-intensity");
    }

    public boolean getRestricted(String identifier) {
        if (this.items.contains("items." + identifier + ".limit.restricted")) {
            return this.items.getBoolean("items." + identifier + ".limit.restricted");
        }
        return true;
    }

    public double getLowLimit(String identifier) {
        if (this.items.contains("items." + identifier + ".limit.low")) {
            return this.items.getDouble("items." + identifier + ".limit.low");
        }
        return -1.0;
    }

    public double getHighLimit(String identifier) {
        if (this.items.contains("items." + identifier + ".limit.high")) {
            return this.items.getDouble("items." + identifier + ".limit.high");
        }
        return -1.0;
    }

    public float getNoiseMultiplier() {
        if (this.config.contains("price-options.noise.intensity-multiplier")) {
            return (float)this.config.getDouble("price-options.noise.intensity-multiplier");
        }
        return 1.0f;
    }

    public float getElasticityMultiplier() {
        if (this.config.contains("price-options.elasticity-multiplier")) {
            return (float)this.config.getDouble("price-options.elasticity-multiplier");
        }
        return 1.0f;
    }

    public Set<String> getCategories() {
        return this.categories.getConfigurationSection("categories.").getKeys(false);
    }

    public String getDisplayName(Category category) {
        return this.categories.getString("categories." + category.getIdentifier() + ".display-name");
    }

    public Material getMaterialOfCategory(Category category) {
        if (this.categories.contains("categories." + category.getIdentifier() + ".display-material")) {
            try {
                return Material.valueOf((String)this.categories.getString("categories." + category.getIdentifier() + ".display-material").toUpperCase());
            } catch (IllegalArgumentException exception) {
                Nascraft.getInstance().getLogger().warning(String.valueOf(ChatColor.RED) + "Category " + category.getIdentifier() + " doesn't have a valid display material.");
                return Material.STONE;
            }
        }
        return Material.STONE;
    }

    public Category getCategoryFromMaterial(String identifier) {
        for (Category category : MarketManager.getInstance().getCategories()) {
            if (!this.categories.contains("categories." + category.getIdentifier() + ".items") || !this.categories.getList("categories." + category.getIdentifier() + ".items").contains(identifier)) continue;
            return category;
        }
        return null;
    }

    public int getMainMenuSize() {
        return this.inventorygui.getInt("main-menu.size");
    }

    public boolean getAlertsMenuEnabled() {
        return this.inventorygui.getBoolean("main-menu.alerts.enabled");
    }

    public int getAlertsSlot() {
        return this.inventorygui.getInt("main-menu.alerts.slot");
    }

    public Material getAlertsMaterial(boolean linked) {
        String path = "main-menu.alerts." + (linked ? "linked" : "not-linked") + ".material";
        return Material.getMaterial((String)this.inventorygui.getString(path).toUpperCase());
    }

    public boolean getLimitOrdersMenuEnabled() {
        return this.inventorygui.getBoolean("main-menu.limit-orders.enabled");
    }

    public int getLimitOrdersSlot() {
        return this.inventorygui.getInt("main-menu.limit-orders.slot");
    }

    public Material getLimitOrdersMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("main-menu.limit-orders.material").toUpperCase());
    }

    public boolean getInformationMenuEnabled() {
        return this.inventorygui.getBoolean("main-menu.information.enabled");
    }

    public int getInformationSlot() {
        return this.inventorygui.getInt("main-menu.information.slot");
    }

    public Material getInformationMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("main-menu.information.material").toUpperCase());
    }

    public boolean getPortfolioMarketMenuEnabled() {
        return this.inventorygui.getBoolean("main-menu.portfolio.enabled");
    }

    public int getPortfolioSlot() {
        return this.inventorygui.getInt("main-menu.portfolio.slot");
    }

    public Material getPortfolioMaterial(boolean linked) {
        if (linked) {
            return Material.getMaterial((String)this.inventorygui.getString("main-menu.portfolio.linked.material").toUpperCase());
        }
        return Material.getMaterial((String)this.inventorygui.getString("main-menu.portfolio.not-linked.material").toUpperCase());
    }

    public boolean getTrendsEnabled() {
        return this.inventorygui.getBoolean("main-menu.trends.enabled");
    }

    public int getTrendsSlot() {
        return this.inventorygui.getInt("main-menu.trends.slot");
    }

    public Material getTrendsMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("main-menu.trends.material").toUpperCase());
    }

    public HashMap<Material, List<Integer>> getMainMenuFillers() {
        HashMap<Material, List<Integer>> fills = new HashMap<Material, List<Integer>>();
        for (String section : this.inventorygui.getConfigurationSection("main-menu.fillers.").getKeys(false)) {
            Material material = Material.valueOf((String)section.toUpperCase());
            fills.put(material, this.inventorygui.getIntegerList("main-menu.fillers." + section));
        }
        return fills;
    }

    public boolean getSetCategorySegments() {
        if (!this.inventorygui.contains("main-menu.categories.item-list")) {
            return true;
        }
        return this.inventorygui.getBoolean("main-menu.categories.item-list");
    }

    public List<Integer> getCategoriesSlots() {
        return this.inventorygui.getIntegerList("main-menu.categories.slots");
    }

    public int getCategoriesMenuSize() {
        return this.inventorygui.getInt("category-section.size");
    }

    public int getCategoryItemSlot() {
        return this.inventorygui.getInt("category-section.category-item.slot");
    }

    public boolean getCategoryBackEnabled() {
        return this.inventorygui.getBoolean("category-section.back-button.enabled");
    }

    public int getCategoryBackSlot() {
        return this.inventorygui.getInt("category-section.back-button.slot");
    }

    public Material getCategoryBackMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("category-section.back-button.material").toUpperCase());
    }

    public int getCategoryNextSlot() {
        return this.inventorygui.getInt("category-section.next-button.slot");
    }

    public Material getCategoryNextMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("category-section.next-button.material").toUpperCase());
    }

    public Material getCategoryFillersMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("category-section.fillers.material").toUpperCase());
    }

    public List<Integer> getCategoryFillersSlots() {
        return this.inventorygui.getIntegerList("category-section.fillers.slots");
    }

    public List<Integer> getCategoryItemsSlots() {
        return this.inventorygui.getIntegerList("category-section.items.slots");
    }

    public int getBuySellMenuSize() {
        return this.inventorygui.getInt("buy-sell.size");
    }

    public int getBuySellMenuItemSlot() {
        return this.inventorygui.getInt("buy-sell.item.slot");
    }

    public boolean getAlertsBuySellEnabled() {
        return this.inventorygui.getInt("buy-sell.alerts.slot") != -1;
    }

    public int getAlertsBuySellSlot() {
        return this.inventorygui.getInt("buy-sell.alerts.slot");
    }

    public Material getAlertsBuySellMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("buy-sell.alerts.material").toUpperCase());
    }

    public boolean getLimitOrdersBuySellEnabled() {
        return this.inventorygui.getInt("buy-sell.limit-orders.slot") != -1;
    }

    public int getLimitOrdersBuySellSlot() {
        return this.inventorygui.getInt("buy-sell.limit-orders.slot");
    }

    public Material getLimitOrdersBuySellMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("buy-sell.limit-orders.material").toUpperCase());
    }

    public boolean getInfoBuySellEnabled() {
        return this.inventorygui.getInt("buy-sell.info.slot") != -1;
    }

    public int getInfoBuySellSlot() {
        return this.inventorygui.getInt("buy-sell.info.slot");
    }

    public Material getInfoBuySellMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("buy-sell.info.material").toUpperCase());
    }

    public boolean getBuySellBackEnabled() {
        return this.inventorygui.getInt("buy-sell.back-button.slot") != -1;
    }

    public int getBuySellBackSlot() {
        return this.inventorygui.getInt("buy-sell.back-button.slot");
    }

    public Material getBuySellBackMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("buy-sell.back-button.material").toUpperCase());
    }

    public List<Integer> getBuySellFillersSlots() {
        return this.inventorygui.getIntegerList("buy-sell.fillers.slots");
    }

    public Material getBuySellFillersMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("buy-sell.fillers.material").toUpperCase());
    }

    public Material getBuySellBuyMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("buy-sell.buy-buttons.material").toUpperCase());
    }

    public HashMap<Integer, Integer> getBuySellBuySlots() {
        HashMap<Integer, Integer> buttons = new HashMap<Integer, Integer>();
        for (String weight : this.inventorygui.getConfigurationSection("buy-sell.buy-buttons.buttons").getKeys(false)) {
            int slot = this.inventorygui.getInt("buy-sell.buy-buttons.buttons." + weight + ".slot");
            buttons.put(Integer.valueOf(weight), slot);
        }
        return buttons;
    }

    public Material getBuySellSellMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("buy-sell.sell-buttons.material").toUpperCase());
    }

    public HashMap<Integer, Integer> getBuySellSellSlots() {
        HashMap<Integer, Integer> buttons = new HashMap<Integer, Integer>();
        for (String weight : this.inventorygui.getConfigurationSection("buy-sell.sell-buttons.buttons").getKeys(false)) {
            int slot = this.inventorygui.getInt("buy-sell.sell-buttons.buttons." + weight + ".slot");
            buttons.put(Integer.valueOf(weight), slot);
        }
        return buttons;
    }

    public int getAlertsMenuSize() {
        return this.inventorygui.getInt("alerts.size");
    }

    public boolean getAlertsMenuBackEnabled() {
        return this.inventorygui.getBoolean("alerts.back-button.enabled");
    }

    public int getAlertsMenuBackSlot() {
        return this.inventorygui.getInt("alerts.back-button.slot");
    }

    public Material getAlertsMenuBackMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("alerts.back-button.material").toUpperCase());
    }

    public Material getAlertsMenuFillersMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("alerts.fillers.material").toUpperCase());
    }

    public List<Integer> getAlertsMenuFillersSlots() {
        return this.inventorygui.getIntegerList("alerts.fillers.slots");
    }

    public List<Integer> getAlertsMenuSlots() {
        return this.inventorygui.getIntegerList("alerts.alerts.slots");
    }

    public int getLimitOrdersMenuSize() {
        return this.inventorygui.getInt("limit-orders.size");
    }

    public boolean getLimitOrdersMenuBackEnabled() {
        return this.inventorygui.getBoolean("limit-orders.back-button.enabled");
    }

    public int getLimitOrdersMenuBackSlot() {
        return this.inventorygui.getInt("limit-orders.back-button.slot");
    }

    public Material getLimitOrdersMenuBackMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("limit-orders.back-button.material").toUpperCase());
    }

    public Material getLimitOrdersMenuFillersMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("limit-orders.fillers.material").toUpperCase());
    }

    public List<Integer> getLimitOrdersMenuFillersSlots() {
        return this.inventorygui.getIntegerList("limit-orders.fillers.slots");
    }

    public List<Integer> getLimitOrdersMenuSlots() {
        return this.inventorygui.getIntegerList("limit-orders.orders.slots");
    }

    public int getSetLimitOrderMenuSize() {
        return this.inventorygui.getInt("set-limit-orders.size");
    }

    public boolean getSetLimitOrderMenuBackEnabled() {
        return this.inventorygui.getInt("set-limit-orders.back-button.slot") != -1;
    }

    public int getSetLimitOrderMenuBackSlot() {
        return this.inventorygui.getInt("set-limit-orders.back-button.slot");
    }

    public Material getSetLimitOrderMenuBackMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("set-limit-orders.back-button.material").toUpperCase());
    }

    public int getSetLimitOrderMenuItemSlot() {
        return this.inventorygui.getInt("set-limit-orders.item.slot");
    }

    public int getSetLimitOrderMenuTimeSlot() {
        return this.inventorygui.getInt("set-limit-orders.time.slot");
    }

    public Material getSetLimitOrderMenuTimeMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("set-limit-orders.time.material").toUpperCase());
    }

    public int getSetLimitOrderMenuPriceSlot() {
        return this.inventorygui.getInt("set-limit-orders.price.slot");
    }

    public Material getSetLimitOrderMenuPriceMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("set-limit-orders.price.material").toUpperCase());
    }

    public int getSetLimitOrderMenuQuantitySlot() {
        return this.inventorygui.getInt("set-limit-orders.quantity.slot");
    }

    public Material getSetLimitOrderMenuQuantityMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("set-limit-orders.quantity.material").toUpperCase());
    }

    public int getSetLimitOrderMenuConfirmSellSlot() {
        return this.inventorygui.getInt("set-limit-orders.confirm-sell.slot");
    }

    public Material getSetLimitOrderMenuConfirmSellMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("set-limit-orders.confirm-sell.material").toUpperCase());
    }

    public int getSetLimitOrderMenuConfirmBuySlot() {
        return this.inventorygui.getInt("set-limit-orders.confirm-buy.slot");
    }

    public Material getSetLimitOrderMenuConfirmBuyMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("set-limit-orders.confirm-buy.material").toUpperCase());
    }

    public Material getSetLimitOrdersMenuFillersMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("set-limit-orders.fillers.material").toUpperCase());
    }

    public List<Integer> getSetLimitOrdersMenuFillersSlots() {
        return this.inventorygui.getIntegerList("set-limit-orders.fillers.slots");
    }

    public boolean getPortfolioMenuBackEnabled() {
        return this.inventorygui.getBoolean("portfolio.back-button.enabled");
    }

    public int getPortfolioMenuBackSlot() {
        return this.inventorygui.getInt("portfolio.back-button.slot");
    }

    public Material getPortfolioMenuBackMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("portfolio.back-button.material").toUpperCase());
    }

    public boolean getPortfolioInfoEnabled() {
        return this.inventorygui.getBoolean("portfolio.info.enabled");
    }

    public int getPortfolioInfoSlot() {
        return this.inventorygui.getInt("portfolio.info.slot");
    }

    public Material getPortfolioDebtMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("portfolio.loan.material").toUpperCase());
    }

    public boolean getPortfolioDebtEnabled() {
        return this.inventorygui.getBoolean("portfolio.loan.enabled");
    }

    public int getPortfolioDebtSlot() {
        return this.inventorygui.getInt("portfolio.loan.slot");
    }

    public int getDebtSize() {
        return this.inventorygui.getInt("portfolio.debt.size");
    }

    public boolean getDebtBackEnabled() {
        return this.inventorygui.getBoolean("portfolio.debt.back-button.enabled");
    }

    public int getDebtBackSlot() {
        return this.inventorygui.getInt("portfolio.debt.back-button.slot");
    }

    public Material getDebtBackMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("portfolio.debt.back-button.material").toUpperCase());
    }

    public boolean getDebtExpEnabled() {
        return this.inventorygui.getBoolean("portfolio.debt.exp.enabled");
    }

    public int getDebtExpSlot() {
        return this.inventorygui.getInt("portfolio.debt.exp.slot");
    }

    public Material getDebtExpMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("portfolio.debt.exp.material").toUpperCase());
    }

    public boolean getDebtInfoEnabled() {
        return this.inventorygui.getBoolean("portfolio.debt.info.enabled");
    }

    public int getDebtInfoSlot() {
        return this.inventorygui.getInt("portfolio.debt.info.slot");
    }

    public Material getDebtInfoMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("portfolio.debt.info.material").toUpperCase());
    }

    public boolean getDebtRepayAllEnabled() {
        return this.inventorygui.getBoolean("portfolio.debt.repay-all.enabled");
    }

    public int getDebtRepayAllSlot() {
        return this.inventorygui.getInt("portfolio.debt.repay-all.slot");
    }

    public Material getDebtRepayAllMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("portfolio.debt.repay-all.material").toUpperCase());
    }

    public boolean getDebtRepayEnabled() {
        return this.inventorygui.getBoolean("portfolio.debt.repay-custom.enabled");
    }

    public int getDebtRepaySlot() {
        return this.inventorygui.getInt("portfolio.debt.repay-custom.slot");
    }

    public Material getDebtRepayMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("portfolio.debt.repay-custom.material").toUpperCase());
    }

    public boolean getDebtMaxLoanEnabled() {
        return this.inventorygui.getBoolean("portfolio.debt.get-max-loan.enabled");
    }

    public int getDebtMaxLoanSlot() {
        return this.inventorygui.getInt("portfolio.debt.get-max-loan.slot");
    }

    public Material getDebtMaxLoanMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("portfolio.debt.get-max-loan.material").toUpperCase());
    }

    public boolean getDebtCustomEnabled() {
        return this.inventorygui.getBoolean("portfolio.debt.custom-loan.enabled");
    }

    public int getDebtCustomSlot() {
        return this.inventorygui.getInt("portfolio.debt.custom-loan.slot");
    }

    public Material getDebtCustomMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("portfolio.debt.custom-loan.material").toUpperCase());
    }

    public List<Integer> getDebtFillersSlots() {
        return this.inventorygui.getIntegerList("portfolio.debt.fillers.slots");
    }

    public Material getDebtFillersMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("portfolio.debt.fillers.material").toUpperCase());
    }

    public boolean getLoansEnabled() {
        return this.config.getBoolean("portfolio.loans.enabled");
    }

    public double getLoansMaxSize() {
        return this.config.getDouble("portfolio.loans.max-size");
    }

    public double getLoansMinSize() {
        return this.config.getDouble("portfolio.loans.min-size");
    }

    public double getLoanSecurityMargin() {
        return this.config.getDouble("portfolio.loans.security-margin");
    }

    public int getMarginCheckingPeriod() {
        return this.config.getInt("portfolio.loans.margin-checking-period");
    }

    public LocalTime getInterestPaymentHour() {
        String dateFormatted = this.config.getString("portfolio.loans.interest-rate.when");
        assert (dateFormatted != null);
        return LocalTime.parse(dateFormatted);
    }

    public double getLoansDailyInterest() {
        return this.config.getDouble("portfolio.loans.interest-rate.percentage");
    }

    public double getLoansMinimumInterest() {
        return this.config.getDouble("portfolio.loans.interest-rate.minimum");
    }

    public boolean getPortfolioTopEnabled() {
        return this.inventorygui.getBoolean("portfolio.leaderboard.enabled");
    }

    public int getPortfolioTopSlot() {
        return this.inventorygui.getInt("portfolio.leaderboard.slot");
    }

    public Material getPortfolioTopMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("portfolio.leaderboard.material").toUpperCase());
    }

    public int getTopSize() {
        return this.inventorygui.getInt("portfolio.top.size");
    }

    public boolean getTopBackEnabled() {
        return this.inventorygui.getBoolean("portfolio.top.back-button.enabled");
    }

    public int getTopBackSlot() {
        return this.inventorygui.getInt("portfolio.top.back-button.slot");
    }

    public Material getTopBackMaterial() {
        return Material.getMaterial((String)this.inventorygui.getString("portfolio.top.back-button.material").toUpperCase());
    }

    public HashMap<Integer, Integer> getTopPositions() {
        HashMap<Integer, Integer> positions = new HashMap<Integer, Integer>();
        for (String pos : this.inventorygui.getConfigurationSection("portfolio.top.positions").getKeys(false)) {
            int slot = this.inventorygui.getInt("portfolio.top.positions." + pos + ".slot");
            positions.put(Integer.valueOf(pos), slot);
        }
        return positions;
    }

    public HashMap<Material, List<Integer>> getTopFillers() {
        HashMap<Material, List<Integer>> fillers = new HashMap<Material, List<Integer>>();
        for (String filler : this.inventorygui.getConfigurationSection("portfolio.top.fillers").getKeys(false)) {
            List<Integer> slots = this.inventorygui.getIntegerList("portfolio.top.fillers." + filler + ".slots");
            Material material = Material.valueOf(this.inventorygui.getString("portfolio.top.fillers." + filler + ".material").toUpperCase());
            fillers.put(material, slots);
        }
        return fillers;
    }

    public String getRedisHost() {
        return this.redisHost;
    }

    public int getRedisPort() {
        return this.redisPort;
    }

    public String getRedisPassword() {
        return this.redisPassword;
    }

    public String getRedisUsername() {
        return this.redisUsername;
    }

    public int getRedisDatabase() {
        return this.redisDatabase;
    }

    public int getRedisMaxConnections() {
        return this.redisMaxConnections;
    }

    public int getRedisMaxIdle() {
        return this.redisMaxIdle;
    }

    public int getRedisMinIdle() {
        return this.redisMinIdle;
    }

    public List<String> getRedisPriorityItems() {
        return this.redisPriorityItems;
    }

    public int getRedisSyncInterval() {
        return this.redisSyncInterval;
    }

    public boolean getRedisUseFallback() {
        return this.redisUseFallback;
    }

    public String getRedisFailoverMode() {
        return this.redisFailoverMode;
    }

    public boolean getFoliaEnabled() {
        return this.foliaEnabled;
    }

    public int getFoliaMaxRegionTasks() {
        return this.foliaMaxRegionTasks;
    }

    public String getFoliaDefaultRegion() {
        return this.foliaDefaultRegion;
    }

    public String getFoliaLoadBalance() {
        return this.foliaLoadBalance;
    }

    public boolean getFoliaAsyncDbOperations() {
        return this.foliaAsyncDbOperations;
    }
    
    public boolean getDistributedSyncEnabled() { 
        return this.config.getBoolean("database.redis.distributed-sync.enabled"); 
    }
    
    public boolean getNoiseMasterEnabled() {
        return this.config.getBoolean("database.redis.distributed-sync.noise-master.enabled");
    }
    
    public boolean getNoiseMasterAutoElect() {
        return this.config.getBoolean("database.redis.distributed-sync.noise-master.auto-elect");
    }
    
    public String getNoiseMasterServerId() {
        return this.config.getString("database.redis.distributed-sync.noise-master.master-server");
    }
    
    public int getNoiseMasterHealthCheckInterval() {
        return this.config.getInt("database.redis.distributed-sync.noise-master.health-check-interval");
    }
    
    public int getNoiseMasterTimeout() {
        return this.config.getInt("database.redis.distributed-sync.noise-master.master-timeout");
    }
}