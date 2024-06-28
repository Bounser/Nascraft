package me.bounser.nascraft.commands.sell;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class SellHandCommand implements CommandExecutor {

    private HashMap<Player, ItemStack> players = new HashMap();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
            return false;
        }

        Player player = ((Player) sender).getPlayer();

        if (!player.hasPermission("nascraft.sellhand")) {
            Lang.get().message((Player) sender, Message.NO_PERMISSION);
            return false;
        }

        assert player != null;
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (args.length == 0) {

            Item item = MarketManager.getInstance().getItem(handItem.getType().toString());

            if(item == null) {
                Lang.get().message(player, Message.SELLHAND_INVALID); return false;
            }

            if(!MarketManager.getInstance().isAValidItem(handItem)) {
                Lang.get().message(player, Message.SELLHAND_INVALID); return false;
            }

            TextComponent component = (TextComponent) MiniMessage.miniMessage().deserialize(
                    Lang.get().message(Message.CLICK_TO_CONFIRM)
            );

            Component hoverText = MiniMessage.miniMessage().deserialize(
                    Lang.get().message(Message.SELLHAND_ESTIMATED_VALUE) +
                            Lang.get().message(Message.LIST_SEGMENT,
                                    Formatter.format(item.getPrice().getProjectedCost(handItem.getAmount(), item.getPrice().getSellTaxMultiplier()) , Style.ROUND_BASIC),
                                    String.valueOf(handItem.getAmount()),
                                    item.getName())
            );

            component = component.hoverEvent(HoverEvent.showText(hoverText))
                    .clickEvent(ClickEvent.runCommand("/nsellhand confirm"));

            Lang.get().getAudience().player(player).sendMessage(component);

            players.put(player, handItem);

        } else if (args[0].equalsIgnoreCase("confirm")) {

            if(player.getInventory().getItemInMainHand().equals(players.get(player))) {
                Item item = MarketManager.getInstance().getItem(player.getInventory().getItemInMainHand());

                item.sell(handItem.getAmount(), player.getUniqueId(), true);
            } else {
                Lang.get().message(player, Message.SELLHAND_ERROR_HAND);
            }
        }
        return false;
    }

}
