package me.bounser.nascraft.config.lang;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Separator;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;

public class Lang {

    private YamlConfiguration lang;

    private final MiniMessage miniMessage;
    private final BukkitAudiences audience;

    private static Lang instance;

    public static Lang get() { return instance == null ? instance = new Lang() : instance; }

    public BukkitAudiences getAudience() { return audience; }


    private Lang() {

        saveResourceIfNotExists("langs/en_US.yml");
        saveResourceIfNotExists("langs/es_ES.yml");
        saveResourceIfNotExists("langs/it_IT.yml");
        saveResourceIfNotExists("langs/de_DE.yml");
        saveResourceIfNotExists("langs/pt_BR.yml");

        File language = new File(Nascraft.getInstance().getDataFolder().getPath() + "/langs/" + Config.getInstance().getSelectedLanguage() + ".yml");

        if (!language.exists()) {
            Nascraft.getInstance().getLogger().severe("Lang file selected does not exist!");
            Nascraft.getInstance().getPluginLoader().disablePlugin(Nascraft.getInstance());
        }

        lang = YamlConfiguration.loadConfiguration(language);

        this.audience = Nascraft.getInstance().adventure();
        this.miniMessage = MiniMessage.miniMessage();
        Formatter.setSeparator(Separator.valueOf(message(Message.SEPARATOR).toUpperCase()));
    }

    public void reload() {

        File language = new File(Nascraft.getInstance().getDataFolder().getPath() + "/langs/" + Config.getInstance().getSelectedLanguage() + ".yml");

        if (!language.exists()) {
            Nascraft.getInstance().getLogger().severe("Lang file selected does not exist!");
            Nascraft.getInstance().getPluginLoader().disablePlugin(Nascraft.getInstance());
        }

        lang = YamlConfiguration.loadConfiguration(language);
        Formatter.setSeparator(Separator.valueOf(message(Message.SEPARATOR).toUpperCase()));
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File resourceFile = new File(Nascraft.getInstance().getDataFolder().getPath() + "/" + resourcePath);
        if (!resourceFile.exists()) Nascraft.getInstance().saveResource(resourcePath, false);
    }

    public void message(Player player, Message lang) {
        audience.player(player).sendMessage(miniMessage.deserialize(this.lang.getString(lang.name().toLowerCase())));
    }

    public void message(Player player, String msg) {
        audience.player(player).sendMessage(miniMessage.deserialize(msg));
    }

    public String message(Message lang) {
        if (!this.lang.contains(lang.name().toLowerCase())) {
            Nascraft.getInstance().getLogger().warning("Lang section not found: " + lang.name().toLowerCase());
            return "Lang section not found: " + lang.name().toLowerCase();
        }
        return this.lang.getString(lang.name().toLowerCase()).replace("&", "§"); }

    public void message(Player player, Message lang, String worth, String amount, String name) {

        audience.player(player).sendMessage(miniMessage.deserialize(this.lang.getString(lang.name().toLowerCase())
                .replace("[WORTH]", worth)
                .replace("[AMOUNT]", amount)
                .replace("[NAME]", name)));
    }

    public void message(Player player, Message lang, String placeholder, String replacement) {

        audience.player(player).sendMessage(miniMessage.deserialize(this.lang.getString(lang.name().toLowerCase())
                .replace(placeholder, replacement)));
    }

    public void message(Player player, Message lang, String placeholder1, String replacement1, String placeholder2, String replacement2, String placeholder3, String replacement3) {

        audience.player(player).sendMessage(miniMessage.deserialize(this.lang.getString(lang.name().toLowerCase())
                .replace(placeholder1, replacement1)
                .replace(placeholder2, replacement2)
                .replace(placeholder3, replacement3)));
    }

    public String message(Message lang, String worth, String amount, String name) {
        return this.lang.getString(lang.name().toLowerCase())
                .replace("&", "§")
                .replace("[WORTH]", worth)
                .replace("[AMOUNT]", amount)
                .replace("[NAME]", name);
    }

    public String message(Message lang, String placeholder, String replacement) {
        return this.lang.getString(lang.name().toLowerCase())
                .replace("&", "§")
                .replace(placeholder, replacement);
    }

    public String message(Message lang, String placeholder1, String replacement1, String placeholder2, String replacement2, String placeholder3, String replacement3) {
        return this.lang.getString(lang.name().toLowerCase())
                .replace("&", "§")
                .replace(placeholder1, replacement1)
                .replace(placeholder2, replacement2)
                .replace(placeholder3, replacement3);
    }
}
