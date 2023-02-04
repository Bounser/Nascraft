package me.bounser.nascraft.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.Item;
import me.bounser.nascraft.market.MarketManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MarketCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED + "This command can only be used by players!");
        }

        if(args.length == 0) {
            sender.sendMessage(ChatColor.RED  + "Invalid use of command. /market buy/sell material quantity");
            return false;
        }
        switch (args[0]){
            case "buy":
                sender.sendMessage("Precio anterior: " + MarketManager.getInstance().getItem(args[1]).getPrice());
                MarketManager.getInstance().getItem(args[1]).buyItem(Integer.parseInt(args[2]));
                Player p = (Player) sender;
                p.getInventory().addItem(new ItemStack(Material.getMaterial(args[1].toUpperCase()), Integer.valueOf(args[2])));
                sender.sendMessage("You just bought " + args[1]);
                sender.sendMessage("Precio despues: " + MarketManager.getInstance().getItem(args[1]).getPrice());
                break;
            case "sell":
                MarketManager.getInstance().getItem(args[1]).sellItem(Integer.parseInt(args[2]));
                sender.sendMessage("You just sold " + args[1]);
                break;
            case "info":
                for(Item item : MarketManager.getInstance().getAllItems()) {

                    sender.sendMessage("Mat: " + item.getMaterial() + " price: " + item.getPrice() + " stock: " + item.getStock());

                }


        }


        return false;
    }
}
