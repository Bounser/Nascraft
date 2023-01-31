package me.bounser.nascraft.commands;

import me.bounser.nascraft.Nascraft;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MarketCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED + "This command can only be used by players!");
        }

        if(args.length < 1) {
            sender.sendMessage(ChatColor.RED  + "Invalid use of command.");
            return false;
        }
        switch (args[0]){

            case "buy":
                if(args[1].equals("1")) { }

            case "sell":

            case "sellall":


        }


        return false;
    }
}
