package me.bounser.nascraft;

import me.bounser.nascraft.advancedgui.LayoutModifier;
import me.bounser.nascraft.api.NascraftAPI;
import me.bounser.nascraft.chart.price.ItemChartReduced;
import me.bounser.nascraft.commands.admin.nascraft.NascraftCommand;
import me.bounser.nascraft.commands.admin.nascraft.NascraftLogListener;
import me.bounser.nascraft.commands.admin.marketeditor.edit.item.EditItemMenuListener;
import me.bounser.nascraft.commands.admin.marketeditor.edit.category.CategoryEditorListener;
import me.bounser.nascraft.commands.admin.marketeditor.overview.MarketEditorInvListener;
import me.bounser.nascraft.commands.alert.AlertsCommand;
import me.bounser.nascraft.commands.alert.SetAlertCommand;
import me.bounser.nascraft.commands.discord.DiscordCommand;
import me.bounser.nascraft.commands.portfolio.PortfolioCommand;
import me.bounser.nascraft.database.DataMigrationManager;
import me.bounser.nascraft.inventorygui.Portfolio.PortfolioInventory;
import me.bounser.nascraft.commands.market.MarketCommand;
import me.bounser.nascraft.commands.sell.SellHandCommand;
import me.bounser.nascraft.commands.sell.SellAllCommand;
import me.bounser.nascraft.commands.sell.sellinv.SellInvListener;
import me.bounser.nascraft.commands.sell.sellinv.SellInvCommand;
import me.bounser.nascraft.commands.sellwand.GiveSellWandCommand;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.DatabaseType;
import me.bounser.nascraft.discord.DiscordBot;
import me.bounser.nascraft.commands.discord.LinkCommand;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.discord.linking.LinkingMethod;
import me.bounser.nascraft.inventorygui.InventoryListener;
import me.bounser.nascraft.managers.CacheManager;
import me.bounser.nascraft.managers.DebtManager;
import me.bounser.nascraft.managers.EventsManager;
import me.bounser.nascraft.managers.scheduler.SchedulerAdapter;
import me.bounser.nascraft.managers.scheduler.SchedulerManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.placeholderapi.PAPIExpansion;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.sellwand.WandListener;
import me.bounser.nascraft.sellwand.WandsManager;
import me.bounser.nascraft.updatechecker.UpdateChecker;
import me.bounser.nascraft.web.WebServerManager;
import me.leoko.advancedgui.AdvancedGUI;
import me.leoko.advancedgui.manager.GuiItemManager;
import me.leoko.advancedgui.manager.GuiWallManager;
import me.leoko.advancedgui.manager.LayoutManager;
import me.leoko.advancedgui.utils.VersionMediator;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

import net.milkbowl.vault.economy.Economy;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.lang.reflect.Method;
import me.bounser.nascraft.managers.TasksManager;


public final class Nascraft extends JavaPlugin {

    private static Nascraft main;
    private static NascraftAPI apiInstance;
    private static Economy economy = null;
    private static Permission perms = null;

    private static final String AGUI_VERSION = "2.2.8";

    private BukkitAudiences adventure;
    private SchedulerAdapter schedulerAdapter;
    private CacheManager cacheManager;
    private DataMigrationManager dataMigrationManager;
    
    private WebServerManager webServerManager;

    public static Nascraft getInstance() { return main; }

    public static NascraftAPI getAPI() { return apiInstance == null ? apiInstance = new NascraftAPI() : apiInstance; }

    @Override
    public void onEnable() {

        main = this;

        Config config = Config.getInstance();

        this.schedulerAdapter = SchedulerManager.init(this);
        
        this.cacheManager = CacheManager.getInstance();
        
        this.dataMigrationManager = new DataMigrationManager(this);

        setupMetrics();

        new UpdateChecker(this, 108216).getVersion(version -> {
            if (!getDescription().getVersion().equals(version))
                getLogger().info("There is a new version available! Download it here: https://www.spigotmc.org/resources/108216/");
        });

        this.adventure = BukkitAudiences.create(this);

        if (!setupEconomy())
            getLogger().warning("Vault is not installed! You'll have to provide another supplier.");

        setupPermissions();

        Plugin AGUI = Bukkit.getPluginManager().getPlugin("AdvancedGUI");

        if (AGUI == null || !AGUI.isEnabled()) {
            getLogger().warning("AdvancedGUI is not installed! You won't have graphs in-game without it!");
            getLogger().warning("Learn more about AdvancedGUI here: https://www.spigotmc.org/resources/83636/");
        } else {
            if (config.getCheckResources()) checkResources();
            LayoutModifier.getInstance();
            if (!Bukkit.getPluginManager().getPlugin("AdvancedGUI").getDescription().getVersion().equals(AGUI_VERSION))
                getLogger().warning("This plugin was made using AdvancedGUI " + AGUI_VERSION + "! You may encounter errors on other versions");
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI detected!");
            new PAPIExpansion().register();
        }

        if (config.getDiscordEnabled()) {
            getLogger().info("Enabling discord extension...");

            if (Config.getInstance().getLinkingMethod().equals(LinkingMethod.NATIVE)
            && config.isCommandEnabled("link")) new LinkCommand();
            if (Config.getInstance().getOptionAlertEnabled()) {
                if (config.isCommandEnabled("alerts")) new AlertsCommand();
                if (config.isCommandEnabled("setalerts")) new SetAlertCommand();
            }
            if (config.isCommandEnabled("discord")) new DiscordCommand();

            new DiscordBot();
            getLogger().info("Discord extension loaded!");
        }

        if (config.getSellWandsEnabled()) {
            if (config.isCommandEnabled("givesellwand")) new GiveSellWandCommand();
            Bukkit.getPluginManager().registerEvents(new WandListener(), this);
            WandsManager.getInstance();
        }

        if (config.getLoansEnabled()) {
            DebtManager.getInstance();
        }

        createImagesFolder();

        MarketManager.getInstance();

        if (config.isCommandEnabled("nascraft")) {
            new NascraftCommand();

            Bukkit.getPluginManager().registerEvents(new NascraftLogListener(), this);

            Bukkit.getPluginManager().registerEvents(new MarketEditorInvListener(), this);
            Bukkit.getPluginManager().registerEvents(new EditItemMenuListener(), this);
            Bukkit.getPluginManager().registerEvents(new CategoryEditorListener(), this);
        }

        if (config.isCommandEnabled("market")) {
            new MarketCommand();
            Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);
        }

        if (config.isCommandEnabled("sellhand")) new SellHandCommand();

        if (config.isCommandEnabled("sell-menu")) {
            new SellInvCommand();
            Bukkit.getPluginManager().registerEvents(new SellInvListener(), this);
        }

        if (config.isCommandEnabled("sellall")) new SellAllCommand();

        if (config.isCommandEnabled("portfolio")) {
            new PortfolioCommand();
            Bukkit.getPluginManager().registerEvents(new PortfolioInventory(), this);
        }

        Bukkit.getPluginManager().registerEvents(new EventsManager(), this);
        ItemChartReduced.load();

        if (config.getWebEnabled()) {
            extractDefaultWebFiles();
            extractImage("images/logo.png");
            extractImage("images/logo-color.png");
            extractImage("images/fire.png");

            webServerManager = new WebServerManager(this, config.getWebPort());

            SchedulerManager.getInstance().runAsync(() -> {
                webServerManager.startServer();
            });
        }
        
        if (SchedulerManager.isFolia()) {
            getLogger().info("Running in Folia mode with region-aware scheduling");
        } else {
            getLogger().info("Running in standard Bukkit mode");
        }
        
        if (Config.getInstance().getDatabaseType() == DatabaseType.REDIS && Config.getInstance().getRedisUseFallback()) {
            getLogger().info("Redis database with SQLite fallback is enabled");
        }
    }

    @Override
    public void onDisable() {
        if (TasksManager.instance != null) {
            TasksManager.getInstance().prepareForShutdown();
        }

        getLogger().info("Saving and closing connection with database...");
        DatabaseManager.get().getDatabase().disconnect();
        getLogger().info("Done!");

        if (Config.getInstance().getDiscordEnabled() && DiscordBot.getInstance() != null) {
            DiscordBot.getInstance().sendClosedMessage();
            DiscordBot.getInstance().getJDA().shutdown();
        }

        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }

        if (webServerManager != null && webServerManager.isRunning()) {
            getLogger().info("Stopping web server...");
            webServerManager.stopServer();
        }
    }

    private void setupMetrics() {
        Metrics metrics = new Metrics(this, 18404);

        metrics.addCustomChart(new org.bstats.charts.SimplePie("discord_bridge", () -> String.valueOf(Config.getInstance().getDiscordEnabled())));

        if (Config.getInstance().getDiscordEnabled())
            metrics.addCustomChart(new org.bstats.charts.SimplePie("linking_method", () -> Config.getInstance().getLinkingMethod().toString()));

        metrics.addCustomChart(new org.bstats.charts.SimplePie("used_with_advancedgui", () -> String.valueOf(Bukkit.getPluginManager().getPlugin("AdvancedGUI") != null)));
        metrics.addCustomChart(new org.bstats.charts.SingleLineChart("operations_per_hour", () -> MarketManager.getInstance().getOperationsLastHour()));
        metrics.addCustomChart(new org.bstats.charts.SimplePie("folia_enabled", () -> String.valueOf(SchedulerManager.isFolia())));
        metrics.addCustomChart(new org.bstats.charts.SimplePie("redis_enabled", () -> String.valueOf(Config.getInstance().getDatabaseType() == DatabaseType.REDIS)));
        metrics.addCustomChart(new org.bstats.charts.AdvancedPie("players_linked_with_discord", new Callable<Map<String, Integer>>() {
            @Override
            public Map<String, Integer> call() {
                Map<String, Integer> valueMap = new HashMap<>();

                if (!Config.getInstance().getDiscordEnabled()) return valueMap;

                int linkedPlayers = getLinkedPlayers();
                valueMap.put("Linked", linkedPlayers);
                valueMap.put("Not linked", Bukkit.getOnlinePlayers().size() - linkedPlayers);
                return valueMap;
            }

            private int getLinkedPlayers() {
                int counter = 0;
                for (Player player : Bukkit.getOnlinePlayers())
                    if (LinkManager.getInstance().getUserDiscordID(player.getUniqueId()) != null) counter++;
                return counter;
            }
        }));
    }

    public static Economy getEconomy() { return economy; }

    public static Permission getPermissions() { return perms; }
    
    /**
     * Get the scheduler adapter
     * 
     * @return The scheduler adapter
     */
    public SchedulerAdapter getSchedulerAdapter() {
        return schedulerAdapter;
    }
    
    /**
     * Get the cache manager
     * 
     * @return The cache manager
     */
    public CacheManager getCacheManager() {
        return cacheManager;
    }
    
    /**
     * Get the data migration manager
     * 
     * @return The data migration manager
     */
    public DataMigrationManager getDataMigrationManager() {
        return dataMigrationManager;
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp != null) perms = rsp.getProvider();
        return perms != null;
    }

    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    private void createImagesFolder() {
        File imageFolder = new File(getDataFolder(), "images");
        if (!imageFolder.exists()) {
            imageFolder.mkdirs();
        }
    }

    private void checkResources() {
        try {
            if (Bukkit.getPluginManager().getPlugin("AdvancedGUI") == null) {
                getLogger().warning("AdvancedGUI is not installed! Graph functionality will be limited.");
                return;
            }
            
            GuiItemManager gim = GuiItemManager.getInstance();
            GuiWallManager gwm = GuiWallManager.getInstance();
            LayoutManager lm = LayoutManager.getInstance();
            
            if (!hasItemKey(gim, "nascraft_item")) {
                getLogger().warning("Missing nascraft_item in AdvancedGUI configuration!");
            }
            
            File resourcesFolder = new File(getDataFolder(), "resources");
            if (!resourcesFolder.exists()) {
                resourcesFolder.mkdirs();
            }
            
            extractLayoutIfNeeded(lm, resourcesFolder, "nascraft_chart");
            extractLayoutIfNeeded(lm, resourcesFolder, "nascraft_portfolio");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error checking AdvancedGUI resources", e);
        }
    }
    
    private boolean hasItemKey(GuiItemManager gim, String key) {
        try {
            Method keyExistsMethod = GuiItemManager.class.getDeclaredMethod("keyExists", String.class);
            keyExistsMethod.setAccessible(true);
            return (boolean) keyExistsMethod.invoke(gim, key);
        } catch (Exception e) {
            getLogger().warning("Could not check if item key exists: " + key);
            return false;
        }
    }
    
    private void extractLayoutIfNeeded(LayoutManager lm, File resourcesFolder, String layoutName) {
        try {
            Method layoutExistsMethod = LayoutManager.class.getDeclaredMethod("layoutExists", String.class);
            layoutExistsMethod.setAccessible(true);
            
            if (!(boolean) layoutExistsMethod.invoke(lm, layoutName)) {
                File layoutFile = new File(resourcesFolder, layoutName + ".json");
                if (!layoutFile.exists()) {
                    InputStream in = getResource("layouts/" + layoutName + ".json");
                    if (in != null) {
                        FileUtils.copyInputStreamToFile(in, layoutFile);
                        getLogger().info("Extracted layout: " + layoutName);
                    }
                }
                
                Method extractLayoutMethod = LayoutManager.class.getDeclaredMethod("extractLayout", File.class);
                extractLayoutMethod.setAccessible(true);
                extractLayoutMethod.invoke(lm, layoutFile);
            }
        } catch (Exception e) {
            getLogger().warning("Could not extract layout: " + layoutName);
        }
    }

    private void extractDefaultWebFiles() {
        File webDir = new File(getDataFolder(), "web");
        if (!webDir.exists()) {
            webDir.mkdirs();
        }

        String[] webFiles = {
                "index.html", "script.js", "style.css"
        };

        for (String fileName : webFiles) {
            File targetFile = new File(webDir, fileName);
            if (!targetFile.exists()) {
                try (InputStream in = getClass().getResourceAsStream("/web/" + fileName)) {
                    if (in != null) {
                        try (FileOutputStream out = new FileOutputStream(targetFile)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "Failed to extract web file: " + fileName, e);
                }
            }
        }
    }

    private void extractImage(String resourcePath) {
        File imageDir = new File(getDataFolder(), "web/images");
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }

        String fileName = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
        File targetFile = new File(imageDir, fileName);

        if (!targetFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/web/" + resourcePath)) {
                if (in != null) {
                    try (FileOutputStream out = new FileOutputStream(targetFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Failed to extract image: " + fileName, e);
            }
        }
    }

}
