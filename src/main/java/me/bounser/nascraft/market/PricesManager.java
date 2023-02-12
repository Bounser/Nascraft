package me.bounser.nascraft.market;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.Data;
import org.bukkit.Bukkit;

public class PricesManager {

    MarketStatus marketStatus;

    public static PricesManager instance;

    public static PricesManager getInstance() {
        return instance == null ? instance = new PricesManager() : instance;
    }

    private PricesManager(){
        marketStatus = MarketStatus.FLAT;

        saveDataTask();
        shortTermPricesTask();
    }

    public void setMarketStatus(MarketStatus marketStatus) {
        if(this.marketStatus != marketStatus) {
            this.marketStatus = marketStatus;
        }
    }

    private void shortTermPricesTask() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllItems()) {
                if(Config.getInstance().getRandomOscilation()){

                    item.changePrice(getPercentage());
                }
                item.addValueToM(item.getPrice());
            }

        }, 60, 1200);

    }

    public void setupFiles() {
        Data.getInstance().setupFiles();
    }

    private void saveDataTask() {

        // All the prices will be stored 2 times each hour.
        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllItems()) item.addValueToH(item.getPrice());
            Data.getInstance().savePrices();

        }, 30000, 36000);

    }

    public float getPercentage() {

        float percentage;

        switch (marketStatus) {

            case BULL1:
                percentage = 1f;
                break;
            case BULL2:
                percentage = 2f;
                break;
            case BULL3:
                percentage = 5f;
                break;
            case BULLRUN:
                percentage = 15f;
                break;

            case BEAR1:
                percentage = -1f;
                break;
            case BEAR2:
                percentage = -2f;
                break;
            case BEAR3:
                percentage = -5f;
                break;
            case CRASH:
                percentage = -15f;
                break;

            default:
                percentage = 0;

        }
        return (float) (Math.random() * percentage - percentage);
    }


}
