package me.bounser.nascraft.updatechecker;

import me.bounser.nascraft.Nascraft;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

    private final Nascraft instance;
    private final int resourceId;

    public UpdateChecker(Nascraft instance, int resourceId) {
        this.instance = instance;
        this.resourceId = resourceId;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            try (InputStream is = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId + "/~").openStream(); Scanner scann = new Scanner(is)) {
                if (scann.hasNext()) {
                    consumer.accept(scann.next());
                }
            } catch (IOException e) {
                instance.getLogger().info("Unable to check for updates: " + e.getMessage());
            }
        });
    }

}
