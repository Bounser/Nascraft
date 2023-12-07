package me.bounser.nascraft.commands.sellall;

import me.bounser.nascraft.market.managers.MarketManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SellAllTabCompleter implements TabCompleter {

    private final List<String> materials = new ArrayList<>();

    public SellAllTabCompleter() {
        MarketManager.getInstance().getAllMaterials().forEach(material -> materials.add(material.toString().toLowerCase()));
        materials.add("everything");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        return StringUtil.copyPartialMatches(args[0], materials, new ArrayList<>());
    }

}
