package me.bounser.nascraft.commands.admin.nascraft;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.Command;
import me.bounser.nascraft.commands.admin.marketeditor.overview.MarketEditorManager;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class NascraftCommand extends Command {

    private final List<String> arguments = Arrays.asList("reload", "edit", "stop", "resume", "cpi", "save", "logs");

    private final List<String> tradesArguments = Arrays.asList("<player nick or uuid>", "<item>", "global");

    public NascraftCommand() {
        super(
                "nascraft",
                new String[]{Config.getInstance().getCommandAlias("nascraft")},
                "Admin command",
                "nascraft.admin"
                );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (sender instanceof Player && !sender.hasPermission("nascraft.admin")) {
            Lang.get().message((Player) sender, Message.NO_PERMISSION);
            return;
        }

        String syntaxError = ChatColor.DARK_PURPLE + "[NC] " +ChatColor.RED + "Wrong syntax. Available arguments: \nreload | edit | save | cpi | stop | resume | logs";

        if (args.length == 0) {
            sender.sendMessage(syntaxError);
            return;
        }

        switch(args[0].toLowerCase()){
            case "save":
                DatabaseManager.get().getDatabase().saveEverything();
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Data saved.");
                break;

            case "logs":

                if (args.length != 2) {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " +ChatColor.RED + "Wrong syntax. Available arguments for /nascraft logs: global, <item>, <player nick or uuid>");
                    return;
                }

                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " +ChatColor.RED + "That command can only be used in-game.");
                    return;
                }

                Player playerLog = (Player) sender;

                if (args[1].equalsIgnoreCase("global")) {

                    playerLog.setMetadata("NascraftLogInventory", new FixedMetadataValue(Nascraft.getInstance(),"global"));
                    playerLog.setMetadata("NascraftLogInventoryPage", new FixedMetadataValue(Nascraft.getInstance(), 0));
                    NascraftLogListener.createTradePage(playerLog, null, null);

                } else {

                    Item item = MarketManager.getInstance().getItem(args[1].toLowerCase());

                    if (item != null) {
                        playerLog.setMetadata("NascraftLogInventory", new FixedMetadataValue(Nascraft.getInstance(), "item-" + item.getIdentifier()));
                        playerLog.setMetadata("NascraftLogInventoryPage", new FixedMetadataValue(Nascraft.getInstance(), 0));
                        NascraftLogListener.createTradePage(playerLog, item, null);
                    } else {
                        Player player = Bukkit.getPlayer(args[1]);

                        if (player == null) {

                            if (isValidUUID(args[1])) {

                                playerLog.setMetadata("NascraftLogInventory", new FixedMetadataValue(Nascraft.getInstance(), "uuid-" + args[1]));
                                playerLog.setMetadata("NascraftLogInventoryPage", new FixedMetadataValue(Nascraft.getInstance(), 0));
                                NascraftLogListener.createTradePage(playerLog, null, UUID.fromString(args[1]));
                                break;
                            }

                            sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " +ChatColor.RED + "Argument not identified.");
                            return;
                        } else {

                            playerLog.setMetadata("NascraftLogInventory", new FixedMetadataValue(Nascraft.getInstance(), "uuid-" + player.getUniqueId()));
                            playerLog.setMetadata("NascraftLogInventoryPage", new FixedMetadataValue(Nascraft.getInstance(), 0));
                            NascraftLogListener.createTradePage(playerLog, null, player.getUniqueId());

                        }
                    }
                }
                break;

            case "cpi":
                sender.sendMessage(ChatColor.BLUE + "CPI: " + MarketManager.getInstance().getConsumerPriceIndex());
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

                Lang.get().reload();

                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Lang reloaded. Using: " + Config.getInstance().getSelectedLanguage());

                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Reloaded! " +
                        MarketManager.getInstance().getAllItems().size() + " items (" +
                        MarketManager.getInstance().getAllParentItems().size() + " parents and " + (MarketManager.getInstance().getAllItems().size() - MarketManager.getInstance().getAllParentItems().size()) +
                        " childs) within " + Config.getInstance().getCategories().size() + " categories.");

                break;

            case "edit":

                if (sender instanceof Player) { MarketEditorManager.getInstance().startEditing((Player) sender); }
                else Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
        }
    }

    public static boolean isValidUUID(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length > 1) {
            if (args[0].equalsIgnoreCase("logs")) {
                return StringUtil.copyPartialMatches(args[1], tradesArguments, new ArrayList<>());
            }
        }

        return StringUtil.copyPartialMatches(args[0], arguments, new ArrayList<>());
    }
}
