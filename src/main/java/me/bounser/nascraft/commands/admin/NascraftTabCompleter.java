package me.bounser.nascraft.commands.admin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NascraftTabCompleter implements TabCompleter {

    private final List<String> arguments = Arrays.asList("reload", "editmarket", "stop", "resume", "locate", "list", "cpi", "save", "lasttrades");

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        return StringUtil.copyPartialMatches(args[0], arguments, new ArrayList<>());
    }

}
