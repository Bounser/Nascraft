package me.bounser.nascraft.market;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.Data;
import org.bukkit.Bukkit;

public class PricesManager {

    Trend trend;

    public static PricesManager instance;

    public static PricesManager getInstance() {
        return instance == null ? instance = new PricesManager() : instance;
    }

    private PricesManager(){
        trend = Trend.valueOf(Config.getInstance().getGeneralTrend());

        saveDataTask();
        shortTermPricesTask();
        dailyTask();
    }

    public void setMarketStatus(Trend trend) {
        if(this.trend != trend) {
            this.trend = trend;
        }
    }

    private void shortTermPricesTask() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllItems()) {
                if(Config.getInstance().getRandomOscillation()){
                    item.changePrice(getPercentage(item));
                }
                item.lowerOperations();

                item.addValueToM(item.getPrice());
            }

        }, 20, 1200);

    }

    public void setupFiles() {
        Data.getInstance().setupFiles();
    }

    private void saveDataTask() {

        // All the prices will be stored 2 times each hour.
        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllItems()) item.addValueToH(item.getPrice());
            Data.getInstance().savePrices();

        }, 30000, 72000);

    }

    private void dailyTask() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllItems()) item.dailyUpdate();

        }, 1728000, 1728000);
    }


    public float getPercentage(Item item) {

        Trend tendency = item.getTrend();

        if(tendency == null) tendency = trend;

        if(tendency.equals(Trend.FLAT)) {
            return (float) (Math.random() * 2 - 1);
        }

        float positive, negative;

        switch (tendency) {

            case BULL1:
                positive = 1.1f;
                negative = 1f;
                break;
            case BULL2:
                positive = 1.2f;
                negative = 1f;
                break;
            case BULL3:
                positive = 1.3f;
                negative = 1f;
                break;
            case BULLRUN:
                positive = 3f;
                negative = 1f;
                break;

            case BEAR1:
                positive = 1f;
                negative = 1.1f;
                break;
            case BEAR2:
                positive = 1f;
                negative = 1.2f;
                break;
            case BEAR3:
                positive = 1f;
                negative = 1.3f;
                break;
            case CRASH:
                positive = 1f;
                negative = 3f;
                break;

            default:
                positive = 1f;
                negative = 1f;

        }
        return (float) (Math.random() * (2*positive) - (negative));
    }

    public Trend getMarketStatus() {
        return trend;
    }

}
