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

        saveData();

        if(Config.getInstance().getRandomOscilation()) { randomOscilations(); }

    }

    public void setMarketStatus(MarketStatus marketStatus) {
        if(this.marketStatus != marketStatus) {
            this.marketStatus = marketStatus;
            marketEvent();
        }

    }

    public void saveData() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            Data.getInstance().savePrices();

        }, 50, 12000);

    }

    public void changeAllMarket(float percentage){

        for(Item item : ItemsManager.getInstance().getAllItems()) {

            item.changePrice(percentage + (float) (Math.random() * percentage*0.5));

        }

    }

    public void marketEvent() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            switch (marketStatus) {

                case BULLRUN:
                    changeAllMarket((float) 0.1);
                    break;
                case BULL1:
                    changeAllMarket((float) 0.01);
                    break;
                case BULL2:
                    changeAllMarket((float) 0.03);
                    break;
                case BULL3:
                    changeAllMarket((float) 0.08);
                    break;

            }

        }, 50, 12000);

    }

    public void randomOscilations() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for(Item item : ItemsManager.getInstance().getAllItems()) {
                item.changePrice((float) (Math.random() * 2 -1));
            }

        }, 50, 12000);

    }

}
