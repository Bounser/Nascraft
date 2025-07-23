package me.bounser.nascraft.commands.admin.nascraft;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.Command;
import me.bounser.nascraft.commands.admin.marketeditor.overview.MarketEditorManager;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.redis.Redis;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.managers.DebtManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.managers.currencies.Currency;
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
import java.util.Set;
import java.util.UUID;

public class NascraftCommand extends Command {

    private final List<String> arguments = Arrays.asList("reload", "edit", "stop", "resume", "info", "save", "sync", "logs", "forgivedebt", "servers", "noise");

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

        String syntaxError = ChatColor.DARK_PURPLE + "[NC] " +ChatColor.RED + "Wrong syntax. Available arguments: \n";

        for (String argument : arguments.subList(0, arguments.size()-2))
            syntaxError += argument + " | ";

        syntaxError += arguments.get(arguments.size() - 1);

        if (args.length == 0) {
            sender.sendMessage(syntaxError);
            return;
        }

        switch(args[0].toLowerCase()){

            case "save":
                DatabaseManager.get().getDatabase().saveEverything();
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Data saved.");
                break;
            
            case "sync":
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Syncing items to Redis...");
                
                if (DatabaseManager.get().getDatabase().getClass().getSimpleName().equals("Redis")) {
                    try {
                        Redis redisDB = (Redis) DatabaseManager.get().getDatabase();
                        redisDB.syncAllItemsToRedis();
                        sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GREEN + "Items synced to Redis successfully!");
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Error syncing items to Redis: " + e.getMessage());
                    }
                } else {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Redis is not the active database. Current database: " + DatabaseManager.get().getDatabase().getClass().getSimpleName());
                }
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

            case "info":

                Currency currency = CurrenciesManager.getInstance().getDefaultCurrency();

                String msg = "\n<color:#9985ff>● All time inflation: <color:#57ffa0>" + Formatter.roundToDecimals(MarketManager.getInstance().getConsumerPriceIndex()-100, 3) + "%</color>\n\n"
                        + "● All outstanding debt: " + Formatter.format(currency, DatabaseManager.get().getDatabase().getAllOutstandingDebt(), Style.ROUND_BASIC) + " (" + DatabaseManager.get().getDatabase().getUUIDAndDebt().keySet().size()  + " debtors)\n"
                        + "● All interests collected: " + Formatter.format(currency, DatabaseManager.get().getDatabase().getAllInterestsPaid(), Style.ROUND_BASIC) + "\n\n"
                        + "● All taxes collected: " + Formatter.format(currency, Math.abs(DatabaseManager.get().getDatabase().getAllTaxesCollected()), Style.ROUND_BASIC) + "</color>\n";

                Lang.get().message((Player) sender, msg);

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
                break;

            case "forgivedebt":

                if (args.length != 3) {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED  + "Wrong usage. /nascraft forgivedebt <player name> <all/amount>");
                    break;
                }

                Player player = Bukkit.getPlayer(args[1]);

                if (player == null) {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED  + "Player not found");
                    break;
                }

                if (args[2] == null || args[2].isEmpty()) {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED  + "Invalid amount");
                    break;
                }

                double debt = 0;
                double playerDebt = DebtManager.getInstance().getDebtOfPlayer(player.getUniqueId());

                try {
                    debt = Double.parseDouble(args[2]);
                } catch (NumberFormatException e) {
                    if (args[2].equalsIgnoreCase("all")) {
                        debt = playerDebt;
                    } else {
                        sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED  + "Invalid amount");
                    }
                }

                DebtManager.getInstance().decreaseDebt(player.getUniqueId(), debt);
                String msgDebt = "\n<color:#9985ff>You have forgiven: " + Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), debt, Style.ROUND_BASIC) + " of debt for the player <b>" + player.getName() +"</b>.\n"
                        + "The player has now a debt of " + Formatter.format(CurrenciesManager.getInstance().getDefaultCurrency(), DebtManager.getInstance().getDebtOfPlayer(player.getUniqueId()), Style.ROUND_BASIC) + "\n";

                Lang.get().message((Player) sender, msgDebt);

                break;

            case "migrate":
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Migration command is handled separately. Use /migrate command directly.");
                break;
                
            case "servers":
                if (sender instanceof Player) {
                    showDistributedSyncStatus((Player) sender);
                } else {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "This command can only be used by players.");
                }
                break;

            case "noise":
                handleNoiseCommand(sender, args);
                break;

            default:
                sender.sendMessage(syntaxError);
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
            if (args[0].equalsIgnoreCase("logs"))
                return StringUtil.copyPartialMatches(args[1], tradesArguments, new ArrayList<>());

            if (args[0].equalsIgnoreCase("forgivedebt")) {

                if (args.length == 3) {

                    Player player = Bukkit.getPlayer(args[1]);

                    if (player == null) return Arrays.asList("Invalid player");

                    return Arrays.asList("all", String.valueOf(Formatter.roundToDecimals(DebtManager.getInstance().getDebtOfPlayer(player.getUniqueId()), CurrenciesManager.getInstance().getDefaultCurrency().getDecimalPrecission())));

                } else {

                    List<String> playerNames = new ArrayList<>();

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        playerNames.add(player.getName());
                    }

                    return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
                }
            }
        }

        return StringUtil.copyPartialMatches(args[0], arguments, new ArrayList<>());
    }

    private void showDistributedSyncStatus(Player player) {
        if (!DatabaseManager.get().getDatabase().getClass().getSimpleName().equals("Redis")) {
            player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Distributed sync is only available with Redis database.");
            player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Current database: " + DatabaseManager.get().getDatabase().getClass().getSimpleName());
            return;
        }
        
        try {
            Redis redisDB = (Redis) DatabaseManager.get().getDatabase();
            
            player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "=== Distributed Market Sync Status ===");
            
            // Check if distributed sync is enabled
            if (redisDB.isDistributedSyncEnabled()) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GREEN + "✓ Distributed sync is ENABLED");
                
                Set<String> activeServers = redisDB.getActiveServers();
                
                if (activeServers.isEmpty()) {
                    player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.YELLOW + "⚠ No other servers detected");
                } else {
                    player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Active servers (" + activeServers.size() + "):");
                    for (String serverId : activeServers) {
                        player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "  • " + ChatColor.WHITE + serverId);
                    }
                }
                
                String currentServerId = redisDB.getDistributedSync().getServerId();
                player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Current server ID: " + ChatColor.WHITE + currentServerId);
                
                showNoiseMasterStatus(player, redisDB);
                
            } else {
                player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "✗ Distributed sync is DISABLED");
                player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Enable it in config.yml under database.redis.distributed-sync.enabled");
            }
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Error checking distributed sync status: " + e.getMessage());
        }
    }
    
    private void showNoiseMasterStatus(Player player, Redis redisDB) {
        try {
            if (Config.getInstance().getNoiseMasterEnabled()) {
                player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GREEN + "✓ Noise master system is ENABLED");
                
                String currentNoiseMaster = redisDB.getDistributedSync().getCurrentNoiseMaster();
                boolean isNoiseMaster = redisDB.getDistributedSync().isNoiseMaster();
                
                if (currentNoiseMaster != null) {
                    player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Current noise master: " + ChatColor.WHITE + currentNoiseMaster);
                    if (isNoiseMaster) {
                        player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GREEN + "✓ This server is the NOISE MASTER");
                    } else {
                        player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "This server is a noise follower");
                    }
                } else {
                    player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.YELLOW + "⚠ No noise master currently active");
                }
                
                player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Use " + ChatColor.WHITE + "/nascraft noise" + ChatColor.GRAY + " for noise master commands");
            } else {
                player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.YELLOW + "⚠ Noise master system is DISABLED");
                player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "All servers apply noise independently");
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Error checking noise master status: " + e.getMessage());
        }
    }
    
    private void handleNoiseCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && !sender.hasPermission("nascraft.admin")) {
            Lang.get().message((Player) sender, Message.NO_PERMISSION);
            return;
        }
        
        if (!DatabaseManager.get().getDatabase().getClass().getSimpleName().equals("Redis")) {
            sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Noise master commands are only available with Redis database.");
            return;
        }
        
        Redis redisDB = (Redis) DatabaseManager.get().getDatabase();
        
        if (args.length == 1) {
            showNoiseCommandHelp(sender);
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "status":
                showNoiseMasterFullStatus(sender, redisDB);
                break;
                
            case "claim":
                claimNoiseMaster(sender, redisDB);
                break;
                
            case "set":
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Usage: /nascraft noise set <server-id>");
                    return;
                }
                setNoiseMaster(sender, redisDB, args[2]);
                break;
                
            default:
                showNoiseCommandHelp(sender);
        }
    }
    
    private void showNoiseCommandHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "=== Noise Master Commands ===");
        sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "/nascraft noise status - Show detailed noise master status");
        sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "/nascraft noise claim - Claim noise master role for this server");
        sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "/nascraft noise set <server-id> - Set specific server as noise master");
        sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.YELLOW + "Note: Only one server should apply noise to prevent price loops");
    }
    
    private void showNoiseMasterFullStatus(CommandSender sender, Redis redisDB) {
        try {
            sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "=== Noise Master Status ===");
            
            if (!Config.getInstance().getNoiseMasterEnabled()) {
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.YELLOW + "⚠ Noise master system is DISABLED");
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "All servers apply noise independently");
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Enable in config.yml: database.redis.distributed-sync.noise-master.enabled");
                return;
            }
            
            sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GREEN + "✓ Noise master system is ENABLED");
            
            String currentNoiseMaster = redisDB.getDistributedSync().getCurrentNoiseMaster();
            boolean isNoiseMaster = redisDB.getDistributedSync().isNoiseMaster();
            String thisServerId = redisDB.getDistributedSync().getServerId();
            
            sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "This server ID: " + ChatColor.WHITE + thisServerId);
            
            if (currentNoiseMaster != null) {
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Current noise master: " + ChatColor.WHITE + currentNoiseMaster);
                if (isNoiseMaster) {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GREEN + "✓ This server is the NOISE MASTER");
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "This server applies noise to all items");
                } else {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "This server is a noise follower");
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "This server receives noise changes from master");
                }
            } else {
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.YELLOW + "⚠ No noise master currently active");
                if (Config.getInstance().getNoiseMasterAutoElect()) {
                    sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Auto-elect is enabled - first server will become master");
                }
            }
            
            Set<String> activeServers = redisDB.getActiveServers();
            sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Active servers (" + activeServers.size() + "):");
            for (String serverId : activeServers) {
                boolean isMaster = serverId.equals(currentNoiseMaster);
                String prefix = isMaster ? ChatColor.GREEN + "★ " : ChatColor.GRAY + "  • ";
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + prefix + ChatColor.WHITE + serverId);
            }
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Error checking noise master status: " + e.getMessage());
        }
    }
    
    private void claimNoiseMaster(CommandSender sender, Redis redisDB) {
        try {
            if (!Config.getInstance().getNoiseMasterEnabled()) {
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Noise master system is disabled in config.yml");
                return;
            }
            
            boolean success = redisDB.getDistributedSync().setNoiseMaster(redisDB.getDistributedSync().getServerId());
            
            if (success) {
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GREEN + "✓ Successfully claimed noise master role!");
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "This server will now apply noise to all items");
            } else {
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Failed to claim noise master role");
            }
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Error claiming noise master role: " + e.getMessage());
        }
    }
    
    private void setNoiseMaster(CommandSender sender, Redis redisDB, String serverId) {
        try {
            if (!Config.getInstance().getNoiseMasterEnabled()) {
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Noise master system is disabled in config.yml");
                return;
            }
            
            Set<String> activeServers = redisDB.getActiveServers();
            if (!activeServers.contains(serverId)) {
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Server '" + serverId + "' is not currently active");
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "Active servers: " + String.join(", ", activeServers));
                return;
            }
            
            boolean success = redisDB.getDistributedSync().setNoiseMaster(serverId);
            
            if (success) {
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GREEN + "✓ Successfully set '" + serverId + "' as noise master!");
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.GRAY + "That server will now apply noise to all items");
            } else {
                sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Failed to set noise master");
            }
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.DARK_PURPLE + "[NC] " + ChatColor.RED + "Error setting noise master: " + e.getMessage());
        }
    }
}
