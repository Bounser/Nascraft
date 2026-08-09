package me.bounser.nascraft;

import me.bounser.nascraft.scheduler.FoliaScheduler;
import me.bounser.nascraft.web.WebConfig;
import me.bounser.nascraft.web.WebServerManager;

import java.io.File;
import java.util.logging.Level;

/**
 * Nascraft 2 entrypoint that restores the self-hosted website shipped in the
 * 1.9.1 release JAR. Keeping the web bootstrap here makes the restoration
 * isolated from the core plugin lifecycle while the 2.0 branch is tested.
 */
public class NascraftWebEntrypoint extends Nascraft {

    private WebServerManager webServerManager;

    @Override
    public void onEnable() {
        super.onEnable();

        WebConfig webConfig = new WebConfig(this);
        if (!webConfig.enabled()) return;

        extractDefaultWebFiles();
        extractImage("images/logo.png");
        extractImage("images/logo-color.png");
        extractImage("images/fire.png");

        webServerManager = new WebServerManager(this, webConfig);
        FoliaScheduler.runAsync(this, webServerManager::startServer);
    }

    @Override
    public void onDisable() {
        if (webServerManager != null && webServerManager.isRunning()) {
            getLogger().info("Stopping web server...");
            webServerManager.stopServer();
        }
        super.onDisable();
    }

    private void extractDefaultWebFiles() {
        String[] resources = {"web/index.html", "web/style.css", "web/script.js"};
        boolean extracted = false;

        File webDirectory = new File(getDataFolder(), "web");
        if (!webDirectory.exists() && !webDirectory.mkdirs()) {
            getLogger().warning("Could not create web directory: " + webDirectory.getAbsolutePath());
        }

        for (String resource : resources) {
            File destination = new File(getDataFolder(), resource);
            if (destination.exists()) continue;

            try {
                saveResource(resource, false);
                getLogger().info("Extracted default web resource: " + resource);
                extracted = true;
            } catch (IllegalArgumentException exception) {
                getLogger().log(Level.SEVERE, "Bundled web resource is missing: " + resource, exception);
            }
        }

        if (!extracted) getLogger().info("External web files are present at " + webDirectory.getAbsolutePath());
        else getLogger().info("Web files extracted to " + webDirectory.getAbsolutePath());
    }

    private void extractImage(String resource) {
        File destination = new File(getDataFolder(), resource);
        if (destination.exists()) return;

        File parent = destination.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try {
            saveResource(resource, false);
        } catch (IllegalArgumentException exception) {
            getLogger().log(Level.WARNING, "Bundled web image is missing: " + resource, exception);
        }
    }
}
