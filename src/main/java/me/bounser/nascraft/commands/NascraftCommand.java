package me.bounser.nascraft.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.MarketStatus;
import me.bounser.nascraft.market.PricesManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NascraftCommand implements CommandExecutor {

    private Nascraft main;
    public  NascraftCommand(Nascraft main){
        this.main = main;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(!sender.hasPermission("nascraft.admin") && sender instanceof Player) {
            sender.sendMessage(ChatColor.RED + "You are not allowed to use this command!");
            return false;
        }
        switch(args[0]){

            case "force":
                switch (args[1]){
                    case "crash": PricesManager.getInstance().setMarketStatus(MarketStatus.CRASH); break;
                    case "bullrun": PricesManager.getInstance().setMarketStatus(MarketStatus.BULLRUN); break;
                    case "bear": PricesManager.getInstance().setMarketStatus(MarketStatus.BEAR1); break;
                    case "bull": PricesManager.getInstance().setMarketStatus(MarketStatus.BULL1); break;
                }


            default: if(sender instanceof Player) sender.sendMessage("Argument not recognized.");
        }



        return false;
    }

}
