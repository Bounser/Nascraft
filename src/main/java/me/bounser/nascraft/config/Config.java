package me.bounser.nascraft.config;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.sellwand.Wand;
import me.bounser.nascraft.discord.linking.LinkingMethod;
import me.bounser.nascraft.market.brokers.BrokerType;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class Config {

    private final FileConfiguration config;
    private FileConfiguration items;
    private FileConfiguration categories;
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
        // inventorygui = setupFile("inventorygui.yml");
    }

    public YamlConfiguration setupFile(String name) {

        File file = new File(main.getDataFolder(), name);

        if (!file.exists()) main.saveResource(name, false);

        return YamlConfiguration.loadConfiguration(file);
    }

    // Config:

    public Boolean getCheckResources() { return config.getBoolean("auto-resources-injection"); }

    public int getDatabasePurgeDays() { return config.getInt("database.days-until-history-removed"); }

    public String getSelectedLanguage() { return config.getString("language"); }

    public float[] getLimits() {
        float[] limit = new float[2];
        limit[0] = (float) config.getDouble("price-options.limits.low");
        limit[1] = (float) config.getDouble("price-options.limits.high");
        return limit;
    }

    public boolean getPriceNoise() { return config.getBoolean("price-options.noise.enabled"); }

    public float getTaxBuy(Item item) {

        if (items.contains("items." + item.getMaterial().toString().toLowerCase() + ".tax.buy")) {
            return 1 + (float) items.getDouble("items." + item.getMaterial().toString().toLowerCase() + ".tax.buy");
        } else {
            return 1 + (float) config.getDouble("market-control.taxation.buy");
        }
    }

    public float getTaxSell(Item item) {

        if (items.contains("items." + item.getMaterial().toString().toLowerCase() + ".tax.sell")) {
            return 1 - (float) items.getDouble("items." + item.getMaterial().toString().toLowerCase() + ".tax.sell");
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

    public float getInitialPrice(Material material) {
        for (String item : getAllMaterials()) {
            if (material.toString().equalsIgnoreCase(item)){
                return (float) items.getDouble("items." + item + ".initial-price");
            }
        }
        return 1;
    }

    public HashMap<Material, Float> getChilds(Material material) {

        HashMap<Material, Float> childs = new HashMap<>();

        childs.put(material, 1f);

        Set<String> section = null;

        if(items.getConfigurationSection("items." + material.toString().toLowerCase() + ".child.") != null) {
            section = items.getConfigurationSection("items." + material.toString().toLowerCase() + ".child.").getKeys(false);
        }

        if (section == null || section.size() == 0) return childs;

        for (String childMat : section){
            childs.put(Material.getMaterial(childMat.toUpperCase()), (float) items.getDouble("items." + material.toString().toLowerCase() + ".child." + childMat + ".multiplier"));
        }
        return childs;
    }

    public String getAlias(Material material) {
        if(!items.contains("items." + material.toString().toLowerCase() + ".alias")) {
            return (Character.toUpperCase(material.toString().toLowerCase().charAt(0)) + material.toString().toLowerCase().substring(1)).replace("_", " ");
        } else {
            return items.getString("items." + material.toString().toLowerCase() + ".alias");
        }
    }

    public float getSupport(Material material) {
        if(items.contains("items." + material.toString().toLowerCase() + ".support")) {
            return (float) items.getDouble("items." + material.toString().toLowerCase() + ".support");
        }
        return 0;
    }

    public float getResistance(Material material) {
        if(items.contains("items." + material.toString().toLowerCase() + ".resistance")) {
            return (float) items.getDouble("items." + material.toString().toLowerCase() + ".resistance");
        }
        return 0;
    }

    public float getElasticity(Material material) {
        if(items.contains("items." + material.toString().toLowerCase() + ".elasticity")) {
            return (float) items.getDouble("items." + material.toString().toLowerCase() + ".elasticity");
        }
        return (float) config.getDouble("price-options.default-elasticity");
    }

    public float getNoiseIntensity(Material material) {
        if(items.contains("items." + material.toString().toLowerCase() + ".noise-intensity")) {
            return (float) items.getDouble("items." + material.toString().toLowerCase() + ".noise-intensity");
        }
        return (float) config.getDouble("price-options.noise.default-intensity");
    }

    // Categories:

    public Set<String> getCategories() {
        return categories.getConfigurationSection("categories.").getKeys(false);
    }

    public String getDisplayName(Category cat ){
        return categories.getString("categories." + cat.getName() + ".display_name");
    }

    public Category getCategoryFromMaterial(Material material) {

        for(Category category : MarketManager.getInstance().getCategories()) {
            if(categories.getList("categories." + category.getName() + ".items").contains(material.toString().toLowerCase())) {
                return category;
            }
        }
        return null;
    }

    public List<BrokerType> getBrokers() {

        List<BrokerType> brokers = new ArrayList<>();

        if (config.getBoolean("brokers.aggressive.enabled")) { brokers.add(BrokerType.AGGRESSIVE); }

        if (config.getBoolean("brokers.conservative.enabled")) { brokers.add(BrokerType.CONSERVATIVE); }

        if (config.getBoolean("brokers.lazy.enabled")) { brokers.add(BrokerType.LAZY); }

        return brokers;
    }

    public float getBrokerFee(BrokerType brokerType) {
        return (float) config.getDouble("brokers." + brokerType.toString().toLowerCase() + ".daily-fee");
    }

    public float getMarketSensibility(BrokerType brokerType) {
        return (float) config.getDouble("brokers." + brokerType.toString().toLowerCase() + ".market-sensibility");
    }

    public float getVolatility(BrokerType brokerType) {
        return (float) config.getDouble("brokers." + brokerType.toString().toLowerCase() + ".volatility");
    }

    public float getPositiveReturn(BrokerType brokerType) {
        return (float) config.getDouble("brokers." + brokerType.toString().toLowerCase() + ".positive-return");
    }

}

