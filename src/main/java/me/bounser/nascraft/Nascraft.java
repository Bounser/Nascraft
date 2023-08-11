package me.bounser.nascraft;

import de.leonhard.storage.Json;
import de.leonhard.storage.util.FileUtils;
import me.bounser.nascraft.advancedgui.LayoutModifier;
import me.bounser.nascraft.commands.MarketCommand;
import me.bounser.nascraft.commands.NascraftCommand;
import me.bounser.nascraft.commands.SellHandCommand;
import me.bounser.nascraft.commands.sellall.SellAllCommand;
import me.bounser.nascraft.commands.sellall.SellAllTabCompleter;
import me.bounser.nascraft.commands.sellinv.InventoryListener;
import me.bounser.nascraft.commands.sellinv.SellInvCommand;
import me.bounser.nascraft.database.Data;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.placeholderapi.PAPIExpansion;
import me.bounser.nascraft.config.Config;
import me.leoko.advancedgui.manager.LayoutManager;
import me.bounser.nascraft.bstats.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

import java.io.*;
import java.util.List;

public final class Nascraft extends JavaPlugin {

    private static Nascraft main;
    private static Economy economy = null;

    // AdvancedGUI version used to test the plugin.
    private final String AdvancedGUI_version = "2.2.6";

    public static Nascraft getInstance() { return main; }

    @Override
    public void onEnable() {

        main = this;

        new Metrics(this, 18404);

        Config.getInstance();

        if (!setupEconomy()) {
            getLogger().severe("Nascraft failed to load! Vault is required.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("AdvancedGUI") == null) {
            getLogger().warning("AdvancedGUI is not installed! Graphs won't work without it!");
            getLogger().warning("Learn more about AdvancedGUI here: https://www.spigotmc.org/resources/83636/");
        } else if (!Bukkit.getPluginManager().getPlugin("AdvancedGUI").getDescription().getVersion().equals(AdvancedGUI_version)){
            getLogger().warning("This plugin was made using AdvancedGUI " + AdvancedGUI_version + "! You may encounter errors on other versions");
        }

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
        }

        if (Config.getInstance().getCheckResources()) { checkResources(); }

        MarketManager.getInstance();

        getCommand("nascraft").setExecutor(new NascraftCommand());
        getCommand("market").setExecutor(new MarketCommand());

        List<String> commands = Config.getInstance().getCommands();
        if(commands.contains("sellhand")) getCommand("sellhand").setExecutor(new SellHandCommand());
        if(commands.contains("sellall")) getCommand("sellall").setExecutor(new SellAllCommand());
        if(commands.contains("sell")) getCommand("sellinv").setExecutor(new SellInvCommand());

        getCommand("sellall").setTabCompleter(new SellAllTabCompleter());

        Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);

        LayoutManager.getInstance().registerLayoutExtension(LayoutModifier.getInstance(), this);
    }

    @Override
    public void onDisable() { Data.getInstance().shutdownDatabase(); }

    public static Economy getEconomy() { return economy; }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) { return false; }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) { return false; }

        economy = rsp.getProvider();
        return economy != null;
    }

    public void checkResources() {

        getLogger().info("Checking required layouts... ");
        getLogger().info("If you want to disable this procedure, set auto_resources_injection to false in the config.yml file.");

        File toLayout0 = new File(getDataFolder().getParent() + "/AdvancedGUI/layout/Nascraft.json");

        if (!toLayout0.exists()) {
            InputStream fromLayout0 = getResource("Nascraft.json");
            assert fromLayout0 != null;
            FileUtils.writeToFile(toLayout0, fromLayout0);
            getLogger().info("Layout Nascraft.json added.");

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ag reload");
        } else {
            Json layout = new Json("Nascraft", getDataFolder().getParent() + "/AdvancedGUI/layout/");

            String ver = layout.getString("layoutVersion");

            if(ver == null || !ver.equals(getDescription().getVersion())) {
                getLogger().info("Layout outdated, updating...");

                InputStream fromLayout0 = getResource("Nascraft.json");
                assert fromLayout0 != null;
                FileUtils.writeToFile(toLayout0, fromLayout0);
                getLogger().info("Layout Nascraft.json updated. (From version " + ver + " to " + getDescription().getVersion() + ")" );

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ag reload");
                return;
            }
            getLogger().info("Layout present and in the correct version.");
        }
    }
}
