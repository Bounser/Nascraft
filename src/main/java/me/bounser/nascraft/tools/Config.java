package me.bounser.nascraft.tools;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.Category;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Set;

public class Config {

    private final FileConfiguration config;
    private static Config instance;
    private static Nascraft main;

    float taxBuy = -1;
    float taxSell = -1;
    int random0 = 0;
    int precission = -1;
    int[] limit = {-1, -1};
    String currency = "0";

    public static Config getInstance() {
        return instance == null ? instance = new Config() : instance;
    }

    private Config() {
        main = Nascraft.getInstance();
        main.getConfig().options().copyDefaults();
        main.saveDefaultConfig();
        this.config = main.getConfig();
    }

    public Boolean getCheckResources() {
        return main.getConfig().getBoolean("AutoResourcesInjection");
    }

    public int getRequiredToMove(String category) {
        return main.getConfig().getInt("Items_quoted.Categories." + category + ".required_to_move");
    }

    public Set<String> getAllMaterials(String category) {
        return main.getConfig().getConfigurationSection("Items_quoted.Categories." + category + ".items.").getKeys(false);
    }

    public Set<String> getCategories() {
        return main.getConfig().getConfigurationSection("Items_quoted.Categories.").getKeys(false);
    }

    public float getInitialPrice(String mat) {

        FileConfiguration config = main.getConfig();

        for (String cat : getCategories()) {

            for (String item : config.getConfigurationSection("Items_quoted.Categories." + cat + ".items.").getKeys(false)) {
                if(mat.equals(item)){
                    return (float) config.getDouble("Items_quoted.Categories." + cat + ".items." + item + ".price");
                }
            }
        }
        return 1;
    }

    public String getCurrency() {
        if(currency.equals("0")) {
            return currency = config.getString("Lang.currency");
        }
        return currency;
    }

    public int getDecimalPrecission() {
        if(precission == -1) {
            precission = config.getInt("Market_control.decimal_limit");
        }
        return precission;
    }


    public boolean getRandomOscillation() {

        if (random0 == 0) {
            if (main.getConfig().getBoolean("Price_options.random_oscillation.enabled")) {
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
            return taxBuy = (float) main.getConfig().getDouble("Market_control.taxation.buy");
        }
    }

    public float getTaxSell() {
        if (taxSell != -1) {
            return taxSell;
        } else {
            return taxSell = (float) main.getConfig().getDouble("Market_control.taxation.sell");
        }
    }
    
    public String getCategory(String mat) {

        for(String cat : getCategories()) {

            if(main.getConfig().getConfigurationSection("Items_quoted.Categories." + cat + ".items").getKeys(false).contains(mat)) {
                return cat;
            }

        }
        return null;
    }

    public HashMap<String, Float> getChilds(String mat, String cat) {

        if(!config.contains("Items_quoted.Categories." + cat + ".items." + mat + ".child")){
            return null;
        }

        HashMap<String, Float> childs = new HashMap<>();

        childs.put(mat, 1f);

        Set<String> section = config.getConfigurationSection("Items_quoted.Categories." + cat + ".items." + mat + ".child.").getKeys(false);

        if(section.size() == 0) return null;
        for(String childMat : section){

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

    public int[] getLimits() {
        if((limit[0] == -1) && (limit[1] == -1)) {

            limit[0] = config.getInt("Market_control.limits.low");
            limit[10] = config.getInt("Market_control.limits.high");

            return limit;
        }
        return limit;
    }

}

