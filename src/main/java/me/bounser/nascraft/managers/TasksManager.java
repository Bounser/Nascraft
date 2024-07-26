package me.bounser.nascraft.managers;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.advancedgui.LayoutModifier;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.sqlite.SQLite;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.discord.DiscordBot;
import me.bounser.nascraft.discord.DiscordLog;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.stats.Instant;
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

    private final int ticksPerSecond = 20;

    private final Plugin AGUI = Bukkit.getPluginManager().getPlugin("AdvancedGUI");

    public static TasksManager getInstance() { return instance == null ? instance = new TasksManager() : instance; }

    private TasksManager(){
        saveDataTask();
        shortTermPricesTask();
        hourlyTask();
        saveInstants();

        DatabaseManager.get().getDatabase().purgeHistory();
    }

    private void shortTermPricesTask() {

        LocalTime timeNow = LocalTime.now();

        LocalTime nextMinute = timeNow.plusMinutes(1).withSecond(0);
        Duration timeRemaining = Duration.between(timeNow, nextMinute);

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            float allChanges = 0;
            for (Item item : MarketManager.getInstance().getAllParentItems()) {
                if (Config.getInstance().getPriceNoise())
                    allChanges += item.getPrice().applyNoise();

                item.lowerOperations();

                item.getPrice().addValueToShortTermStorage();
            }

            MarketManager.getInstance().updateMarketChange1h(allChanges/MarketManager.getInstance().getAllParentItems().size());

            if (AGUI != null &&
                AGUI.isEnabled() &&
                GuiWallManager.getInstance().getActiveInstances() != null)

                for (GuiWallInstance instance : GuiWallManager.getInstance().getActiveInstances()) {

                    if (instance.getLayout().getName().equals("Nascraft"))
                        for (Player player : Bukkit.getOnlinePlayers())
                            if (instance.getInteraction(player) != null)
                                LayoutModifier.getInstance().updateMainPage(instance.getInteraction(player).getComponentTree(), true, player);

                }

            // FundsManager.getInstance().operateBrokers();

            // LimitOrdersManager.getInstance().checkOrders();

            if (Config.getInstance().getDiscordEnabled()) {
                if (Config.getInstance().getDiscordMenuEnabled()) {
                    DiscordBot.getInstance().update();
                    DiscordAlerts.getInstance().updateAlerts();
                }
                if (Config.getInstance().getLogChannelEnabled()) {
                    DiscordLog.getInstance().flushBuffer();
                }
            }

        }, timeRemaining.getSeconds()*ticksPerSecond, 60L * ticksPerSecond);
    }

    private void saveDataTask() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            DatabaseManager.get().getDatabase().saveEverything();

            DatabaseManager.get().getDatabase().saveCPIValue(MarketManager.getInstance().getConsumerPriceIndex());

        }, 60L * 5 * ticksPerSecond, 60L * 5 * ticksPerSecond); // 5 min
    }

    private void saveInstants() {

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllParentItems()) {

                item.getItemStats().addInstant(new Instant(
                        LocalDateTime.now(),
                        item.getPrice().getValue(),
                        item.getVolume()
                ));

                item.restartVolume();
            }

        }, 2400, 60L * ticksPerSecond);
    }

    private void hourlyTask() {
        LocalTime timeNow = LocalTime.now();

        LocalTime nextHour = timeNow.plusHours(1).withMinute(0).withSecond(0);
        Duration timeRemaining = Duration.between(timeNow, nextHour);

        Bukkit.getScheduler().runTaskTimerAsynchronously(Nascraft.getInstance(), () -> {

            for (Item item : MarketManager.getInstance().getAllItems()) {
                item.getPrice().restartHourLimits();
            }

            MarketManager.getInstance().setOperationsLastHour(0);

            if (DatabaseManager.get().getDatabase() instanceof SQLite) {
                ((SQLite) DatabaseManager.get().getDatabase()).flush();
            }

            if (Config.getInstance().getAlertsMenuEnabled()) DatabaseManager.get().getDatabase().purgeAlerts();

        }, timeRemaining.getSeconds()*ticksPerSecond, 60 * 60 * ticksPerSecond); // 1 hour
    }
}
