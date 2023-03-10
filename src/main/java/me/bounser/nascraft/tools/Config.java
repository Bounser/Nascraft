package me.bounser.nascraft.tools;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.managers.resources.Category;
import me.bounser.nascraft.market.managers.MarketManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class Config {

    private final FileConfiguration config;
    private static Config instance;

    private float taxBuy = -1;
    private float taxSell = -1;
    private int random0 = 0;
    private int force = -1;
    private int precission = -1;
    private final float[] limit = {-1, -1};
    private String currency = "0";
    private String buyMsg = "0";
    private String sellMsg = "0";

    public static Config getInstance() {
        return instance == null ? instance = new Config() : instance;
    }

    private Config() {
        Nascraft.getInstance().saveDefaultConfig();
        this.config = Nascraft.getInstance().getConfig();
    }

    public Boolean getCheckResources() {
        return config.getBoolean("AutoResourcesInjection");
    }

    public Set<String> getAllMaterials(String category) {
        return config.getConfigurationSection("Items_quoted.Categories." + category + ".items.").getKeys(false);
    }

    public Set<String> getCategories() {
        return config.getConfigurationSection("Items_quoted.Categories.").getKeys(false);
    }

    public float getInitialPrice(String mat) {

        for (Category cat : MarketManager.getInstance().getCategories()) {

            for (String item : config.getConfigurationSection("Items_quoted.Categories." + cat.getName() + ".items.").getKeys(false)) {
                if (mat.equals(item)){
                    return (float) config.getDouble("Items_quoted.Categories." + cat.getName() + ".items." + item + ".price");
                }
            }
        }
        return 1;
    }

    public String getCurrency() {
        if (currency.equals("0")) {
            return currency = config.getString("Lang.currency");
        }
        return currency;
    }

    public int getDecimalPrecission() {
        if (precission == -1) {
            precission = config.getInt("Price_options.decimal_limit");
        }
        return precission;
    }

    public boolean getRandomOscillation() {

        if (random0 == 0) {
            if (config.getBoolean("Price_options.random_oscillation.enabled")) {
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
            return taxBuy = (float) config.getDouble("Market_control.taxation.buy");
        }
    }

    public float getTaxSell() {
        if (taxSell != -1) {
            return taxSell;
        } else {
            return taxSell = (float) config.getDouble("Market_control.taxation.sell");
        }
    }

    public HashMap<String, Float> getChilds(String mat, String cat) {

        if (!config.contains("Items_quoted.Categories." + cat + ".items." + mat + ".child")){
            return null;
        }

        HashMap<String, Float> childs = new HashMap<>();

        childs.put(mat, 1f);

        Set<String> section = config.getConfigurationSection("Items_quoted.Categories." + cat + ".items." + mat + ".child.").getKeys(false);

        if (section.size() == 0) return null;
        for (String childMat : section){
            childs.put(childMat, (float) config.getDouble("Items_quoted.Categories." + cat + ".items." + mat + ".child." + childMat + ".multiplier"));
        }
        return childs;
    }

    public String getDisplayName(Category cat ){
        return config.getString("Items_quoted.Categories." + cat.getName() + ".name");
    }

    public String getGeneralTrend(){
        return config.getString("Price_options.random_oscillation.market_trend");
    }

    public String getItemDefaultTrend(String category, String material){
        String trend = config.getString("Items_quoted.Categories." + category + ".items." + material + ".trend");
        return trend == null ? "FLAT" : trend;
    }

    public float[] getLimits() {
        if ((limit[0] == -1) && (limit[1] == -1)) {
            limit[0] = (float) config.getDouble("Price_options.limits.low");
            limit[1] = (float) config.getDouble("Price_options.limits.high");
            return limit;
        }
        return limit;
    }

    public boolean isForceAllowed() {
        if (force == -1)
            if (config.getBoolean("Market_control.force_command")) {
                force = 1;
                return true;
            } else {
                force = 0;
                return false;
            }
        return force != 0;
    }

    public List<String> getLang() {

        List<String> msg = new ArrayList<>();
        msg.add(config.getString("Lang.title"));
        msg.add(config.getString("Lang.topmovers"));
        msg.add(config.getString("Lang.subtop"));
        msg.add(config.getString("Lang.buy"));
        msg.add(config.getString("Lang.sell"));
        msg.add(config.getString("Lang.price"));
        msg.add(config.getString("Lang.amount_selection"));
        msg.add(config.getString("trend"));

        return msg;
    }

    public String getBuyMessage() {
        if (buyMsg.equals( "0")) {
            return buyMsg = config.getString("Lang.buy_message");
        } else {
            return buyMsg;
        }
    }

    public String getSellMessage() {
        if (sellMsg.equals( "0")) {
            return sellMsg = config.getString("Lang.sell_message");
        } else {
            return sellMsg;
        }
    }

    public boolean getMarketPermissionRequirement() {
        return config.getBoolean("Market_control.market_permission");
    }

    public String getAlias(String mat, String category) {
        if(!config.contains("Items_quoted.Categories." + category + "." + mat + ".alias")) {
            return (Character.toUpperCase(mat.charAt(0)) + mat.substring(1)).replace("_", " ");
        }
        return config.getString("Items_quoted.Categories." + category + "." + mat + ".alias");
    }

}

