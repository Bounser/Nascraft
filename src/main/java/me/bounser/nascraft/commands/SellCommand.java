package me.bounser.nascraft.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.RoundUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class SellCommand implements CommandExecutor {

    HashMap<Player, ItemStack> players = new HashMap();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player) {

            Player player = ((Player) sender).getPlayer();

            if (!player.hasPermission("nascraft.sell")) {
                player.sendMessage(ChatColor.RED + "Permission required.");
                return false;
            }

            assert player != null;
            ItemStack handItems = player.getInventory().getItemInMainHand();

            if (args.length == 0) {

                Item item = MarketManager.getInstance().getItem(handItems.getType().toString());

                if(item != null) {

                    String text = ChatColor.GRAY + "Estimated value: \n\n> " + ChatColor.GOLD + handItems.getType() +" x "+ handItems.getAmount() +" = "+ RoundUtils.round(handItems.getAmount()*item.getPrice().getSellPrice()) + Config.getInstance().getCurrency() + "\n  ";

                    TextComponent message = new TextComponent("\n " + ChatColor.GOLD + "[ Click here to confirm ]\n");
                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sell confirm"));
                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(text).create()));

                    player.spigot().sendMessage(message);
                    players.put(player, handItems);

                } else {
                    player.sendMessage(ChatColor.RED + "Hold a valid item!");
                }

            } else if (args[0].equalsIgnoreCase("confirm")) {

                if(player.getInventory().getItemInMainHand().equals(players.get(player))) {
                    Item item = MarketManager.getInstance().getItem(player.getInventory().getItemInMainHand().getType().toString());

                    item.sellItem(handItems.getAmount(), player, item.getMaterial(), 1);
                } else {
                    Bukkit.broadcastMessage(ChatColor.RED + "Error. Don't change the items in your hand when confirming");
                }
            }

        } else {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
        }
        return false;
    }

}
