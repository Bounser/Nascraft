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
import me.bounser.nascraft.inventorygui.Portfolio.PortfolioInventory;
import me.bounser.nascraft.commands.market.MarketCommand;
import me.bounser.nascraft.commands.sell.SellHandCommand;
import me.bounser.nascraft.commands.sell.SellAllCommand;
import me.bounser.nascraft.commands.sell.sellinv.SellInvListener;
import me.bounser.nascraft.commands.sell.sellinv.SellInvCommand;
import me.bounser.nascraft.commands.sellwand.GiveSellWandCommand;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.discord.DiscordBot;
import me.bounser.nascraft.commands.discord.LinkCommand;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.discord.linking.LinkingMethod;
import me.bounser.nascraft.inventorygui.InventoryListener;
import me.bounser.nascraft.managers.DebtManager;
import me.bounser.nascraft.managers.EventsManager;
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
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;


public final class Nascraft extends JavaPlugin {

    private static Nascraft main;
    private static NascraftAPI apiInstance;
    private static Economy economy = null;
    private static Permission perms = null;

    private static final String AGUI_VERSION = "2.2.8";

    private BukkitAudiences adventure;

    private WebServerManager webServerManager;

    public static Nascraft getInstance() { return main; }

    public static NascraftAPI getAPI() { return apiInstance == null ? apiInstance = new NascraftAPI() : apiInstance; }

    @Override
    public void onEnable() {

        main = this;

        Config config = Config.getInstance();

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

            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                webServerManager.startServer();
            });
        }
    }

    @Override
    public void onDisable() {

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

        metrics.addCustomChart(new SimplePie("discord_bridge", () -> String.valueOf(Config.getInstance().getDiscordEnabled())));

        if (Config.getInstance().getDiscordEnabled())
            metrics.addCustomChart(new SimplePie("linking_method", () -> Config.getInstance().getLinkingMethod().toString()));

        metrics.addCustomChart(new SimplePie("used_with_advancedgui", () -> String.valueOf(Bukkit.getPluginManager().getPlugin("AdvancedGUI") != null)));
        metrics.addCustomChart(new SingleLineChart("operations_per_hour", () -> MarketManager.getInstance().getOperationsLastHour()));
        metrics.addCustomChart(new AdvancedPie("players_linked_with_discord", new Callable<Map<String, Integer>>() {
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

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) { return false; }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) { return false; }

        economy = rsp.getProvider();
        return economy != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    public @NonNull BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    private void createImagesFolder() {

        File imagesFolder = new File(getDataFolder(), "images");

        if (!imagesFolder.exists()) {
            boolean success = imagesFolder.mkdirs();
            if (!success) getLogger().warning("Failed to create images folder.");
        }
    }

    private void checkResources() {

        getLogger().info("Checking required layouts... ");
        getLogger().info("If you want to disable this procedure, set auto_resources_injection to false in the config.yml file.");

        File fileToReplace = new File(getDataFolder().getParent() + "/AdvancedGUI/layout/Nascraft.json");

        if (!fileToReplace.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(getResource("Nascraft.json")));
                StringBuilder jsonContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }
                reader.close();

                FileUtils.writeStringToFile(fileToReplace, jsonContent.toString(), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            getLogger().info("Layout Nascraft.json added.");

            LayoutManager.getInstance().shutdownSync();
            GuiWallManager.getInstance().shutdown();
            GuiItemManager.getInstance().shutdown();

            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                AdvancedGUI.getInstance().readConfig();
                VersionMediator.reload();
                LayoutManager.getInstance().reload(layout -> getLogger().severe("§cFailed to load layout: " + layout + " §7(see console for details)"));
                Bukkit.getScheduler().runTask(AdvancedGUI.getInstance(), () -> {
                    GuiWallManager.getInstance().setup();
                    GuiItemManager.getInstance().setup();
                });
            });
        } else {
            getLogger().info("Layout (Nascraft.json) present!");
        }
    }

    private void extractDefaultWebFiles() {
        getLogger().info("Checking external web directory: " + new File(getDataFolder(), "web").getPath());

        String[] essentialFiles = {
                "web/index.html",
                "web/style.css",
                "web/script.js"
        };

        boolean copiedAny = false;
        for (String resourcePath : essentialFiles) {
            File targetFile = new File(getDataFolder(), resourcePath);
            try {
                if (!targetFile.exists()) {
                    saveResource(resourcePath, false);
                    getLogger().info("Copied default file: " + resourcePath);
                    copiedAny = true;
                }
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.SEVERE, "Error extracting: " + resourcePath, e);
            }
        }

        if (!copiedAny) {
            getLogger().info("External web files are present.");
        } else {
            getLogger().info("Default web files copied to " + new File(getDataFolder(), "web").getPath());
        }

    }

    private void extractImage(String resourcePath) {
        File targetFile = new File(getDataFolder(), resourcePath);

        if (!targetFile.exists()) {
            try {
                saveResource(resourcePath, false);
            } catch (IllegalArgumentException e) {
                getLogger().log(Level.SEVERE, "Failed to extract image: Resource path '" + resourcePath + "' not found within the plugin JAR!", e);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "An unexpected error occurred while extracting image '" + resourcePath + "'", e);
            }
        }
    }


}
