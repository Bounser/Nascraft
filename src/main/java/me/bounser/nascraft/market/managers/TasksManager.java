package me.bounser.nascraft.market.managers;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.advancedgui.LayoutModifier;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.tools.Config;
import me.bounser.nascraft.tools.Data;
import me.leoko.advancedgui.manager.GuiWallManager;
import me.leoko.advancedgui.utils.GuiWallInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TasksManager {

    public static TasksManager instance;

    public static TasksManager getInstance() {
        return instance == null ? instance = new TasksManager() : instance;
    }

    private TasksManager(){

        saveDataTask();
        shortTermPricesTask();
        dailyTask();
    }

    private void shortTermPricesTask() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllItems()) {
                if (Config.getInstance().getRandomOscillation()) {
                    item.getPrice().applyNoise();
                }
                item.lowerOperations();

                item.addValueToM(item.getPrice().getValue());
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

            for (Item item : MarketManager.getInstance().getAllItems()) item.addValueToH(item.getPrice().getValue());
            Data.getInstance().savePrices();

        }, 30000, 72000);
    }

    private void dailyTask() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllItems()) item.dailyUpdate();

        }, 1728000, 1728000);
    }

}
