package me.bounser.nascraft.config;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.brokers.BrokerType;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.managers.MarketManager;
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

    public Boolean getCheckResources() { return config.getBoolean("auto_resources_injection"); }

    public int getDatabasePurgeDays() { return config.getInt("database.days-until-history-removed"); }

    public String getSelectedLanguage() { return config.getString("language"); }

    public boolean getPriceNoise() {
        return config.getBoolean("price_options.noise.enabled");
    }

    public float getTaxBuy() {
        return 1 + (float) config.getDouble("market_control.taxation.buy");
    }

    public float getTaxSell() {
        return 1 - (float) config.getDouble("market_control.taxation.sell");
    }

    public float[] getLimits() {
        float[] limit = new float[2];
            limit[0] = (float) config.getDouble("price_options.limits.low");
            limit[1] = (float) config.getDouble("price_options.limits.high");
            return limit;
    }

    public boolean getMarketPermissionRequirement() { return config.getBoolean("market_control.market_permission"); }

    public List<String> getCommands() { return config.getStringList("commands.enabled"); }

    public boolean getDiscordEnabled() { return config.getBoolean("discord_bot.enabled"); }

    public String getToken() { return config.getString("discord_bot.token"); }

    public String getChannel() { return config.getString("discord_bot.channel"); }

    public int getDefaultSlots() { return config.getInt("discord_bot.default_inventory"); }

    public float getSlotPriceFactor() { return (float) config.getDouble("discord_bot.slot_price_factor"); }

    public float getSlotPriceBase() { return (float) config.getDouble("discord_bot.slot_price_base"); }


    // Items:

    public Set<String> getAllMaterials() {
        return items.getConfigurationSection("items.").getKeys(false);
    }

    public float getInitialPrice(Material material) {
        for (String item : getAllMaterials()) {
            if (material.toString().equalsIgnoreCase(item)){
                return (float) items.getDouble("items." + item + ".initial_price");
            }
        }
        return 1;
    }

    public HashMap<Material, Float> getChilds(Material material) {

        HashMap<Material, Float> childs = new HashMap<>();

        childs.put(material, 1f);

        Set<String> section = null;

        if(items.getConfigurationSection("items." + material + ".child.") != null) {
            section = items.getConfigurationSection("items." + material + ".child.").getKeys(false);
        }

        if (section == null ||section.size() == 0) return childs;

        for (String childMat : section){
            childs.put(Material.getMaterial(childMat), (float) items.getDouble("items." + material + ".child." + childMat + ".multiplier"));
        }
        return childs;
    }

    public String getAlias(Material material) {
        if(!items.contains("items." + material + ".alias")) {
            return (Character.toUpperCase(material.toString().toLowerCase().charAt(0)) + material.toString().toLowerCase().substring(1)).replace("_", " ");
        } else {
            return items.getString("items." + material + ".alias");
        }
    }

    public float getSupport(Material material) {
        if(items.contains("items." + material + ".support")) {
            return (float) items.getDouble("items." + material.toString().toLowerCase() + ".support");
        }
        return 0;
    }

    public float getResistance(Material material) {
        if(items.contains("items." + material + ".resistance")) {
            return (float) items.getDouble("items." + material.toString().toLowerCase() + ".resistance");
        }
        return 0;
    }

    public float getElasticity(Material material) {
        if(items.contains("items." + material + ".elasticity")) {
            return (float) items.getDouble("items." + material.toString().toLowerCase() + ".elasticity");
        }
        return 1;
    }

    public float getNoiseIntensity(Material material) {
        if(items.contains("items." + material + ".noise_intensity")) {
            return (float) items.getDouble("items." + material.toString().toLowerCase() + ".noise_intensity");
        }
        return 1;
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

