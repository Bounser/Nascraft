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

    private final YamlConfiguration lang;

    private final MiniMessage miniMessage;
    private final BukkitAudiences audience;

    private static Lang instance;

    public static Lang get() { return instance == null ? instance = new Lang() : instance; }

    private Lang() {

        Nascraft main = Nascraft.getInstance();

        saveResourceIfNotExists("langs/en_US.yml");
        saveResourceIfNotExists("langs/es_ES.yml");
        // saveResourceIfNotExists("langs/ca_ES.yml");

        File language = new File(main.getDataFolder().getPath() + "/langs/" + Config.getInstance().getSelectedLanguage() + ".yml");

        if (!language.exists()) {
            main.getLogger().severe("Lang file selected does not exist!");
            main.getPluginLoader().disablePlugin(main);
        }

        lang = YamlConfiguration.loadConfiguration(language);

        this.audience = Nascraft.getInstance().adventure();
        this.miniMessage = MiniMessage.miniMessage();
        Formatter.setSeparator(Separator.valueOf(message(Message.SEPARATOR).toUpperCase()));
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File resourceFile = new File(Nascraft.getInstance().getDataFolder().getPath() + "/" + resourcePath);
        if (!resourceFile.exists()) Nascraft.getInstance().saveResource(resourcePath, false);
    }

    public void message(Player player, Message lang) {
        audience.player(player).sendMessage(miniMessage.deserialize(this.lang.getString(lang.name().toLowerCase())));
    }

    public String message(Message lang) { return this.lang.getString(lang.name().toLowerCase()).replace("&", "§"); }

    public void message(Player player, Message lang, String worth, String amount, String material) {

        audience.player(player).sendMessage(miniMessage.deserialize(this.lang.getString(lang.name().toLowerCase())
                .replace("[WORTH]", worth)
                .replace("[AMOUNT]", amount)
                .replace("[MATERIAL]", material)));
    }

    public String message(Message lang, String worth, String amount, String material) {
        return this.lang.getString(lang.name().toLowerCase())
                .replace("&", "§")
                .replace("[WORTH]", worth)
                .replace("[AMOUNT]", amount)
                .replace("[MATERIAL]", material);
    }

    public boolean after() { return lang.getBoolean("after"); }

}
