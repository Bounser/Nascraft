package me.bounser.nascraft;

import me.bounser.nascraft.scheduler.FoliaScheduler;
import me.bounser.nascraft.web.WebConfig;
import me.bounser.nascraft.web.WebServerManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Nascraft 2 entrypoint that restores the self-hosted website shipped in the
 * official Nascraft 1.9.1 release JAR.
 */
public class NascraftWebEntrypoint extends Nascraft {

    private static final String WEB_BUNDLE_RESOURCE = "web-original-1.9.1.zip";
    private static final String WEB_BUNDLE_VERSION = "1.9.1-original";
    private static final Set<String> ALLOWED_WEB_FILES = Set.of(
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

        if (!webDirectory.exists() && !webDirectory.mkdirs()) {
            getLogger().warning("Could not create web directory: " + webDirectory.getAbsolutePath());
        }

        try (InputStream resource = getResource(WEB_BUNDLE_RESOURCE)) {
            if (resource == null) {
                getLogger().severe("Bundled original web frontend is missing: " + WEB_BUNDLE_RESOURCE);
                return;
            }

            try (ZipInputStream zip = new ZipInputStream(resource)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String path = entry.getName().replace('\\', '/');
                    if (!ALLOWED_WEB_FILES.contains(path) || entry.isDirectory()) continue;

                    File destination = new File(getDataFolder(), path);
                    File parent = destination.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new IOException("Could not create directory " + parent);
                    }

                    try (FileOutputStream output = new FileOutputStream(destination, false)) {
                        zip.transferTo(output);
                    }
                    getLogger().info("Restored original web resource: " + path);
                }
            }

            Files.writeString(marker.toPath(), WEB_BUNDLE_VERSION + System.lineSeparator(), StandardCharsets.UTF_8);
            getLogger().info("Original Nascraft 1.9.1 web frontend restored to " + webDirectory.getAbsolutePath());
        } catch (IOException exception) {
            getLogger().log(Level.SEVERE, "Failed to restore original Nascraft 1.9.1 web frontend.", exception);
        }
    }
}
