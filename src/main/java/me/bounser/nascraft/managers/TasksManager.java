package me.bounser.nascraft.managers;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.advancedgui.LayoutModifier;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.discord.DiscordBot;
import me.bounser.nascraft.discord.DiscordLog;
import me.bounser.nascraft.managers.scheduler.SchedulerManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.stats.Instant;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.portfolio.PortfoliosManager;
import me.leoko.advancedgui.manager.GuiWallManager;
import me.leoko.advancedgui.utils.GuiWallInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

public class TasksManager {

    public static TasksManager instance;

    private final int ticksPerSecond = 20;

    private final Plugin AGUI = Bukkit.getPluginManager().getPlugin("AdvancedGUI");
    
    // Store task IDs for potential cancellation
    private AtomicInteger shortTermPricesTaskId = new AtomicInteger(-1);
    private AtomicInteger discordTaskId = new AtomicInteger(-1);
    private AtomicInteger noiseTaskId = new AtomicInteger(-1);
    private AtomicInteger saveDataTaskId = new AtomicInteger(-1);
    private AtomicInteger saveInstantsTaskId = new AtomicInteger(-1);
    private AtomicInteger hourlyTaskId = new AtomicInteger(-1);
    
    // Flag to prevent scheduling tasks during shutdown
    private boolean isShuttingDown = false;

    public static TasksManager getInstance() { return instance == null ? instance = new TasksManager() : instance; }

    private TasksManager(){

        LocalTime timeNow = LocalTime.now();

        LocalTime nextMinute = timeNow.plusMinutes(1).withSecond(0);
        Duration timeRemaining = Duration.between(timeNow, nextMinute);

        // Registering tasks:
        saveDataTask();
        noiseTask((int) timeRemaining.getSeconds());
        discordTask((int) timeRemaining.getSeconds());
        shortTermPricesTask((int) timeRemaining.getSeconds());
        hourlyTask();
        saveInstants();

        DatabaseManager.get().getDatabase().purgeHistory();
    }
    
    /**
     * Sets the shutdown flag to prevent scheduling new tasks during plugin shutdown
     */
    public void prepareForShutdown() {
        isShuttingDown = true;
        cancelAllTasks();
    }
    
    /**
     * Cancels all scheduled tasks
     */
    public void cancelAllTasks() {
        if (shortTermPricesTaskId.get() != -1)
            SchedulerManager.getInstance().cancelTask(shortTermPricesTaskId.get());
        
        if (discordTaskId.get() != -1)
            SchedulerManager.getInstance().cancelTask(discordTaskId.get());
        
        if (noiseTaskId.get() != -1)
            SchedulerManager.getInstance().cancelTask(noiseTaskId.get());
        
        if (saveDataTaskId.get() != -1)
            SchedulerManager.getInstance().cancelTask(saveDataTaskId.get());
        
        if (saveInstantsTaskId.get() != -1)
            SchedulerManager.getInstance().cancelTask(saveInstantsTaskId.get());
        
        if (hourlyTaskId.get() != -1)
            SchedulerManager.getInstance().cancelTask(hourlyTaskId.get());
    }

    private void shortTermPricesTask(int delay) {
        if (isShuttingDown) return;
        
        shortTermPricesTaskId.set(SchedulerManager.getInstance().scheduleAsyncRepeating(() -> {
            float allChanges = 0;
            for (Item item : MarketManager.getInstance().getAllParentItems()) {
                if (Config.getInstance().getPriceNoise())
                    allChanges += item.getPrice().getChange();

                item.lowerOperations();

                item.getPrice().addValueToShortTermStorage();
            }

            MarketManager.getInstance().updateMarketChange1h(allChanges/MarketManager.getInstance().getAllParentItems().size());

            if (AGUI != null &&
                AGUI.isEnabled() &&
                GuiWallManager.getInstance().getActiveInstances() != null) {

                for (GuiWallInstance instance : GuiWallManager.getInstance().getActiveInstances()) {
                    if (instance.getLayout().getName().equals("Nascraft")) {
                        // Use scheduler to run UI updates for each player in their appropriate region
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (instance.getInteraction(player) != null) {
                                final Player finalPlayer = player;
                                SchedulerManager.getInstance().runForEntity(player, (entity) -> {
                                    LayoutModifier.getInstance().updateMainPage(
                                            instance.getInteraction(finalPlayer).getComponentTree(), 
                                            true, 
                                            finalPlayer
                                    );
                                });
                            }
                        }
                    }
                }
            }
        }, (long) delay * ticksPerSecond, 60L * ticksPerSecond));
    }

    private void discordTask(int delay) {
        if (isShuttingDown) return;
        
        if (Config.getInstance().getDiscordEnabled()) {
            discordTaskId.set(SchedulerManager.getInstance().scheduleAsyncRepeating(() -> {
                if (Config.getInstance().getDiscordMenuEnabled()) {
                    DiscordBot.getInstance().update();
                    DiscordAlerts.getInstance().updateAlerts();
                }

                if (Config.getInstance().getLogChannelEnabled())
                    DiscordLog.getInstance().flushBuffer();
            }, (long) delay * ticksPerSecond, ((long) Config.getInstance().getUpdateTime() * ticksPerSecond)));
        }
    }

    private void noiseTask(int delay) {
        if (isShuttingDown) return;
        
        noiseTaskId.set(SchedulerManager.getInstance().scheduleAsyncRepeating(() -> {
            // Check if this server should apply noise (only noise master applies noise)
            if (shouldApplyNoise()) {
                for (Item item : MarketManager.getInstance().getAllParentItems()) {
                    if (Config.getInstance().getPriceNoise())
                        item.getPrice().applyNoise();
                }
            }
        }, (long) delay * ticksPerSecond, (long) Config.getInstance().getNoiseTime() * ticksPerSecond));
    }
    
    /**
     * Check if this server should apply noise to prevent price change loops
     * @return true if this server should apply noise
     */
    private boolean shouldApplyNoise() {
        // Get the distributed sync instance from Redis database
        if (DatabaseManager.get().getDatabase() instanceof me.bounser.nascraft.database.redis.Redis) {
            me.bounser.nascraft.database.redis.Redis redis = (me.bounser.nascraft.database.redis.Redis) DatabaseManager.get().getDatabase();
            return redis.getDistributedSync().shouldApplyNoise();
        }
        
        // If not using Redis or distributed sync is not enabled, allow noise
        return true;
    }

    private void saveDataTask() {
        if (isShuttingDown) return;
        
        saveDataTaskId.set(SchedulerManager.getInstance().scheduleAsyncRepeating(() -> {
            DatabaseManager.get().getDatabase().saveEverything();
            DatabaseManager.get().getDatabase().saveCPIValue(MarketManager.getInstance().getConsumerPriceIndex());
            PortfoliosManager.getInstance().savePortfoliosWorthOfOnlinePlayers();
            
            // Player-specific operations should be done in their respective regions
            for (Player player : Bukkit.getOnlinePlayers()) {
                final Player finalPlayer = player;
                SchedulerManager.getInstance().runForEntity(player, (entity) -> {
                    // Use correct method name - saveOrUpdateName instead of updateBalance
                    DatabaseManager.get().getDatabase().saveOrUpdateName(finalPlayer.getUniqueId(), finalPlayer.getName());
                });
            }
        }, 60L * 5 * ticksPerSecond, 60L * 5 * ticksPerSecond)); // 5 min
    }

    private void saveInstants() {
        if (isShuttingDown) return;
        
        saveInstantsTaskId.set(SchedulerManager.getInstance().scheduleAsyncRepeating(() -> {
            for (Item item : MarketManager.getInstance().getAllParentItems()) {
                item.getItemStats().addInstant(new Instant(
                        LocalDateTime.now(),
                        item.getPrice().getValue(),
                        item.getVolume()
                ));

                item.restartVolume();
            }
        }, 2400, 60L * ticksPerSecond));
    }

    private void hourlyTask() {
        if (isShuttingDown) return;
        
        LocalTime timeNow = LocalTime.now();

        LocalTime nextHour = timeNow.plusHours(1).withMinute(0).withSecond(0);
        Duration timeRemaining = Duration.between(timeNow, nextHour);

        hourlyTaskId.set(SchedulerManager.getInstance().scheduleAsyncRepeating(() -> {
            for (Item item : MarketManager.getInstance().getAllItems()) {
                item.getPrice().restartHourLimits();
            }

            MarketManager.getInstance().setOperationsLastHour(0);

            if (Config.getInstance().getAlertsMenuEnabled()) 
                DatabaseManager.get().getDatabase().purgeAlerts();
                
        }, timeRemaining.getSeconds() * ticksPerSecond, 60 * 60 * ticksPerSecond)); // 1 hour
    }
}
