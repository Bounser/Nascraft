package me.bounser.nascraft.commands.market;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.inventorygui.BuySellMenu;
import me.bounser.nascraft.inventorygui.CategoryMenu;
import me.bounser.nascraft.inventorygui.MarketMenuManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.resources.Category;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MarketCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(sender instanceof Player) {

            Player player = (Player) sender;

            if (!player.hasPermission("nascraft.market") && Config.getInstance().getMarketPermissionRequirement()) {
                Lang.get().message(player, Message.NO_PERMISSION);
                return false;
            }

            if (args.length == 0 && player.hasPermission("nascraft.market.gui")) {
                MarketMenuManager.getInstance().openMenu(player);
                return false;
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("category") && player.hasPermission("nascraft.market.gui")) {

                Category category = MarketManager.getInstance().getCategoryFromIdentifier(args[1]);

                if (category == null) {
                    Lang.get().message(player, Message.MARKET_CMD_INVALID_CATEGORY);
                    return false;
                }

                MarketMenuManager.getInstance().setMenuOfPlayer(player, new CategoryMenu(player, category));
                return false;
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("item") && player.hasPermission("nascraft.market.gui")) {

                Item item = MarketManager.getInstance().getItem(args[1].toLowerCase());

                if (item == null) {
                    Lang.get().message(player, Message.MARKET_CMD_INVALID_ITEM);
                    return false;
                }

                MarketMenuManager.getInstance().setMenuOfPlayer(player, new BuySellMenu(player, item));
                return false;
            }

            if (args.length != 3) {
                Lang.get().message(player, Message.MARKET_CMD_INVALID_USE);
                return false;
            }

            int quantity;

            try {
                quantity = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                Lang.get().message(player, Message.MARKET_CMD_INVALID_QUANTITY);
                return false;
            }

            if (quantity > 64) {
                Lang.get().message(player, Message.MARKET_CMD_MAX_QUANTITY_REACHED);
                return false;
            }

            Item item = MarketManager.getInstance().getItem(args[1]);

            if (item == null) {
                Lang.get().message(player, Message.MARKET_CMD_INVALID_IDENTIFIER);
                return false;
            }

            switch (args[0].toLowerCase()){
                case "buy":
                    item.buy(quantity, player.getUniqueId(), true);
                    break;
                case "sell":
                    item.sell(quantity, player.getUniqueId(), true);
                    break;
                default:
                    Lang.get().message(player, Message.MARKET_CMD_INVALID_OPTION);
            }

        } else {
            if (args.length != 4) {
                Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Invalid use of command. (CONSOLE) /market <Buy/Sell> <Material> <Quantity> <Player>");
                return false;
            }

            Player player = Bukkit.getPlayer(args[3]);

            if (player == null) {
                Nascraft.getInstance().getLogger().info(ChatColor.RED + "Invalid player");
                return false;
            }
            Item item = MarketManager.getInstance().getItem(args[1]);
            switch (args[0]){
                case "buy":
                    item.buy(Integer.parseInt(args[2]), player.getUniqueId(), true);
                    break;
                case "sell":
                    item.sell(Integer.parseInt(args[2]), player.getUniqueId(), true);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Wrong option: buy / sell");
            }
        }
        return false;
    }

}
