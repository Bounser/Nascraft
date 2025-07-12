package me.bounser.nascraft.commands.admin.nascraft;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.DatabaseType;
import me.bounser.nascraft.config.Config;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to migrate data from SQLite to Redis
 */
public class MigrateDataCommand implements CommandExecutor {

    private final Nascraft plugin;
    
    public MigrateDataCommand(Nascraft plugin) {
        this.plugin = plugin;
        plugin.getCommand("migrate").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only admins can use this command
        if (!sender.hasPermission("nascraft.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        // Check if migration is already in progress
        if (plugin.getDataMigrationManager().isMigrationInProgress()) {
            sender.sendMessage(ChatColor.RED + "A data migration is already in progress. Please wait for it to finish.");
            return true;
        }
        
        // Check current database type
        if (Config.getInstance().getDatabaseType() != DatabaseType.SQLITE) {
            sender.sendMessage(ChatColor.RED + "This command can only be used when the primary database is SQLite.");
            sender.sendMessage(ChatColor.RED + "Current database type: " + Config.getInstance().getDatabaseType());
            return true;
        }
        
        // Check if Redis is configured
        if (Config.getInstance().getRedisHost() == null || Config.getInstance().getRedisHost().isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Redis is not properly configured. Please check your config.yml file.");
            return true;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Starting data migration from SQLite to Redis...");
        sender.sendMessage(ChatColor.YELLOW + "This may take some time depending on the size of your database.");
        
        // Start migration process
        plugin.getDataMigrationManager().migrateFromSQLiteToRedis()
            .thenAccept(success -> {
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "Data migration completed successfully!");
                    sender.sendMessage(ChatColor.GREEN + "To use Redis as your primary database, change the database type in config.yml to REDIS.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Data migration failed. Check the console for details.");
                }
            });
        
        return true;
    }
} 