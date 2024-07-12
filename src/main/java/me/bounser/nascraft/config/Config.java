package me.bounser.nascraft.config;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.DatabaseType;
import me.bounser.nascraft.market.funds.FundsManager;
import me.bounser.nascraft.market.funds.Strategy;
import me.bounser.nascraft.sellwand.Wand;
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
import java.util.*;

public class Config {

    private final FileConfiguration config;
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

        DatabaseManager.get().getDatabase().saveEverything();

        items = setupFile("items.yml");
        categories = setupFile("categories.yml");
        // investments = setupFile("investments.yml");

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

    public Boolean getCheckResources() {
        return config.getBoolean("auto-resources-injection");
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

    public float[] getLimits() {
        float[] limit = new float[2];
        limit[0] = (float) config.getDouble("price-options.limits.low");
        limit[1] = (float) config.getDouble("price-options.limits.high");
        return limit;
    }

    public boolean getPriceNoise() {
        return config.getBoolean("price-options.noise.enabled");
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

    public boolean getMarketPermissionRequirement() {
        return config.getBoolean("market-control.market-permission");
    }

    public List<String> getCommands() {
        return config.getStringList("commands.enabled");
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
                cooldown = config.getInt("sell-wands.wands." + name + ".permission");
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
                    estimate
            ));
        }

        return wands;
    }

    public int getPlaceholderPrecission() {
        return config.getInt("placeholders.decimal-precision");
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

    public String getChannel() {
        return config.getString("discord-bot.main-menu.channel");
    }

    public String getAdminRoleID() {
        return config.getString("discord-bot.admin-role-id");
    }

    public int getDefaultSlots() {
        return config.getInt("discord-bot.main-menu.default-inventory");
    }

    public float getSlotPriceFactor() {
        return (float) config.getDouble("discord-bot.main-menu.slot-price-factor");
    }

    public float getSlotPriceBase() {
        return (float) config.getDouble("discord-bot.main-menu.slot-price-base");
    }

    public Material getDiscordInvFillersMaterial() {
        return Material.getMaterial(config.getString("discord-bot.main-menu.in-game-gui.fillers.material").toUpperCase());
    }

    public Material getDiscordInvLockedMaterial() {
        return Material.getMaterial(config.getString("discord-bot.main-menu.in-game-gui.locked.material").toUpperCase());
    }

    public int getDiscordInvInfoSlot() {
        return config.getInt("discord-bot.main-menu.in-game-gui.info.slot");
    }

    public String getDiscordInvInfoTexture() {
        return config.getString("discord-bot.main-menu.in-game-gui.info.texture");
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

            childs.add(new Item(parent, multiplier, itemStack, childIdentifier, alias));
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
            if (categories.getList("categories." + category.getIdentifier() + ".items").contains(identifier)) {
                return category;
            }
        }
        return null;
    }

    public void setupFunds() {

        for (String identifier : investments.getConfigurationSection("investments.funds.").getKeys(false)) {

            HashMap<Strategy, Float> weightedStrategy = new HashMap<>();

            for (String strategyString : investments.getConfigurationSection("investments.funds." + identifier + ".strategy.").getKeys(false)) {

                Strategy strategy = null;

                try {
                    strategy = Strategy.valueOf(strategyString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    Nascraft.getInstance().getLogger().warning("Strategy " + strategyString + " is not a valid strategy!");
                    Nascraft.getInstance().getPluginLoader().disablePlugin(Nascraft.getInstance());
                }

                if (strategy != null)
                    weightedStrategy.put(
                            strategy,
                            (float) investments.getDouble("investments.funds." + identifier + ".strategy." + strategyString + ".weight")
                    );
            }
            FundsManager.getInstance().createFund(identifier, weightedStrategy);
        }
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

    public boolean getInformationMenuEnabled() {
        return inventorygui.getBoolean("main-menu.information.enabled");
    }

    public int getInformationSlot() {
        return inventorygui.getInt("main-menu.information.slot");
    }

    public Material getInformationMaterial() {
        return Material.getMaterial(inventorygui.getString("main-menu.information.material").toUpperCase());
    }

    public boolean getDiscordMarketMenuEnabled() {
        return inventorygui.getBoolean("main-menu.discord.enabled");
    }

    public int getDiscordSlot() {
        return inventorygui.getInt("main-menu.discord.slot");
    }

    public Material getDiscordMaterial(boolean linked) {
        if (linked)
            return Material.getMaterial(inventorygui.getString("main-menu.discord.linked.material").toUpperCase());
        return Material.getMaterial(inventorygui.getString("main-menu.discord.not-linked.material").toUpperCase());
    }

    public Material getFillersMaterial() {
        return Material.getMaterial(inventorygui.getString("main-menu.fillers.material").toUpperCase());
    }

    public List<Integer> getFillersSlots() {
        return inventorygui.getIntegerList("main-menu.fillers.slots");
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
        return inventorygui.getBoolean("buy-sell.alerts.enabled");
    }

    public int getAlertsBuySellSlot() {
        return inventorygui.getInt("buy-sell.alerts.slot");
    }

    public Material getAlertsBuySellMaterial() {
        return Material.getMaterial(inventorygui.getString("buy-sell.alerts.material").toUpperCase());
    }

    public boolean getInfoBuySellEnabled() {
        return inventorygui.getBoolean("buy-sell.info.enabled");
    }

    public int getInfoBuySellSlot() {
        return inventorygui.getInt("buy-sell.info.slot");
    }

    public Material getInfoBuySellMaterial() {
        return Material.getMaterial(inventorygui.getString("buy-sell.info.material").toUpperCase());
    }

    public boolean getBuySellBackEnabled() {
        return inventorygui.getBoolean("buy-sell.back-button.enabled");
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

}