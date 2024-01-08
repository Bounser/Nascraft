package me.bounser.nascraft.managers;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.advancedgui.LayoutModifier;
import me.bounser.nascraft.database.SQLite;
import me.bounser.nascraft.database.playerinfo.PlayerInfoManager;
import me.bounser.nascraft.discord.DiscordAlerts;
import me.bounser.nascraft.discord.DiscordBot;
import me.bounser.nascraft.market.unit.Instant;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.config.Config;
import me.leoko.advancedgui.manager.GuiWallManager;
import me.leoko.advancedgui.utils.GuiWallInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TasksManager {

    public static TasksManager instance;

    private int ticksPerSecond = 20;

    private Plugin AGUI = Bukkit.getPluginManager().getPlugin("AdvancedGUI");

    public static TasksManager getInstance() { return instance == null ? instance = new TasksManager() : instance; }

    private TasksManager(){
        saveDataTask();
        shortTermPricesTask();
        hourlyTask();
        dailyTask();
        saveInstants();
    }

    private void shortTermPricesTask() {

        LocalTime timeNow = LocalTime.now();

        LocalTime nextMinute = timeNow.plusMinutes(1).withSecond(0);
        Duration timeRemaining = Duration.between(timeNow, nextMinute);

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            float allChanges = 0;
            for (Item item : MarketManager.getInstance().getAllItems()) {
                if (Config.getInstance().getPriceNoise())
                    allChanges += item.getPrice().applyNoise();

                item.lowerOperations();

                item.addValueToHour(item.getPrice().getValue());
            }

            MarketManager.getInstance().updateMarketChange1h(allChanges/MarketManager.getInstance().getAllItems().size());


            if (AGUI != null && AGUI.isEnabled() &&
                    GuiWallManager.getInstance().getActiveInstances() != null)

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

            BrokersManager.getInstance().operateBrokers();

            if (Config.getInstance().getDiscordEnabled()) {
                DiscordBot.getInstance().update();
                DiscordAlerts.getInstance().updateAlerts();
            }

        }, timeRemaining.getSeconds()*20, 60 * ticksPerSecond);
    }

    private void saveDataTask() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            SQLite.getInstance().saveEverything();

        }, 2400, 60 * 5 * ticksPerSecond); // 5 min
    }

    private void saveInstants() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllItems()) {

                item.getItemStats().addInstant(new Instant(
                        LocalDateTime.now(),
                        item.getPrice().getValue(),
                        item.getVolume()
                ));

                item.restartVolume();

            }

        }, 2400, 60 * ticksPerSecond);
    }

    private void hourlyTask() {
        LocalTime timeNow = LocalTime.now();

        LocalTime nextHour = timeNow.plusHours(1).withMinute(0).withSecond(0);
        Duration timeRemaining = Duration.between(timeNow, nextHour);

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllItems()) {
                item.addValueToDay(item.getPrice().getValue());
                item.getPrice().restartHourLimits();
            }

            MarketManager.getInstance().setOperationsLastHour(0);

        }, timeRemaining.getSeconds()*20, 72000); // 1 hour
    }

    private void dailyTask() {

        LocalTime timeNow = LocalTime.now();

        LocalTime midnight = LocalTime.MIDNIGHT;
        Duration timeRemaining = Duration.between(timeNow, midnight);

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllItems()) item.dailyUpdate();

        }, timeRemaining.getSeconds()*20, 1728000);
    }

    public void save() {

        for (Item item : MarketManager.getInstance().getAllItems())
            SQLite.getInstance().saveItem(item);

        PlayerInfoManager.getInstance().saveEverything();
    }

}
