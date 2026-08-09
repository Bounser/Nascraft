package me.bounser.nascraft.web;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Configuration facade for the self-hosted web extension.
 *
 * Nascraft 1.9.1 used the keys web.enabled/web.port. Nascraft 2 prefers the
 * website.* namespace while retaining the old keys as a migration fallback.
 */
public final class WebConfig {

    private final FileConfiguration config;

    public WebConfig(JavaPlugin plugin) {
        this.config = plugin.getConfig();
    }

    public boolean enabled() {
        if (config.contains("website.enabled")) return config.getBoolean("website.enabled");
        return config.getBoolean("web.enabled", false);
    }

    public int port() {
        if (config.contains("website.port")) return config.getInt("website.port", 8080);
        return config.getInt("web.port", 8080);
    }

    public String title() {
        return config.getString("website.title", "Nascraft Market");
    }

    public String accent() {
        return config.getString("website.accent", "#6E56CF");
    }

    public String defaultMode() {
        return config.getString("website.default-mode", "regular");
    }

    public boolean lockMode() {
        return config.getBoolean("website.lock-mode", false);
    }

    public String defaultTheme() {
        return config.getString("website.default-theme", "system");
    }

    public String loginCommand() {
        return config.getString("website.login-command", "webcode");
    }

    public boolean bindDevice() {
        return config.getBoolean("website.session.bind-device", true);
    }

    public boolean bindIp() {
        return config.getBoolean("website.session.bind-ip", false);
    }
}
