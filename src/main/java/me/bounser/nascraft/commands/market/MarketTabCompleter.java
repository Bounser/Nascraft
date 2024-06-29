package me.bounser.nascraft.commands.market;

import me.bounser.nascraft.market.MarketManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MarketTabCompleter implements TabCompleter {
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        switch (args.length) {
            case 1:
                return StringUtil.copyPartialMatches(args[0], Arrays.asList("buy", "sell"), new ArrayList<>());
            case 2:
                return StringUtil.copyPartialMatches(args[1], MarketManager.getInstance().getAllItemsAndChildsIdentifiers(), new ArrayList<>());
            case 3:
                return Collections.singletonList("quantity");
        }

        return null;
    }
}
