package me.bounser.nascraft.commands;

import me.bounser.nascraft.Nascraft;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class NascraftCommand implements CommandExecutor {

    private Nascraft main;
    public  NascraftCommand(Nascraft main){
        this.main = main;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {





        return false;
    }

}
