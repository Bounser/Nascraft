package me.bounser.nascraft.config;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.managers.MarketManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class Config {

    private final FileConfiguration config;
    private FileConfiguration items;
    private FileConfiguration categories;
    private FileConfiguration lang;
    private FileConfiguration inventorygui;
    private static Config instance;
    private Nascraft main;


    private List<String> langList = new ArrayList<>();
    private float taxBuy = -1;
    private float taxSell = -1;
    private int random0 = 0;
    private int force = -1;
    private int precision = -1;
    private final float[] limit = {-1, -1};

    public static Config getInstance() {
        return instance == null ? instance = new Config() : instance;
    }

    private Config() {
        main = Nascraft.getInstance();
        main.saveDefaultConfig();
        this.config = Nascraft.getInstance().getConfig();

        items = setupFile("items.yml");
        categories = setupFile("categories.yml");
        lang = setupFile("lang.yml");
        // inventorygui = setupFile("inventorygui.yml");

        setupLang();
    }

    public YamlConfiguration setupFile(String name) {

        File file = new File(main.getDataFolder(), name);

        if (!file.exists()) main.saveResource(name, false);

        return YamlConfiguration.loadConfiguration(file);
    }

    public void setupLang() {

        langList.add(lang.getString("lang.currency"));
        // Layout
        langList.add(lang.getString("lang.title"));
        langList.add(lang.getString("lang.topmovers"));
        langList.add(lang.getString("lang.subtop"));
        langList.add(lang.getString("lang.buy_message").replace("&","§"));
        langList.add(lang.getString("lang.sell_message").replace("&","§"));
        langList.add(lang.getString("lang.buy"));
        langList.add(lang.getString("lang.sell"));
        langList.add(lang.getString("lang.price"));
        langList.add(lang.getString("lang.amount_selection"));
        langList.add(lang.getString("lang.trend"));
        // Commands
        langList.add(lang.getString("lang.permission_text").replace("&","§"));
        langList.add(lang.getString("lang.sellall_everything").replace("&","§"));
        langList.add(lang.getString("lang.sellall_everything_error").replace("&","§"));
        langList.add(lang.getString("lang.sellall").replace("&","§"));
        langList.add(lang.getString("lang.sellall_error_without_item").replace("&","§"));
        langList.add(lang.getString("lang.sellall_error_wrong_material").replace("&","§"));
        langList.add(lang.getString("lang.estimated_value").replace("&","§"));
        langList.add(lang.getString("lang.click_to_confirm").replace("&","§"));

        langList.add(lang.getString("lang.sell_title").replace("&","§"));
        langList.add(lang.getString("lang.sell_close").replace("&","§"));
        langList.add(lang.getString("lang.sell_button_name").replace("&","§"));
        langList.add(lang.getString("lang.sell_button_lore").replace("&","§"));
        langList.add(lang.getString("lang.sell_remove_item").replace("&","§"));
        langList.add(lang.getString("lang.sell_action_message").replace("&","§"));
        langList.add(lang.getString("lang.sell_item_not_allowed").replace("&","§"));
        langList.add(lang.getString("lang.sell_full").replace("&","§"));

        langList.add(lang.getString("lang.sellhand_invalid").replace("&","§"));
        langList.add(lang.getString("lang.sellhand_error_hand").replace("&","§"));
        langList.add(lang.getString("lang.sell_estimated_value").replace("&","§"));
    }

    // Config:

    public Boolean getCheckResources() {
        return config.getBoolean("auto_resources_injection");
    }

    public boolean getPriceNoise() {

        if (random0 == 0) {
            if (config.getBoolean("price_options.noise.enabled")) {
                random0 = 1;
            } else {
                random0 = 0;
            }
        }
        return random0 == 1;
    }

    public float getTaxBuy() {
        if (taxBuy != -1) {
            return taxBuy;
        } else {
            return taxBuy = 1 + (float) config.getDouble("market_control.taxation.buy");
        }
    }

    public float getTaxSell() {
        if (taxSell != -1) {
            return taxSell;
        } else {
            return taxSell = 1 - (float) config.getDouble("market_control.taxation.sell");
        }
    }

    public float[] getLimits() {
        if ((limit[0] == -1) && (limit[1] == -1)) {
            limit[0] = (float) config.getDouble("price_options.limits.low");
            limit[1] = (float) config.getDouble("price_options.limits.high");
            return limit;
        }
        return limit;
    }

    public boolean getMarketPermissionRequirement() { return config.getBoolean("market_control.market_permission"); }

    public List<String> getCommands() {
        return config.getStringList("commands.enabled");
    }

    // Items:

    public Set<String> getAllMaterials() {
        return items.getConfigurationSection("items.").getKeys(false);
    }

    public float getInitialPrice(String mat) {
        for (String item : getAllMaterials()) {
            if (mat.equalsIgnoreCase(item)){
                return (float) items.getDouble("items." + item + ".initial_price");
            }
        }
        return 1;
    }

    public HashMap<String, Float> getChilds(String mat, String cat) {

        if (!items.contains("items." + mat + ".child")){
            return null;
        }

        HashMap<String, Float> childs = new HashMap<>();

        childs.put(mat, 1f);

        Set<String> section = items.getConfigurationSection("items." + mat + ".child.").getKeys(false);

        if (section.size() == 0) return null;
        for (String childMat : section){
            childs.put(childMat, (float) items.getDouble("items." + mat + ".child." + childMat + ".multiplier"));
        }
        return childs;
    }

    public String getAlias(String material) {
        if(!items.contains("items." + material + ".alias")) {
            return (Character.toUpperCase(material.charAt(0)) + material.substring(1)).replace("_", " ");
        } else {
            return items.getString("items." + material + ".alias");
        }
    }

    public float getSupport(String material) {
        if(items.contains("items." + material + ".support")) {
            return (float) items.getDouble("items." + material + ".support");
        }
        return 0;
    }

    public float getResistance(String material) {
        if(items.contains("items." + material + ".resistance")) {
            return (float) items.getDouble("items." + material + ".resistance");
        }
        return 0;
    }

    public float getElasticity(String material) {
        if(items.contains("items." + material + ".elasticity")) {
            return (float) items.getDouble("items." + material + ".elasticity");
        }
        return 1;
    }

    public float getNoiseIntensity(String material) {
        if(items.contains("items." + material + ".noise_intensity")) {
            return (float) items.getDouble("items." + material + ".noise_intensity");
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

    public Category getCategoryFromMaterial(String material) {

        for(Category category : MarketManager.getInstance().getCategories()) {
            if(categories.getList("categories." + category.getName() + ".items").contains(material)) {
                return category;
            }
        }
        return null;
    }

    // public String mode() { return config.getString("Data_storage.method"); }

    public String mode() { return "JSON"; }

    /*
    public String address() {
        return config.getString("Data_storage.address");
    }
    public int port() {
        return config.getInt("Data_storage.port");
    }
    public String database() { return config.getString("Data_storage.database");}
    public String user() {
        return config.getString("Data_storage.user");
    }
    public String password() {
        return config.getString("Data_storage.password");
    }
     */

    // Lang:

    public String getCurrency() {
        return langList.get(0);
    }

    public String getTitle() {
        return langList.get(1);
    }

    public String getTopMoversText() {
        return langList.get(2);
    }

    public String getSubTopMoversText() {
        return langList.get(3);
    }

    public String getBuyMessage(String amount, String worth, String material) {
        return langList.get(4).replace("[AMOUNT]", amount).replace("[WORTH]", worth).replace("[MATERIAL]", material).replace("[CURRENCY]", getCurrency());
    }

    public String getSellMessage(String amount, String worth, String material) {
        return langList.get(5).replace("[AMOUNT]", amount).replace("[WORTH]", worth).replace("[MATERIAL]", material).replace("[CURRENCY]", getCurrency());
    }

    public String getBuyText() {
        return langList.get(6);
    }

    public String getSellText() {
        return langList.get(7);
    }

    public String getPriceText() {
        return langList.get(8);
    }

    public String getAmountSelectionText() {
        return langList.get(9);
    }

    public String getTrendText() {
        return langList.get(10);
    }

    public String getPermissionText() {
        return langList.get(11);
    }

    public String getSellallEverythingText(String materials, String worth) {
        return langList.get(12).replace("[NUM_MATERIALS]", materials).replace("[WORTH]", worth);
    }

    public String getSellallEverythingErrorText() {
        return langList.get(13);
    }

    public String getSellallText(String amount, String material, String worth) {
        return langList.get(14).replace("[AMOUNT]", amount).replace("[MATERIAL]", material).replace("[WORTH]", worth);
    }

    public String getSellallErrorWithoutItemText(String material) {
        return langList.get(15).replace("[MATERIAL]", material);
    }

    public String getSellallErrorWrongMaterialText() {return langList.get(16); }

    public String getSellallEverythingEstimatedText(String totalWorth) {
        return langList.get(17).replace("[TOTAL]", totalWorth).replace("[CURRENCY]", getCurrency());
    }

    public String getClickToConfirmText() { return langList.get(18); }

    public String getSellTitle() { return langList.get(19); }

    public String getSellCloseText() { return langList.get(20); }

    public String getSellButtonName() { return langList.get(21); }

    public String getSellButtonLore(String worth) { return langList.get(22).replace("[WORTH]", worth).replace("[CURRENCY]", getCurrency()); }

    public String getSellRemoveItemText() { return langList.get(23); }

    public String getSellActionText(String worth) { return langList.get(24).replace("[WORTH]", worth).replace("[CURRENCY", getCurrency()); }

    public String getSellItemNotAllowedText() { return langList.get(25); }

    public String getSellFullText() { return langList.get(26); }

    public String getSellHandInvalidItem() { return langList.get(27); }

    public String getSellHandErrorText() { return langList.get(28); }

    public String getSellHandEstimatedValue() { return langList.get(29); }

}

