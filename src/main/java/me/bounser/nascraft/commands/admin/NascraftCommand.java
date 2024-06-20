package me.bounser.nascraft.commands.admin;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.admin.marketeditor.overview.MarketEditorManager;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.MarketManager;
import me.leoko.advancedgui.manager.GuiWallManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public class NascraftCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player && !sender.hasPermission("nascraft.admin")) {
            Lang.get().message((Player) sender, Message.NO_PERMISSION);
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " +ChatColor.RED + "Wrong syntax. Available arguments: reload | editmarket | save | info | locate | stop | resume");
            return false;
        }

        switch(args[0]){
            case "save":
                DatabaseManager.get().getDatabase().saveEverything();
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Data saved.");
                break;
            case "info":
                for (Item item : MarketManager.getInstance().getAllItems())
                    sender.sendMessage(ChatColor.GRAY + "Mat: " + item.getIdentifier() + " value: " + item.getPrice().getValue() + " stock: " + item.getPrice().getStock());
                break;
            case "locate":
                sender.sendMessage(String.valueOf(GuiWallManager.getInstance().getActiveInstances((Player) sender).get(0).getInteraction((Player) sender).getComponentTree().locate(args[1]).getState((Player) sender, GuiWallManager.getInstance().getActiveInstances((Player) sender).get(0).getCursor((Player) sender))));
                break;

            case "stop":
                if(MarketManager.getInstance().getActive()) {
                    MarketManager.getInstance().stop();
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Shop stopped. Resume it with /nascraft resume.");
                } else {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Shop is already stopped!");
                }
                break;

            case "resume":
                if(!MarketManager.getInstance().getActive()) {
                    MarketManager.getInstance().resume();
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Shop resumed.");
                } else {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Shop is already active!");
                }
                break;

            case "reload":
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Reloading...");

                Config.getInstance().reload();

                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Reloaded! " + MarketManager.getInstance().getAllItems().size() + " items and " + Config.getInstance().getCategories().size() + " categories.");
                break;

            case "editmarket":

                if (sender instanceof Player) { MarketEditorManager.getInstance().startEditing((Player) sender); }
                else Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");

                break;

            case "lasttrades":

                int offset;

                if (args.length > 2) {

                    Player player = Bukkit.getPlayer(args[1]);

                    if (player == null) {
                        if (sender instanceof Player)
                            sender.sendMessage(ChatColor.RED + "Player not found.");
                        else
                            Nascraft.getInstance().getLogger().info(ChatColor.RED + "Player not found.");
                        return false;
                    }

                    try {
                        offset = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        offset = 0;
                    }

                    String report = ChatColor.GRAY + "\nLast trades of user " + player.getName() +": (Page " + offset + ")\n\n";

                    for (Trade trade : DatabaseManager.get().getDatabase().retrieveTrades(offset*10-1)) {
                        report += ChatColor.GRAY + getFormatedDate(trade.getDate());
                        report += trade.isBuy() ? ChatColor.GREEN + " BOUGHT: " : ChatColor.RED + " SOLD: ";
                        report += trade.getAmount() + " x " + trade.getTradable().getIdentifier();
                        report += " (" + Formatter.format(trade.getValue(), Style.ROUND_BASIC) + ")\n";
                    }

                    if (sender instanceof Player)
                        sender.sendMessage(report);
                    else
                        Nascraft.getInstance().getLogger().info(report);

                    return false;

                } else if (args.length > 1) {
                    try {
                        offset = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        offset = 0;
                    }
                } else {
                    offset = 0;
                }

                String report = ChatColor.GRAY + "\nLast trades: (Page " + offset + ")\n\n";

                for (Trade trade : DatabaseManager.get().getDatabase().retrieveTrades(offset*10-1)) {
                    report += ChatColor.GRAY + getFormatedDate(trade.getDate());
                    report += " " + trade.getUuid().toString();
                    report += trade.isBuy() ? ChatColor.GREEN + "BOUGHT: " : ChatColor.RED + "SOLD: ";
                    report += trade.getAmount() + " x " + trade.getTradable().getIdentifier();
                    report += " (" + Formatter.format(trade.getValue(), Style.ROUND_BASIC) + ")\n";
                }

                if (sender instanceof Player)
                    sender.sendMessage(report);
                else
                    Nascraft.getInstance().getLogger().info(report);

                break;

            default: sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Argument not recognized. Available arguments: reload | editmarket | save | info | locate | stop | resume");
        }
        return false;
    }

    private String getFormatedDate(LocalDateTime date) {

        String minute = String.valueOf(date.getMinute()).length() == 1 ? "0" + date.getMinute() : String.valueOf(date.getMinute());

        return date.getDayOfMonth() + "/" + date.getMonthValue() + "/" + date.getYear() + " " + date.getHour() + ":" + minute;
    }
}
