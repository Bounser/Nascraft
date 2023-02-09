package me.bounser.nascraft;

import de.leonhard.storage.util.FileUtils;
import me.bounser.nascraft.advancedgui.LayoutModifier;
import me.bounser.nascraft.commands.MarketCommand;
import me.bounser.nascraft.commands.NascraftCommand;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.tools.Config;
import me.leoko.advancedgui.manager.LayoutManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public final class Nascraft extends JavaPlugin {

    private static Nascraft main;
    private static Economy econ = null;

    public static Nascraft getInstance(){ return main; }

    @Override
    public void onEnable() {
        main = this;

        if (!setupEconomy()) {
            getLogger().severe("Nascraft failed to load! Vault is required.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (Config.getInstance().getCheckResources()) { /*checkResources();*/ }

        MarketManager.getInstance();

        getCommand("nascraft").setExecutor(new NascraftCommand());
        getCommand("market").setExecutor(new MarketCommand());

        LayoutManager.getInstance().registerLayoutExtension(new LayoutModifier(), this);
    }

    @Override
    public void onDisable() { /*Data.getInstance().savePrices();*/ }


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
        if (toLayout0.exists()) {

            InputStream input = null;
            try {
                input = new FileInputStream(toLayout0);
            } catch (FileNotFoundException e) {
                getLogger().info("Error trying to read layout Nascraft.json in AdvancedGUI's layouts folder");
                e.printStackTrace();
            }
            InputStream fromLayout0 = getResource("Nascraft.json");

            if (input != fromLayout0) {
                getLogger().info("Layout Nascraft.json updated.");
                FileUtils.writeToFile(toLayout0, fromLayout0);
            }

        } else {

            InputStream fromLayout0 = getResource("Nascraft.json");

            getLogger().info("Layout Nascraft.json added.");
            FileUtils.writeToFile(toLayout0, fromLayout0);

        }

    }
}
