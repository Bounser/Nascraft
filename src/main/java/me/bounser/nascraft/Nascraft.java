package me.bounser.nascraft;

import de.leonhard.storage.util.FileUtils;
import me.bounser.nascraft.commands.NascraftCommand;
import me.bounser.nascraft.tools.Config;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public final class Nascraft extends JavaPlugin {

    private static Nascraft main;
    public static Nascraft getInstance(){ return main; }

    @Override
    public void onEnable() {
        main = this;

        if (Config.getInstance().getCheckResources()){ checkResources(); }

        getCommand("nascraft").setExecutor(new NascraftCommand(this));

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
