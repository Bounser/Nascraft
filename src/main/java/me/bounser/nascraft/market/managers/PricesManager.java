package me.bounser.nascraft.market.managers;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.advancedgui.LayoutModifier;
import me.bounser.nascraft.market.managers.resources.Trend;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.notifications.NotificationsManager;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.Data;
import me.leoko.advancedgui.manager.GuiWallManager;
import me.leoko.advancedgui.utils.GuiWallInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PricesManager {

    private Trend trend;

    public static PricesManager instance;

    public static PricesManager getInstance() {
        return instance == null ? instance = new PricesManager() : instance;
    }

    private PricesManager(){
        trend = Trend.valueOf(Config.getInstance().getGeneralTrend());

        saveDataTask();
        shortTermPricesTask();
        dailyTask();
        NotificationsManager.getInstance().notifyTask();
    }

    public void setMarketStatus(Trend trend) {
        if (this.trend != trend) {
            this.trend = trend;
        }
    }

    private void shortTermPricesTask() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllItems()) {
                if (Config.getInstance().getRandomOscillation()) {
                    item.changePrice(getPercentage(item));
                }
                item.lowerOperations();

                item.addValueToM(item.getPrice());
            }

            if (GuiWallManager.getInstance().getActiveInstances() != null)
            for (GuiWallInstance instance : GuiWallManager.getInstance().getActiveInstances()) {

                if (instance.getLayout().getName().equals("Nascraft")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {

                        if (instance.getInteraction(player) != null) {
                            LayoutModifier.getInstance().updateMainPage(instance.getInteraction(player).getComponentTree(), true, player);
                        }
                    }
                }
            }
            GraphManager.getInstance().outdatedCollector();

        }, 20, 1200);
    }

    private void saveDataTask() {

        // All the prices will be stored each hour
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

        if (tendency == null) tendency = trend;

        if (tendency.equals(Trend.FLAT)) {
            return (float) (Math.random() * 2 - 1);
        }
        float[] ext = Trend.extents(trend);

        return (float) (Math.random() * (2*ext[0]) - (ext[1]));
    }

    public Trend getMarketStatus() {
        return trend;
    }

}
