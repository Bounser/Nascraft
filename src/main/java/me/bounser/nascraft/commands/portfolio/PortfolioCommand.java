package me.bounser.nascraft.commands.portfolio;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.Command;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.inventorygui.Portfolio.PortfolioInventory;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

public class PortfolioCommand extends Command {

    public PortfolioCommand() {
        super(
                "portfolio",
                new String[]{Config.getInstance().getCommandAlias("portfolio")},
                "Nascraft portfolio",
                "nascraft.portfolio"
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) return;

        Player player = (Player) sender;

        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_TITLE));

        Inventory inventory = Bukkit.createInventory(player, 45, BukkitComponentSerializer.legacy().serialize(title));
        player.openInventory(inventory);
        player.setMetadata("NascraftPortfolio", new FixedMetadataValue(Nascraft.getInstance(),true));

        PortfolioInventory.getInstance().updatePortfolioInventory(player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

}
