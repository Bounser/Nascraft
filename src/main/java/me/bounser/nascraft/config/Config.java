package me.bounser.nascraft.config;

import de.tr7zw.changeme.nbtapi.NBT;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.DatabaseType;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.managers.currencies.CurrencyType;
import me.bounser.nascraft.market.limitorders.Duration;
import me.bounser.nascraft.sellwand.Wand;
import me.bounser.nascraft.managers.currencies.Currency;
import me.bounser.nascraft.discord.linking.LinkingMethod;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.time.LocalTime;
import java.util.*;

public class Config {

    private FileConfiguration config;
    private FileConfiguration items;
    private FileConfiguration categories;
    private FileConfiguration inventorygui;

    private FileConfiguration investments;
    private static Config instance;
    private Nascraft main;

    public static Config getInstance() {
        return instance == null ? instance = new Config() : instance;
    }

    private Config() {
        main = Nascraft.getInstance();
        main.saveDefaultConfig();
        this.config = Nascraft.getInstance().getConfig();

        items = setupFile("items.yml");
        categories = setupFile("categories.yml");
        inventorygui = setupFile("inventorygui.yml");
        // investments = setupFile("investments.yml");
    }

    public YamlConfiguration setupFile(String name) {

        File file = new File(main.getDataFolder(), name);

        if (!file.exists()) main.saveResource(name, false);

        return YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {

        config = YamlConfiguration.loadConfiguration(new File(main.getDataFolder(), "config.yml"));

        DatabaseManager.get().getDatabase().saveEverything();

        items = setupFile("items.yml");
        categories = setupFile("categories.yml");

        MarketManager.getInstance().reload();
    }

    public FileConfiguration getItemsFileConfiguration() {
        return items;
    }

    public File getItemsFile() {
        return new File(main.getDataFolder(), "items.yml");
    }

    public FileConfiguration getCategoriesFileConfiguration() {
        return categories;
    }

    public File getCategoriesFile() {
        return new File(main.getDataFolder(), "categories.yml");
    }

    // Config:

    public Boolean getWebEnabled() {
        if (config.contains("web.enabled")) {
            return config.getBoolean("web.enabled");
        } else {
            return false;
        }
    }

    public int getWebPort() {
        if (config.contains("web.port")) {
            return config.getInt("web.port");
        } else {
            return 8080;
        }
    }

    public Boolean getCheckResources() {
        return config.getBoolean("auto-resources-injection");
    }

    public List<String> getIgnoredKeys() {
        return config.getStringList("ignored-keys");
    }

    public float getLayoutCooldown() {
        if (config.contains("layout-cooldown")) {
            return (float) config.getDouble("layout-cooldown");
        }
        return 1;
    }

    public DatabaseType getDatabaseType() {

        DatabaseType databaseType = null;

        try {
            databaseType = DatabaseType.valueOf(config.getString("database.type").toUpperCase());
        } catch (IllegalArgumentException e) {

            Nascraft.getInstance().getLogger().warning("Error trying to recognize database type: " + config.getString("database.type").toUpperCase());
            Nascraft.getInstance().getLogger().warning("Is it a valid type of database?");
            e.printStackTrace();
            Nascraft.getInstance().getPluginLoader().disablePlugin(Nascraft.getInstance());
        }

        if (databaseType == null) {
            Nascraft.getInstance().getLogger().warning("DatabaseManager type not recognized: " + config.getString("database.type").toUpperCase());
            Nascraft.getInstance().getPluginLoader().disablePlugin(Nascraft.getInstance());
        }

        return databaseType;
    }

    public String getHost() {
        return config.getString("database.mysql.host");
    }

    public int getPort() {
        return config.getInt("database.mysql.port");
    }

    public String getDatabase() {
        return config.getString("database.mysql.database");
    }

    public String getUser() {
        return config.getString("database.mysql.user");
    }

    public String getPassword() {
        return config.getString("database.mysql.password");
    }

    public int getDatabasePurgeDays() {
        return config.getInt("database.days-until-history-removed");
    }

    public String getSelectedLanguage() {
        return config.getString("language");
    }

    public List<Currency> getCurrencies() {

        List<Currency> currencies = new ArrayList<>();

        for (String supplier : config.getConfigurationSection("currencies.suppliers").getKeys(false)) {

            if (supplier.equals("vault")) {
                currencies.add(
                        new Currency(
                                "vault",
                                CurrencyType.VAULT,
                                null,
                                null,
                                config.getString("currencies.suppliers.vault.not-enough"),
                                config.getString("currencies.suppliers.vault.format"),
                                null,
                                config.getInt("currencies.suppliers.vault.decimal-positions"),
                                config.getDouble("currencies.suppliers.vault.top-limit"),
                                config.getDouble("currencies.suppliers.vault.low-limit")));
            } else {
                currencies.add(
                        new Currency(
                                supplier,
                                CurrencyType.CUSTOM,
                                config.getString("currencies.suppliers." + supplier + ".deposit-cmd"),
                                config.getString("currencies.suppliers." + supplier + ".withdraw-cmd"),
                                config.getString("currencies.suppliers." + supplier + ".not-enough"),
                                config.getString("currencies.suppliers." + supplier + ".format"),
                                config.getString("currencies.suppliers." + supplier + ".balance-placeholder"),
                                config.getInt("currencies.suppliers." + supplier + ".decimal-positions"),
                                config.getDouble("currencies.suppliers." + supplier + ".top-limit"),
                                config.getDouble("currencies.suppliers." + supplier + ".low-limit")));
            }
        }

        return currencies;
    }

    public String getDefaultCurrencyIdentifier() {
        return config.getString("currencies.default-currency");
    }

    public String getCurrency(String identifier) {

        if (items.contains("items." + identifier + ".currency")) {
            return items.getString("items." + identifier + ".currency");
        }
        return config.getString("currencies.default-currency");
    }

    public float[] getLimits() {
        float[] limit = new float[2];
        limit[0] = (float) config.getDouble("price-options.limits.low");
        limit[1] = (float) config.getDouble("price-options.limits.high");
        return limit;
    }

    public boolean getPriceNoise() {
        return config.getBoolean("price-options.noise.enabled");
    }

    public int getNoiseTime() {
        if (!config.contains("price-options.noise.time")) return 60;
        return config.getInt("price-options.noise.time");
    }

    public boolean isMarketClosed() {
        if (!config.contains("market-control.closed")) return false;
        return config.getBoolean("market-control.closed");
    }

    public void setMarketOpen() {
        config.set("market-control.closed", true);
    }

    public void setMarketClosed() {
        config.set("market-control.closed", false);
    }

    public float getTaxBuy(String identifier) {

        if (items.contains("items." + identifier + ".tax.buy")) {
            return 1 + (float) items.getDouble("items." + identifier + ".tax.buy");
        } else {
            return 1 + (float) config.getDouble("market-control.taxation.buy");
        }
    }

    public float getTaxSell(String identifier) {

        if (items.contains("items." + identifier + ".tax.sell")) {
            return 1 - (float) items.getDouble("items." + identifier + ".tax.sell");
        } else {
            return 1 - (float) config.getDouble("market-control.taxation.sell");
        }
    }

    public boolean takeIntoAccountTax() {
        if (!config.contains("market-control.taxation.take-into-account-taxes")) return false;
        return config.getBoolean("market-control.taxation.take-into-account-taxes");
    }

    public boolean getMarketPermissionRequirement() {
        return config.getBoolean("market-control.market-permission");
    }

    public String getCommandAlias(String command) {
        return config.getString("commands." + command + ".alias");
    }

    public boolean isCommandEnabled(String command) {
        return config.getBoolean("commands." + command + ".enabled");
    }

    public int getGetSellMenuSize() {
        return config.getInt("commands.sell-menu.size");
    }

    public boolean getHelpEnabled() {
        return config.getBoolean("commands.sell-menu.help.enabled");
    }

    public int getHelpSlot() {
        return config.getInt("commands.sell-menu.help.slot");
    }

    public String getHelpTexture() {
        return config.getString("commands.sell-menu.help.texture");
    }

    public Material getFillerMaterial() {
        return Material.getMaterial(config.getString("commands.sell-menu.filler.material").toUpperCase());
    }

    public int getSellButtonSlot() {
        return config.getInt("commands.sell-menu.sell-button.slot");
    }

    public Material getSellButtonMaterial() {
        return Material.getMaterial(config.getString("commands.sell-menu.sell-button.material").toUpperCase());
    }


    public boolean getCloseButtonEnabled() {
        return config.getBoolean("commands.sell-menu.close-button.enabled");
    }

    public int getCloseButtonSlot() {
        return config.getInt("commands.sell-menu.close-button.slot");
    }

    public Material getCloseButtonMaterial() {
        return Material.getMaterial(config.getString("commands.sell-menu.close-button.material").toUpperCase());
    }


    public boolean getSellWandsEnabled() {
        return config.getBoolean("sell-wands.enabled");
    }

    public boolean getSellWandsPermissionNeeded(String wandName) {
        return config.contains("sell-wands." + wandName + ".permission");
    }

    public String getSellWandPermission(String wandName) {
        return config.getString("sell-wands.wands." + wandName + ".permission");
    }

    public List<Wand> getWands() {

        List<Wand> wands = new ArrayList<>();

        for (String name : config.getConfigurationSection("sell-wands.wands").getKeys(false)) {

            float multiplier = 1;
            if (config.contains("sell-wands.wands." + name + ".multiplier")) {
                multiplier = (float) config.getDouble("sell-wands.wands." + name + ".multiplier");
            }

            int uses = -1;
            if (config.contains("sell-wands.wands." + name + ".uses")) {
                uses = config.getInt("sell-wands.wands." + name + ".uses");
            }

            float maxProfit = -1;
            if (config.contains("sell-wands.wands." + name + ".max-profit")) {
                maxProfit = (float) config.getDouble("sell-wands.wands." + name + ".max-profit");
            }

            boolean enchanted = false;
            if (config.contains("sell-wands.wands." + name + ".enchanted")) {
                enchanted = config.getBoolean("sell-wands.wands." + name + ".enchanted");
            }

            int cooldown = 3;
            if (config.contains("sell-wands.wands." + name + ".cooldown")) {
                cooldown = config.getInt("sell-wands.wands." + name + ".cooldown");
            }

            String permission = null;
            if (config.contains("sell-wands.wands." + name + ".permission")) {
                permission = config.getString("sell-wands.wands." + name + ".permission");
            }

            Action sell = Action.LEFT_CLICK_BLOCK;
            if (config.contains("sell-wands.wands." + name + ".sell")) {
                switch (config.getString("sell-wands.wands." + name + ".sell").toLowerCase()) {
                    case "left":
                        sell = Action.LEFT_CLICK_BLOCK;
                        break;
                    case "right":
                        sell = Action.RIGHT_CLICK_BLOCK;
                        break;
                    case "none":
                        sell = null;
                }
            }

            Action estimate = Action.RIGHT_CLICK_BLOCK;
            if (config.contains("sell-wands.wands." + name + ".estimate")) {
                switch (config.getString("sell-wands.wands." + name + ".estimate").toLowerCase()) {
                    case "left":
                        estimate = Action.LEFT_CLICK_BLOCK;
                        break;
                    case "right":
                        estimate = Action.RIGHT_CLICK_BLOCK;
                        break;
                    case "none":
                        estimate = null;
                }
            }

            List<Currency> currencies = new ArrayList<>();

            if (config.contains("sell-wands.wands." + name + ".currencies")) {
                for (String currencyIdentifier : config.getStringList("sell-wands.wands." + name + ".currencies")) {
                    currencies.add(CurrenciesManager.getInstance().getCurrency(currencyIdentifier.toLowerCase()));
                }
            } else {
                currencies.addAll(CurrenciesManager.getInstance().getCurrencies());
            }

            wands.add(new Wand(name,
                    Material.getMaterial(config.getString("sell-wands.wands." + name + ".material").toUpperCase()),
                    config.getString("sell-wands.wands." + name + ".display-name"),
                    config.getStringList("sell-wands.wands." + name + ".lore"),
                    uses,
                    multiplier,
                    maxProfit,
                    cooldown,
                    enchanted,
                    permission,
                    sell,
                    estimate,
                    currencies
            ));
        }

        return wands;
    }

    public boolean getDiscordEnabled() {
        return config.getBoolean("discord-bot.enabled");
    }

    public LinkingMethod getLinkingMethod() {
        return LinkingMethod.valueOf(config.getString("discord-bot..main-menu.link-method").toUpperCase());
    }

    public String getToken() {
        return config.getString("discord-bot.token");
    }

    public boolean getLogChannelEnabled() {
        return config.getBoolean("discord-bot.log-trades.enabled");
    }

    public String getLogChannel() {
        return config.getString("discord-bot.log-trades.channel");
    }

    public boolean getDiscordMenuEnabled() {
        return config.getBoolean("discord-bot.main-menu.enabled");
    }

    public int getUpdateTime() {
        if (!config.contains("discord-bot.main-menu.options.update-time")) return 60;
        return config.getInt("discord-bot.main-menu.options.update-time");
    }

    public boolean getOptionWikiEnabled() {
        return config.getBoolean("discord-bot.main-menu.options.wiki.enabled");
    }

    public boolean getOptionAlertEnabled() {
        return config.getBoolean("discord-bot.main-menu.options.alerts.enabled");
    }

    public int getAlertsDaysUntilExpired() {
        return config.getInt("discord-bot.main-menu.options.alerts.expiration");
    }

    public boolean getOptionGraphsEnabled() {
        return config.getBoolean("discord-bot.main-menu.options.detailed-graphs.enabled");
    }

    public boolean getOptionPersonalLogEnabled() {
        return config.getBoolean("discord-bot.main-menu.options.personal-log.enabled");
    }

    public boolean getOptionSelectionEnabled() {
        return config.getBoolean("discord-bot.main-menu.options.selection-bar.enabled");
    }

    public boolean getOptionCPIEnabled() {
        return config.getBoolean("discord-bot.main-menu.options.cpi.enabled");
    }

    public boolean getOptionCPIComparisonEnabled() {
        if (!config.contains("discord-bot.main-menu.options.cpi-comparison.enabled")) return true;
        return config.getBoolean("discord-bot.main-menu.options.cpi-comparison.enabled");
    }

    public boolean getOptionFlowsEnabled() {
        if (!config.contains("discord-bot.main-menu.options.flows.enabled")) return true;
        return config.getBoolean("discord-bot.main-menu.options.flows.enabled");
    }

    public String getChannel() {
        return config.getString("discord-bot.main-menu.channel");
    }

    public String getAdminRoleID() {
        return config.getString("discord-bot.admin-role-id");
    }

    public int getDefaultSlots() {
        return config.getInt("portfolio.default-size");
    }

    public int getPortfolioMaxStorage() {
        return config.getInt("portfolio.storage-limit");
    }

    public float getSlotPriceFactor() {
        return (float) config.getDouble("portfolio.slot-price-factor");
    }

    public float getSlotPriceBase() {
        return (float) config.getDouble("portfolio.slot-price-base");
    }

    public Material getPortfolioFillerMaterial() {
        return Material.getMaterial(config.getString("portfolio.in-game-gui.fillers.material").toUpperCase());
    }

    public Material getPortfolioLockedMaterial() {
        return Material.getMaterial(config.getString("portfolio.in-game-gui.locked.material").toUpperCase());
    }

    public boolean getLimitOrdersEnabled() {
        return config.getBoolean("limit-orders.enabled");
    }

    public int getMaxLimitOrdersPerPlayer() {
        return config.getInt("limit-orders.max-per-player");
    }

    public int getMaxLimitOrderSize() {
        return config.getInt("limit-orders.order-max-size");
    }

    public int getCheckingPeriod() {
        return config.getInt("limit-orders.checking-period");
    }

    public List<Duration> getDurations() {
        List<Duration> durations = new ArrayList<>();

        for (String duration : config.getConfigurationSection("limit-orders.durations.").getKeys(false)) {
            durations.add(
                    new Duration(
                            config.getInt("limit-orders.durations." + duration + ".duration"),
                            config.getString("limit-orders.durations." + duration + ".display"),
                            (float) config.getDouble("limit-orders.durations." + duration + ".fee"),
                            (float) config.getDouble("limit-orders.durations." + duration + ".min-fee")
                    )
            );
        }

        return durations;
    }

    public float getDiscordBuyTax() {

        if (config.getBoolean("discord-bot.main-menu.slot-price.taxation.override")) {
            return (float) (1 + config.getDouble("discord-bot.main-menu.slot-price.taxation.buy"));
        } else {
            return 1 + (float) config.getDouble("market-control.taxation.buy");
        }
    }

    public float getDiscordSellTax() {

        if (config.getBoolean("discord-bot.main-menu.slot-price.taxation.override")) {
            return (float) (1 - config.getDouble("discord-bot.main-menu.slot-price.taxation.sell"));
        } else {
            return 1 - (float) config.getDouble("market-control.taxation.sell");
        }
    }

    // Items:

    public Set<String> getAllMaterials() {
        return items.getConfigurationSection("items.").getKeys(false);
    }

    public float getInitialPrice(String identifier) {
        for (String item : getAllMaterials()) {
            if (identifier.equalsIgnoreCase(item)) {
                return (float) items.getDouble("items." + item + ".initial-price");
            }
        }
        return 1;
    }

    public boolean includeInCPI(Item item) {

        if (items.contains("items." + item.getIdentifier() + ".exclude-from-cpi"))
            return (!items.getBoolean("items." + item.getIdentifier() + ".exclude-from-cpi"));

        return true;
    }

    public List<Item> getChilds(String identifier) {

        List<Item> childs = new ArrayList<>();

        Set<String> section = null;

        if (items.getConfigurationSection("items." + identifier + ".child.") != null) {
            section = items.getConfigurationSection("items." + identifier + ".child.").getKeys(false);
        }

        Item parent = MarketManager.getInstance().getItem(identifier);

        if (section == null || section.isEmpty()) return childs;

        for (String childIdentifier : section) {

            ItemStack itemStack = getItemStackOfChild(identifier, childIdentifier);
            float multiplier = (float) items.getDouble("items." + identifier + ".child." + childIdentifier + ".multiplier");

            String alias = childIdentifier;

            if (items.contains("items." + identifier + ".child." + childIdentifier + ".alias")) {
                alias = items.getString("items." + identifier + ".child." + childIdentifier + ".alias");
            } else {
                alias = (Character.toUpperCase(alias.charAt(0)) + alias.substring(1)).replace("_", " ");
            }

            childs.add(new Item(parent, multiplier, itemStack, childIdentifier, alias, parent.getCurrency()));
        }

        return childs;
    }

    public ItemStack getItemStackOfItem(String identifier) {

        ItemStack itemStack = null;

        if (items.contains("items." + identifier + ".item-stack"))
            itemStack = items.getSerializable("items." + identifier + ".item-stack", ItemStack.class);

        if (itemStack == null)
            try {
                itemStack = new ItemStack(Material.getMaterial(identifier.replaceAll("\\d", "").toUpperCase()));
            } catch (IllegalArgumentException e) {
                Nascraft.getInstance().getLogger().severe("Couldn't load item with identifier: " + identifier);
                Nascraft.getInstance().getLogger().severe("Reason: Material " + identifier.replaceAll("\\d", "").toUpperCase() + " is not valid!");
                Nascraft.getInstance().getLogger().severe("Does the item exist in the version of your server?");
                Nascraft.getInstance().getPluginLoader().disablePlugin(Nascraft.getInstance());
            }

        for (String ignoredKey : getIgnoredKeys())
            NBT.modify(itemStack, nbt -> {
                nbt.removeKey(ignoredKey);
            });

        return itemStack;

    }

    public ItemStack getItemStackOfChild(String identifier, String childIdentifier) {

        ItemStack itemStack = null;

        if (items.contains("items." + identifier + ".childs." + childIdentifier + "item-stack"))
            itemStack = items.getSerializable("items." + identifier + ".childs." + childIdentifier + "item-stack", ItemStack.class);

        if (itemStack == null)
            try {
                itemStack = new ItemStack(Material.getMaterial(childIdentifier.replaceAll("\\d", "").toUpperCase()));
            } catch (IllegalArgumentException e) {
                Nascraft.getInstance().getLogger().severe("Couldn't load item with identifier: " + identifier);
                Nascraft.getInstance().getLogger().severe("Reason: Material " + identifier.replaceAll("\\d", "").toUpperCase() + " is not valid!");
                Nascraft.getInstance().getLogger().severe("Does the item exist in the version of your server?");
                Nascraft.getInstance().getPluginLoader().disablePlugin(Nascraft.getInstance());
            }

        return itemStack;
    }

    public boolean hasAlias(String identifier) {
        return items.contains("items." + identifier + ".alias");
    }

    public String getAlias(String identifier) {
        if (!items.contains("items." + identifier + ".alias")) {
            return (Character.toUpperCase(identifier.charAt(0)) + identifier.substring(1)).replace("_", " ");
        } else {
            return items.getString("items." + identifier + ".alias");
        }
    }

    public float getSupport(String identifier) {
        if (items.contains("items." + identifier + ".support")) {
            return (float) items.getDouble("items." + identifier + ".support");
        }
        return 0;
    }

    public float getResistance(String identifier) {
        if (items.contains("items." + identifier + ".resistance")) {
            return (float) items.getDouble("items." + identifier + ".resistance");
        }
        return 0;
    }

    public float getElasticity(String identifier) {
        if (items.contains("items." + identifier + ".elasticity")) {
            return (float) items.getDouble("items." + identifier + ".elasticity");
        }
        return (float) config.getDouble("price-options.default-elasticity");
    }

    public float getNoiseIntensity(String identifier) {
        if (items.contains("items." + identifier + ".noise-intensity")) {
            return (float) items.getDouble("items." + identifier + ".noise-intensity");
        }
        return (float) config.getDouble("price-options.noise.default-intensity");
    }

    public boolean getRestricted(String identifier) {
        if (items.contains("items." + identifier + ".limit.restricted")) {
            return items.getBoolean("items." + identifier + ".limit.restricted");
        }
        return true;
    }

    public double getLowLimit(String identifier) {
        if (items.contains("items." + identifier + ".limit.low")) {
            return items.getDouble("items." + identifier + ".limit.low");
        }
        return -1;
    }

    public double getHighLimit(String identifier) {
        if (items.contains("items." + identifier + ".limit.high")) {
            return items.getDouble("items." + identifier + ".limit.high");
        }
        return -1;
    }

    public float getNoiseMultiplier() {
        if (config.contains("price-options.noise.intensity-multiplier")) {
            return (float) config.getDouble("price-options.noise.intensity-multiplier");
        }
        return 1;
    }

    public float getElasticityMultiplier() {
        if (config.contains("price-options.elasticity-multiplier")) {
            return (float) config.getDouble("price-options.elasticity-multiplier");
        }
        return 1;
    }

    // Categories:

    public Set<String> getCategories() {
        return categories.getConfigurationSection("categories.").getKeys(false);
    }

    public String getDisplayName(Category category) {
        return categories.getString("categories." + category.getIdentifier() + ".display-name");
    }

    public Material getMaterialOfCategory(Category category) {

        if (categories.contains("categories." + category.getIdentifier() + ".display-material")) {

            try {
                return Material.valueOf(categories.getString("categories." + category.getIdentifier() + ".display-material").toUpperCase());
            } catch (IllegalArgumentException exception) {
                Nascraft.getInstance().getLogger().warning(ChatColor.RED + "Category " + category.getIdentifier() + " doesn't have a valid display material.");
                return Material.STONE;
            }
        }

        return Material.STONE;

    }

    public Category getCategoryFromMaterial(String identifier) {

        for (Category category : MarketManager.getInstance().getCategories()) {
            if (categories.contains("categories." + category.getIdentifier() + ".items")) {
                if (categories.getList("categories." + category.getIdentifier() + ".items").contains(identifier)) {
                    return category;
                }
            }
        }
        return null;
    }

    public int getMainMenuSize() {
        return inventorygui.getInt("main-menu.size");
    }

    public boolean getAlertsMenuEnabled() {
        return inventorygui.getBoolean("main-menu.alerts.enabled");
    }

    public int getAlertsSlot() {
        return inventorygui.getInt("main-menu.alerts.slot");
    }

    public Material getAlertsMaterial(boolean linked) {

        String path = "main-menu.alerts." + (linked ? "linked" : "not-linked") + ".material";

        return Material.getMaterial(inventorygui.getString(path).toUpperCase());
    }

    public boolean getLimitOrdersMenuEnabled() {
        return inventorygui.getBoolean("main-menu.limit-orders.enabled");
    }

    public int getLimitOrdersSlot() {
        return inventorygui.getInt("main-menu.limit-orders.slot");
    }

    public Material getLimitOrdersMaterial() {
        return Material.getMaterial(inventorygui.getString("main-menu.limit-orders.material").toUpperCase());
    }

    public boolean getInformationMenuEnabled() {
        return inventorygui.getBoolean("main-menu.information.enabled");
    }

    public int getInformationSlot() {
        return inventorygui.getInt("main-menu.information.slot");
    }

    public Material getInformationMaterial() {
        return Material.getMaterial(inventorygui.getString("main-menu.information.material").toUpperCase());
    }

    public boolean getPortfolioMarketMenuEnabled() {
        return inventorygui.getBoolean("main-menu.portfolio.enabled");
    }

    public int getPortfolioSlot() {
        return inventorygui.getInt("main-menu.portfolio.slot");
    }

    public Material getPortfolioMaterial(boolean linked) {
        if (linked)
            return Material.getMaterial(inventorygui.getString("main-menu.portfolio.linked.material").toUpperCase());
        return Material.getMaterial(inventorygui.getString("main-menu.portfolio.not-linked.material").toUpperCase());
    }

    public boolean getTrendsEnabled() {
        return inventorygui.getBoolean("main-menu.trends.enabled");
    }

    public int getTrendsSlot() {
        return inventorygui.getInt("main-menu.trends.slot");
    }

    public Material getTrendsMaterial() {
        return Material.getMaterial(inventorygui.getString("main-menu.trends.material").toUpperCase());
    }

    public HashMap<Material, List<Integer>> getMainMenuFillers() {

        HashMap<Material, List<Integer>> fills = new HashMap<>();

        for (String section : inventorygui.getConfigurationSection("main-menu.fillers.").getKeys(false)) {

            Material material = Material.valueOf(section.toUpperCase());

            fills.put(material, inventorygui.getIntegerList("main-menu.fillers." + section));
        }

        return fills;
    }

    public boolean getSetCategorySegments() {
        if (!inventorygui.contains("main-menu.categories.item-list")) return true;
        return inventorygui.getBoolean("main-menu.categories.item-list");
    }

    public List<Integer> getCategoriesSlots() {
        return inventorygui.getIntegerList("main-menu.categories.slots");
    }

    public int getCategoriesMenuSize() {
        return inventorygui.getInt("category-section.size");
    }

    public int getCategoryItemSlot() {
        return inventorygui.getInt("category-section.category-item.slot");
    }

    public boolean getCategoryBackEnabled() {
        return inventorygui.getBoolean("category-section.back-button.enabled");
    }

    public int getCategoryBackSlot() {
        return inventorygui.getInt("category-section.back-button.slot");
    }

    public Material getCategoryBackMaterial() {
        return Material.getMaterial(inventorygui.getString("category-section.back-button.material").toUpperCase());
    }

    public int getCategoryNextSlot() {
        return inventorygui.getInt("category-section.next-button.slot");
    }

    public Material getCategoryNextMaterial() {
        return Material.getMaterial(inventorygui.getString("category-section.next-button.material").toUpperCase());
    }

    public Material getCategoryFillersMaterial() {
        return Material.getMaterial(inventorygui.getString("category-section.fillers.material").toUpperCase());
    }

    public List<Integer> getCategoryFillersSlots() {
        return inventorygui.getIntegerList("category-section.fillers.slots");
    }

    public List<Integer> getCategoryItemsSlots() {
        return inventorygui.getIntegerList("category-section.items.slots");
    }

    public int getBuySellMenuSize() {
        return inventorygui.getInt("buy-sell.size");
    }

    public int getBuySellMenuItemSlot() { return inventorygui.getInt("buy-sell.item.slot"); }


    public boolean getAlertsBuySellEnabled() {
        return inventorygui.getInt("buy-sell.alerts.slot") != -1;
    }

    public int getAlertsBuySellSlot() {
        return inventorygui.getInt("buy-sell.alerts.slot");
    }

    public Material getAlertsBuySellMaterial() {
        return Material.getMaterial(inventorygui.getString("buy-sell.alerts.material").toUpperCase());
    }

    public boolean getLimitOrdersBuySellEnabled() {
        return inventorygui.getInt("buy-sell.limit-orders.slot") != -1;
    }

    public int getLimitOrdersBuySellSlot() {
        return inventorygui.getInt("buy-sell.limit-orders.slot");
    }

    public Material getLimitOrdersBuySellMaterial() {
        return Material.getMaterial(inventorygui.getString("buy-sell.limit-orders.material").toUpperCase());
    }

    public boolean getInfoBuySellEnabled() {
        return inventorygui.getInt("buy-sell.info.slot") != -1;
    }

    public int getInfoBuySellSlot() {
        return inventorygui.getInt("buy-sell.info.slot");
    }

    public Material getInfoBuySellMaterial() {
        return Material.getMaterial(inventorygui.getString("buy-sell.info.material").toUpperCase());
    }

    public boolean getBuySellBackEnabled() {
        return inventorygui.getInt("buy-sell.back-button.slot") != -1;
    }

    public int getBuySellBackSlot() {
        return inventorygui.getInt("buy-sell.back-button.slot");
    }

    public Material getBuySellBackMaterial() {
        return Material.getMaterial(inventorygui.getString("buy-sell.back-button.material").toUpperCase());
    }

    public List<Integer> getBuySellFillersSlots() {
        return inventorygui.getIntegerList("buy-sell.fillers.slots");
    }

    public Material getBuySellFillersMaterial() {
        return Material.getMaterial(inventorygui.getString("buy-sell.fillers.material").toUpperCase());
    }

    //

    public Material getBuySellBuyMaterial() {
        return Material.getMaterial(inventorygui.getString("buy-sell.buy-buttons.material").toUpperCase());
    }

    public HashMap<Integer, Integer> getBuySellBuySlots() {

        HashMap<Integer, Integer> buttons = new HashMap<>();

        for (String weight : inventorygui.getConfigurationSection("buy-sell.buy-buttons.buttons").getKeys(false)) {
            int slot = inventorygui.getInt("buy-sell.buy-buttons.buttons." + weight + ".slot");
            buttons.put(Integer.valueOf(weight), slot);
        }

        return buttons;
    }

    //


    public Material getBuySellSellMaterial() {
        return Material.getMaterial(inventorygui.getString("buy-sell.sell-buttons.material").toUpperCase());
    }

    public HashMap<Integer, Integer> getBuySellSellSlots() {

        HashMap<Integer, Integer> buttons = new HashMap<>();

        for (String weight : inventorygui.getConfigurationSection("buy-sell.sell-buttons.buttons").getKeys(false)) {
            int slot = inventorygui.getInt("buy-sell.sell-buttons.buttons." + weight + ".slot");
            buttons.put(Integer.valueOf(weight), slot);
        }

        return buttons;
    }

    public int getAlertsMenuSize() {
        return inventorygui.getInt("alerts.size");
    }

    public boolean getAlertsMenuBackEnabled() {
        return inventorygui.getBoolean("alerts.back-button.enabled");
    }

    public int getAlertsMenuBackSlot() {
        return inventorygui.getInt("alerts.back-button.slot");
    }

    public Material getAlertsMenuBackMaterial() {
        return Material.getMaterial(inventorygui.getString("alerts.back-button.material").toUpperCase());
    }

    public Material getAlertsMenuFillersMaterial() {
        return Material.getMaterial(inventorygui.getString("alerts.fillers.material").toUpperCase());
    }

    public List<Integer> getAlertsMenuFillersSlots() {
        return inventorygui.getIntegerList("alerts.fillers.slots");
    }

    public List<Integer> getAlertsMenuSlots() {
        return inventorygui.getIntegerList("alerts.alerts.slots");
    }

    public int getLimitOrdersMenuSize() {
        return inventorygui.getInt("limit-orders.size");
    }

    public boolean getLimitOrdersMenuBackEnabled() {
        return inventorygui.getBoolean("limit-orders.back-button.enabled");
    }

    public int getLimitOrdersMenuBackSlot() {
        return inventorygui.getInt("limit-orders.back-button.slot");
    }

    public Material getLimitOrdersMenuBackMaterial() {
        return Material.getMaterial(inventorygui.getString("limit-orders.back-button.material").toUpperCase());
    }

    public Material getLimitOrdersMenuFillersMaterial() {
        return Material.getMaterial(inventorygui.getString("limit-orders.fillers.material").toUpperCase());
    }

    public List<Integer> getLimitOrdersMenuFillersSlots() {
        return inventorygui.getIntegerList("limit-orders.fillers.slots");
    }

    public List<Integer> getLimitOrdersMenuSlots() {
        return inventorygui.getIntegerList("limit-orders.orders.slots");
    }

    public int getSetLimitOrderMenuSize() {
        return inventorygui.getInt("set-limit-orders.size");
    }

    public boolean getSetLimitOrderMenuBackEnabled() {
        return inventorygui.getInt("set-limit-orders.back-button.slot") != -1;
    }

    public int getSetLimitOrderMenuBackSlot() {
        return inventorygui.getInt("set-limit-orders.back-button.slot");
    }

    public Material getSetLimitOrderMenuBackMaterial() {
        return Material.getMaterial(inventorygui.getString("set-limit-orders.back-button.material").toUpperCase());
    }

    public int getSetLimitOrderMenuItemSlot() {
        return inventorygui.getInt("set-limit-orders.item.slot");
    }

    public int getSetLimitOrderMenuTimeSlot() {
        return inventorygui.getInt("set-limit-orders.time.slot");
    }

    public Material getSetLimitOrderMenuTimeMaterial() {
        return Material.getMaterial(inventorygui.getString("set-limit-orders.time.material").toUpperCase());
    }

    public int getSetLimitOrderMenuPriceSlot() {
        return inventorygui.getInt("set-limit-orders.price.slot");
    }

    public Material getSetLimitOrderMenuPriceMaterial() {
        return Material.getMaterial(inventorygui.getString("set-limit-orders.price.material").toUpperCase());
    }

    public int getSetLimitOrderMenuQuantitySlot() {
        return inventorygui.getInt("set-limit-orders.quantity.slot");
    }

    public Material getSetLimitOrderMenuQuantityMaterial() {
        return Material.getMaterial(inventorygui.getString("set-limit-orders.quantity.material").toUpperCase());
    }

    public int getSetLimitOrderMenuConfirmSellSlot() {
        return inventorygui.getInt("set-limit-orders.confirm-sell.slot");
    }

    public Material getSetLimitOrderMenuConfirmSellMaterial() {
        return Material.getMaterial(inventorygui.getString("set-limit-orders.confirm-sell.material").toUpperCase());
    }

    public int getSetLimitOrderMenuConfirmBuySlot() {
        return inventorygui.getInt("set-limit-orders.confirm-buy.slot");
    }

    public Material getSetLimitOrderMenuConfirmBuyMaterial() {
        return Material.getMaterial(inventorygui.getString("set-limit-orders.confirm-buy.material").toUpperCase());
    }

    public Material getSetLimitOrdersMenuFillersMaterial() {
        return Material.getMaterial(inventorygui.getString("set-limit-orders.fillers.material").toUpperCase());
    }

    public List<Integer> getSetLimitOrdersMenuFillersSlots() {
        return inventorygui.getIntegerList("set-limit-orders.fillers.slots");
    }

    public boolean getPortfolioMenuBackEnabled() {
        return inventorygui.getBoolean("portfolio.back-button.enabled");
    }

    public int getPortfolioMenuBackSlot() {
        return inventorygui.getInt("portfolio.back-button.slot");
    }

    public Material getPortfolioMenuBackMaterial() {
        return Material.getMaterial(inventorygui.getString("portfolio.back-button.material").toUpperCase());
    }

    public boolean getPortfolioInfoEnabled() {
        return inventorygui.getBoolean("portfolio.info.enabled");
    }

    public int getPortfolioInfoSlot() {
        return inventorygui.getInt("portfolio.info.slot");
    }

    public Material getPortfolioDebtMaterial() {
        return Material.getMaterial(inventorygui.getString("portfolio.loan.material").toUpperCase());
    }

    public boolean getPortfolioDebtEnabled() {
        return inventorygui.getBoolean("portfolio.loan.enabled");
    }

    public int getPortfolioDebtSlot() {
        return inventorygui.getInt("portfolio.loan.slot");
    }

    public int getDebtSize() {
        return inventorygui.getInt("portfolio.debt.size");
    }

    public boolean getDebtBackEnabled() {
        return inventorygui.getBoolean("portfolio.debt.back-button.enabled");
    }

    public int getDebtBackSlot() {
        return inventorygui.getInt("portfolio.debt.back-button.slot");
    }

    public Material getDebtBackMaterial() {
        return Material.getMaterial(inventorygui.getString("portfolio.debt.back-button.material").toUpperCase());
    }

    public boolean getDebtExpEnabled() {
        return inventorygui.getBoolean("portfolio.debt.exp.enabled");
    }

    public int getDebtExpSlot() {
        return inventorygui.getInt("portfolio.debt.exp.slot");
    }

    public Material getDebtExpMaterial() {
        return Material.getMaterial(inventorygui.getString("portfolio.debt.exp.material").toUpperCase());
    }

    public boolean getDebtInfoEnabled() {
        return inventorygui.getBoolean("portfolio.debt.info.enabled");
    }

    public int getDebtInfoSlot() {
        return inventorygui.getInt("portfolio.debt.info.slot");
    }

    public Material getDebtInfoMaterial() {
        return Material.getMaterial(inventorygui.getString("portfolio.debt.info.material").toUpperCase());
    }

    public boolean getDebtRepayAllEnabled() {
        return inventorygui.getBoolean("portfolio.debt.repay-all.enabled");
    }

    public int getDebtRepayAllSlot() {
        return inventorygui.getInt("portfolio.debt.repay-all.slot");
    }

    public Material getDebtRepayAllMaterial() {
        return Material.getMaterial(inventorygui.getString("portfolio.debt.repay-all.material").toUpperCase());
    }

    public boolean getDebtRepayEnabled() {
        return inventorygui.getBoolean("portfolio.debt.repay-custom.enabled");
    }

    public int getDebtRepaySlot() {
        return inventorygui.getInt("portfolio.debt.repay-custom.slot");
    }

    public Material getDebtRepayMaterial() {
        return Material.getMaterial(inventorygui.getString("portfolio.debt.repay-custom.material").toUpperCase());
    }

    public boolean getDebtMaxLoanEnabled() {
        return inventorygui.getBoolean("portfolio.debt.get-max-loan.enabled");
    }

    public int getDebtMaxLoanSlot() {
        return inventorygui.getInt("portfolio.debt.get-max-loan.slot");
    }

    public Material getDebtMaxLoanMaterial() {
        return Material.getMaterial(inventorygui.getString("portfolio.debt.get-max-loan.material").toUpperCase());
    }

    public boolean getDebtCustomEnabled() {
        return inventorygui.getBoolean("portfolio.debt.custom-loan.enabled");
    }

    public int getDebtCustomSlot() {
        return inventorygui.getInt("portfolio.debt.custom-loan.slot");
    }

    public Material getDebtCustomMaterial() {
        return Material.getMaterial(inventorygui.getString("portfolio.debt.custom-loan.material").toUpperCase());
    }

    public List<Integer> getDebtFillersSlots() {
        return inventorygui.getIntegerList("portfolio.debt.fillers.slots");
    }

    public Material getDebtFillersMaterial() {
        return Material.getMaterial(inventorygui.getString("portfolio.debt.fillers.material").toUpperCase());
    }

    public boolean getLoansEnabled() {
        return config.getBoolean("portfolio.loans.enabled");
    }

    public double getLoansMaxSize() {
        return config.getDouble("portfolio.loans.max-size");
    }

    public double getLoansMinSize() {
        return config.getDouble("portfolio.loans.min-size");
    }

    public double getLoanSecurityMargin() {
        return config.getDouble("portfolio.loans.security-margin");
    }

    public int getMarginCheckingPeriod() {
        return config.getInt("portfolio.loans.margin-checking-period");
    }

    public LocalTime getInterestPaymentHour() {

        String dateFormatted = config.getString("portfolio.loans.interest-rate.when");

        assert dateFormatted != null;
        return LocalTime.parse(dateFormatted);
    }

    public double getLoansDailyInterest() {
        return config.getDouble("portfolio.loans.interest-rate.percentage");
    }

    public double getLoansMinimumInterest() {
        return config.getDouble("portfolio.loans.interest-rate.minimum");
    }

    public boolean getPortfolioTopEnabled() {
        return inventorygui.getBoolean("portfolio.leaderboard.enabled");
    }

    public int getPortfolioTopSlot() {
        return inventorygui.getInt("portfolio.leaderboard.slot");
    }

    public Material getPortfolioTopMaterial() {
        return Material.getMaterial(inventorygui.getString("portfolio.leaderboard.material").toUpperCase());
    }

    public int getTopSize() {
        return inventorygui.getInt("portfolio.top.size");
    }

    public boolean getTopBackEnabled() {
        return inventorygui.getBoolean("portfolio.top.back-button.enabled");
    }

    public int getTopBackSlot() {
        return inventorygui.getInt("portfolio.top.back-button.slot");
    }

    public Material getTopBackMaterial() {
        return Material.getMaterial(inventorygui.getString("portfolio.top.back-button.material").toUpperCase());
    }

    public HashMap<Integer, Integer> getTopPositions() {

        HashMap<Integer, Integer> positions = new HashMap<>();

        for (String pos : inventorygui.getConfigurationSection("portfolio.top.positions").getKeys(false)) {
            int slot = inventorygui.getInt("portfolio.top.positions." + pos + ".slot");
            positions.put(Integer.valueOf(pos), slot);
        }

        return positions;
    }

    public HashMap<Material, List<Integer>> getTopFillers() {

        HashMap<Material, List<Integer>> fillers = new HashMap<>();

        for (String filler : inventorygui.getConfigurationSection("portfolio.top.fillers").getKeys(false)) {
            List<Integer> slots = inventorygui.getIntegerList("portfolio.top.fillers." + filler + ".slots");
            Material material = Material.valueOf(inventorygui.getString("portfolio.top.fillers." + filler + ".material").toUpperCase());
            fillers.put(material, slots);
        }

        return fillers;
    }
}