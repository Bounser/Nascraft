package me.bounser.nascraft;

import me.bounser.nascraft.scheduler.FoliaScheduler;
import me.bounser.nascraft.web.WebConfig;
import me.bounser.nascraft.web.WebServerManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;

/**
 * Nascraft 2 entrypoint that restores the self-hosted website shipped in the
 * official Nascraft 1.9.1 release JAR.
 */
public class NascraftWebEntrypoint extends Nascraft {

    private static final String WEB_BUNDLE_VERSION = "1.9.1-original-direct";
    private static final List<String> ORIGINAL_WEB_RESOURCES = List.of(
            "web/index.html",
            "web/style.css",
            "web/script.js",
            "images/logo.png",
            "images/logo-color.png",
            "images/fire.png"
    );

    private WebServerManager webServerManager;

    @Override
    public void onEnable() {
        super.onEnable();

        WebConfig webConfig = new WebConfig(this);
        if (!webConfig.enabled()) return;

        restoreOriginalWebFrontend();

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

    private void restoreOriginalWebFrontend() {
        File webDirectory = new File(getDataFolder(), "web");
        File marker = new File(webDirectory, ".nascraft-web-version");

        try {
            if (marker.isFile()
                    && Files.readString(marker.toPath(), StandardCharsets.UTF_8).trim().equals(WEB_BUNDLE_VERSION)) {
                getLogger().info("Original Nascraft 1.9.1 web frontend is present at " + webDirectory.getAbsolutePath());
                return;
            }
        } catch (IOException exception) {
            getLogger().log(Level.WARNING, "Could not read web frontend version marker; restoring bundled frontend.", exception);
        }

        try {
            for (String resource : ORIGINAL_WEB_RESOURCES) {
                File destination = new File(getDataFolder(), resource);
                File parent = destination.getParentFile();
                if (parent != null && !parent.exists() && !parent.mkdirs()) {
                    throw new IOException("Could not create directory " + parent);
                }

                saveResource(resource, true);
                getLogger().info("Restored original web resource: " + resource);
            }

            if (!webDirectory.exists() && !webDirectory.mkdirs()) {
                throw new IOException("Could not create web directory " + webDirectory);
            }
            Files.writeString(marker.toPath(), WEB_BUNDLE_VERSION + System.lineSeparator(), StandardCharsets.UTF_8);
            getLogger().info("Original Nascraft 1.9.1 web frontend restored to " + webDirectory.getAbsolutePath());
        } catch (IOException | IllegalArgumentException exception) {
            getLogger().log(Level.SEVERE, "Failed to restore original Nascraft 1.9.1 web frontend.", exception);
        }
    }
}
