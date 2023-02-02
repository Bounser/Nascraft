package me.bounser.nascraft.tools;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.Item;
import me.bounser.nascraft.market.MarketManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Config {

    private final FileConfiguration config;
    private static Config instance;
    private static Nascraft main;

    float taxBuy = -1;
    float taxSell = -1;
    int random0 = 0;
    int precission = -1;

    public static Config getInstance(){
        return instance == null ? instance = new Config() : instance;
    }

    private Config() {
        main = Nascraft.getInstance();
        main.getConfig().options().copyDefaults();
        main.saveDefaultConfig();
        this.config = main.getConfig();
    }

    public Boolean getCheckResources(){ return main.getConfig().getBoolean("AutoResourcesInjection"); }

    public int getRequiredToMove(String category){ return main.getConfig().getInt("Items_quoted.Categories." + category + ".required_to_move"); }

    public Set<String> getAllMaterials(String category) {
        return main.getConfig().getConfigurationSection("Items_quoted.Categories." + category + ".items.").getKeys(false);
    }

    public Set<String> getCategories() { return main.getConfig().getConfigurationSection("Items_quoted.Categories.").getKeys(false); }

    public float getInitialPrice(String mat) {

        FileConfiguration config = main.getConfig();

        for(String cat : getCategories()) {

            for(String item : config.getConfigurationSection("Items_quoted.Categories." + cat + ".").getKeys(false)) {

                if(item.equals(mat)) return (float) config.getDouble("Items_quoted.Categories." + cat + ".items." + item + ".price");
            }
        }
        return 1;
    }

    public int getDecimalPrecission() {
        if(precission == -1) {
            precission = config.getInt("Market_control.decimal_limit");
        }
        return precission;
    }


    public boolean getRandomOscilation() {

        if (random0 == 0) {
            if (main.getConfig().getBoolean("Price_options.random_oscilation")) {
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

            if(main.getConfig().getConfigurationSection("Items_Quoted.Categories." + cat + ".items").getKeys(false).contains(mat)) {
                return cat;
            }

        }
        return null;
    }

    public HashMap<String, Float> getChilds(String mat) {

        HashMap<String, Float> childs = new HashMap<>();
        Item item = MarketManager.getInstance().getItem(mat);

        if(!config.contains("Items_Quoted.Categories." + item.getCategory() + ".items." + item.getMaterial() + ".child.")){
            return null;
        }
        Set<String> section = config.getConfigurationSection("Items_Quoted.Categories." + item.getCategory() + ".items." + item.getMaterial() + ".child.").getKeys(false);

        if(section.size() == 0) return null;
        for(String childMat : section){

            childs.put(childMat, (float) config.getDouble("Items_Quoted.Categories." + item.getCategory() + ".items." + item.getMaterial() + ".child." + mat + ".price"));

        }
        return childs;
    }

}

