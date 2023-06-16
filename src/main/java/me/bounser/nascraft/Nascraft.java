package me.bounser.nascraft;

import de.leonhard.storage.Json;
import de.leonhard.storage.util.FileUtils;
import me.bounser.nascraft.advancedgui.LayoutModifier;
import me.bounser.nascraft.commands.MarketCommand;
import me.bounser.nascraft.commands.NascraftCommand;
import me.bounser.nascraft.commands.SellCommand;
import me.bounser.nascraft.database.Data;
import me.bounser.nascraft.database.SQLManager;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.placeholderapi.PAPIExpansion;
import me.bounser.nascraft.tools.Config;
import me.leoko.advancedgui.manager.LayoutManager;
import me.bounser.nascraft.tools.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

import java.io.*;

public final class Nascraft extends JavaPlugin {

    private static Nascraft main;
    private static Economy econ = null;
    private SQLManager sqlManager;

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

        if(Bukkit.getPluginManager().getPlugin("AdvancedGUI") == null) {
            getLogger().warning("AdvancedGUI is not installed! Graphs won't work without it!");
            getLogger().warning("Learn more about AdvancedGUI here: https://www.spigotmc.org/resources/83636/");
        }

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIExpansion(this).register();
        }

        if (Config.getInstance().getCheckResources()) { checkResources(); }

        MarketManager.getInstance();

        getCommand("nascraft").setExecutor(new NascraftCommand());
        getCommand("market").setExecutor(new MarketCommand());
        getCommand("sell").setExecutor(new SellCommand());

        LayoutManager.getInstance().registerLayoutExtension(LayoutModifier.getInstance(), this);
    }

    @Override
    public void onDisable() { Data.getInstance().shutdownDatabase(); }

    public static Economy getEconomy() { return econ; }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) { return false; }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) { return false; }

        econ = rsp.getProvider();
        return econ != null;
    }

    public void checkResources() {

        getLogger().info("Checking required layouts... ");
        getLogger().info("If you want to disable this procedure, set AutoLayoutInjection to false in the config.yml file.");

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
