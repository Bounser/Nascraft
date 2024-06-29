package me.bounser.nascraft.commands.sellwand;

import me.bounser.nascraft.sellwand.WandsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GiveSellWandTabCompleter implements TabCompleter {

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        return StringUtil.copyPartialMatches(args[0], WandsManager.getInstance().getWands().keySet(), new ArrayList<>());
    }

}
