package me.bounser.nascraft.commands.sell.sellall;

import me.bounser.nascraft.market.MarketManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SellAllTabCompleter implements TabCompleter {

    private final List<String> options = new ArrayList<>();

    public SellAllTabCompleter() {
        MarketManager.getInstance().getAllItems().forEach(item -> options.add(item.getIdentifier()));
        options.add("everything");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        return StringUtil.copyPartialMatches(args[0], options, new ArrayList<>());
    }

}
