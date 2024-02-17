package me.bounser.nascraft.config;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.DatabaseType;
import me.bounser.nascraft.database.SQLite;
import me.bounser.nascraft.market.funds.Fund;
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
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.units.qual.N;

import javax.xml.crypto.Data;
import java.io.File;
import java.util.*;

public class Config {

    private final FileConfiguration config;
    private FileConfiguration items;
    private FileConfiguration categories;
    private FileConfiguration investments;
    private FileConfiguration inventorygui;
    private static Config instance;
    private Nascraft main;


    public static Config getInstance() { return instance == null ? instance = new Config() : instance; }

    private Config() {
        main = Nascraft.getInstance();
        main.saveDefaultConfig();
        this.config = Nascraft.getInstance().getConfig();

        items = setupFile("items.yml");
        categories = setupFile("categories.yml");
        investments = setupFile("investments.yml");
        // inventorygui = setupFile("inventorygui.yml");
    }

    public YamlConfiguration setupFile(String name) {

        File file = new File(main.getDataFolder(), name);

        if (!file.exists()) main.saveResource(name, false);

        return YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {

        SQLite.getInstance().saveEverything();

        items = setupFile("items.yml");
        categories = setupFile("categories.yml");
        investments = setupFile("investments.yml");

        MarketManager.getInstance().reload();

    }
    public FileConfiguration getItemsFileConfiguration() { return items; }

    public File getItemsFile() { return new File(main.getDataFolder(), "items.yml");}

    public FileConfiguration getCategoriesFileConfiguration() { return categories; }

    public File getCategoriesFile() { return new File(main.getDataFolder(), "categories.yml");}

    // Config:

    public Boolean getCheckResources() { return config.getBoolean("auto-resources-injection"); }

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

    public String getHost() { return config.getString("database.mysql.host"); }
    public int getPort() { return config.getInt("database.mysql.port"); }
    public String getDatabase() { return config.getString("database.mysql.database"); }
    public String getUser() { return config.getString("database.mysql.user"); }
    public String getPassword() { return config.getString("database.mysql.password"); }

    public int getDatabasePurgeDays() { return config.getInt("database.days-until-history-removed"); }

    public String getSelectedLanguage() { return config.getString("language"); }

    public float[] getLimits() {
        float[] limit = new float[2];
        limit[0] = (float) config.getDouble("price-options.limits.low");
        limit[1] = (float) config.getDouble("price-options.limits.high");
        return limit;
    }

    public boolean getPriceNoise() { return config.getBoolean("price-options.noise.enabled"); }

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

    public boolean getMarketPermissionRequirement() { return config.getBoolean("market-control.market-permission"); }

    public List<String> getCommands() { return config.getStringList("commands.enabled"); }

    public boolean getSellWandsEnabled() { return config.getBoolean("sell-wands.enabled"); }

    public boolean getSellWandsPermissionNeeded(String wandName) { return config.contains("sell-wands." + wandName + ".permission"); }

    public String getSellWandPermission(String wandName) { return config.getString("sell-wands.wands." + wandName + ".permission"); }

    public List<Wand> getWands() {

        List<Wand> wands = new ArrayList<>();

        for (String name : config.getConfigurationSection("sell-wands.wands").getKeys(false)) {

            float multiplier = 1;
            if (config.contains("sell-wands.wands." + name +  ".multiplier")) {
                multiplier = (float) config.getDouble("sell-wands.wands." + name + ".multiplier");
            }

            int uses = -1;
            if (config.contains("sell-wands.wands." + name +  ".uses")) {
                uses = config.getInt("sell-wands.wands." + name + ".uses");
            }

            float maxProfit = -1;
            if (config.contains("sell-wands.wands." + name +  ".max-profit")) {
                maxProfit = (float) config.getDouble("sell-wands.wands." + name + ".max-profit");
            }

            boolean enchanted = false;
            if (config.contains("sell-wands.wands." + name +  ".enchanted")) {
                enchanted = config.getBoolean("sell-wands.wands." + name + ".enchanted");
            }

            int cooldown = 3;
            if (config.contains("sell-wands.wands." + name +  ".cooldown")) {
                cooldown = config.getInt("sell-wands.wands." + name + ".cooldown");
            }

            wands.add(new Wand(name,
                    Material.getMaterial(config.getString("sell-wands.wands." + name +  ".material").toUpperCase()),
                    config.getString("sell-wands.wands." + name +  ".display-name"),
                    config.getStringList("sell-wands.wands." + name +  ".lore"),
                    uses,
                    multiplier,
                    maxProfit,
                    cooldown,
                    enchanted
                    ));
        }

        return wands;
    }

    public int getPlaceholderPrecission() { return config.getInt("placeholders.decimal-precision"); }

    public boolean getDiscordEnabled() { return config.getBoolean("discord-bot.enabled"); }

    public LinkingMethod getLinkingMethod() { return LinkingMethod.valueOf(config.getString("discord-bot.link-method").toUpperCase()); }

    public String getToken() { return config.getString("discord-bot.token"); }

    public String getChannel() { return config.getString("discord-bot.channel"); }

    public String getAdminRoleID() { return config.getString("discord-bot.admin-role-id"); }

    public int getDefaultSlots() { return config.getInt("discord-bot.default-inventory"); }

    public float getSlotPriceFactor() { return (float) config.getDouble("discord-bot.slot-price-factor"); }

    public float getSlotPriceBase() { return (float) config.getDouble("discord-bot.slot-price-base"); }

    public float getDiscordBuyTax() {

        if (config.getBoolean("discord-bot.slot-price.taxation.override")) {
            return (float) (1 + config.getDouble("discord-bot.slot-price.taxation.buy"));
        } else {
            return 1 + (float) config.getDouble("market-control.taxation.buy");
        }
    }

    public float getDiscordSellTax() {

        if (config.getBoolean("discord-bot.slot-price.taxation.override")) {
            return (float) (1 - config.getDouble("discord-bot.slot-price.taxation.sell"));
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
            if (identifier.equalsIgnoreCase(item)){
                return (float) items.getDouble("items." + item + ".initial-price");
            }
        }
        return 1;
    }

    public HashMap<ItemStack, Float> getChilds(Item item) {

        HashMap<ItemStack, Float> childs = new LinkedHashMap<>();

        childs.put(item.getItemStack(), 1f);

        Set<String> section = null;

        if(items.getConfigurationSection("items." + item.getIdentifier() + ".child.") != null) {
            section = items.getConfigurationSection("items." + item.getIdentifier() + ".child.").getKeys(false);
        }

        if (section == null || section.size() == 0) return childs;

        for (String childMat : section) {
            childs.put(new ItemStack(Material.getMaterial(childMat.toUpperCase())), (float) items.getDouble("items." +item.getIdentifier() + ".child." + childMat + ".multiplier"));
        }

        return childs;
    }

    public ItemStack getItemStackOfItem(String identifier) {

        if (!items.contains("items." + identifier + ".item-stack")) return null;

        return items.getSerializable("items." + identifier + ".item-stack", ItemStack.class);

    }

    public String getAlias(String identifier) {
        if(!items.contains("items." + identifier + ".alias")) {
            return (Character.toUpperCase(identifier.charAt(0)) + identifier.substring(1)).replace("_", " ");
        } else {
            return items.getString("items." + identifier + ".alias");
        }
    }

    public float getSupport(String identifier) {
        if(items.contains("items." + identifier + ".support")) {
            return (float) items.getDouble("items." + identifier + ".support");
        }
        return 0;
    }

    public float getResistance(String identifier) {
        if(items.contains("items." + identifier + ".resistance")) {
            return (float) items.getDouble("items." + identifier + ".resistance");
        }
        return 0;
    }

    public float getElasticity(String identifier) {
        if(items.contains("items." + identifier + ".elasticity")) {
            return (float) items.getDouble("items." + identifier + ".elasticity");
        }
        return (float) config.getDouble("price-options.default-elasticity");
    }

    public float getNoiseIntensity(String identifier) {
        if(items.contains("items." + identifier + ".noise-intensity")) {
            return (float) items.getDouble("items." + identifier + ".noise-intensity");
        }
        return (float) config.getDouble("price-options.noise.default-intensity");
    }

    // Categories:

    public Set<String> getCategories() { return categories.getConfigurationSection("categories.").getKeys(false); }

    public String getDisplayName(Category category){
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

        for(Category category : MarketManager.getInstance().getCategories()) {
            if(categories.getList("categories." + category.getIdentifier() + ".items").contains(identifier)) {
                return category;
            }
        }
        return null;
    }

    public List<Fund> getFunds() {

        List<Fund> brokers = new ArrayList<>();

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

        return brokers;
    }


}

