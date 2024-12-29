package me.bounser.nascraft.commands.sell;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.commands.Command;
import me.bounser.nascraft.config.Config;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

public class SellHandCommand extends Command {

    private HashMap<Player, ItemStack> players = new HashMap();

    public SellHandCommand() {
        super(
                "sellhand",
                new String[]{Config.getInstance().getCommandAlias("sellhand")},
                "Sell items directly to the market",
                "nascraft.sellhand"
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
            return;
        }

        Player player = ((Player) sender).getPlayer();

        if (!player.hasPermission("nascraft.sellhand")) {
            Lang.get().message((Player) sender, Message.NO_PERMISSION);
            return;
        }

        assert player != null;
        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (args.length == 0) {

            Item item = MarketManager.getInstance().getItem(handItem);

            if(item == null) {
                Lang.get().message(player, Message.SELLHAND_INVALID); return;
            }

            TextComponent component = (TextComponent) MiniMessage.miniMessage().deserialize(
                    Lang.get().message(Message.CLICK_TO_CONFIRM)
            );

            Component hoverText = MiniMessage.miniMessage().deserialize(
                    Lang.get().message(Message.SELLHAND_ESTIMATED_VALUE) +
                            Lang.get().message(Message.LIST_SEGMENT,
                                    Formatter.format(item.getCurrency(), item.getPrice().getProjectedCost(handItem.getAmount()*item.getMultiplier(), item.getPrice().getSellTaxMultiplier()) , Style.ROUND_BASIC),
                                    String.valueOf(handItem.getAmount()),
                                    item.getName())
            );

            component = component.hoverEvent(HoverEvent.showText(hoverText))
                    .clickEvent(ClickEvent.runCommand("/" + Config.getInstance().getCommandAlias("sellhand") + " confirm"));

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
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
