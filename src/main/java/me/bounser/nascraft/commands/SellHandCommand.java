package me.bounser.nascraft.commands;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import me.bounser.nascraft.market.unit.Item;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
        ItemStack handItems = player.getInventory().getItemInMainHand();

        if (args.length == 0) {

            Item item = MarketManager.getInstance().getItem(handItems.getType().toString());

            if(item == null) {
                Lang.get().message(player, Message.SELLHAND_INVALID); return false;
            }

            ItemMeta meta = handItems.getItemMeta();

            if(meta.hasDisplayName() || meta.hasEnchants() || meta.hasLore() || meta.hasAttributeModifiers() || meta.hasCustomModelData()) {
                Lang.get().message(player, Message.SELLHAND_INVALID); return false;
            }

            String message = "<hover:show_text:" +
                    "\"" + Lang.get().message(Message.SELLHAND_ESTIMATED_VALUE) +
                    Lang.get().message(Message.LIST_SEGMENT,
                                       Formatter.format(handItems.getAmount()*item.getPrice().getSellPrice(),
                                                        Style.ROUND_BASIC),
                                                        String.valueOf(handItems.getAmount()),
                                                        item.getName()) + "\">" +
                    "<click:run_command:\"/nsellhand confirm\">" +
                    Lang.get().message(Message.CLICK_TO_CONFIRM);

            Audience audience = (Audience) player;
            audience.sendMessage(MiniMessage.miniMessage().deserialize(message));

            players.put(player, handItems);

        } else if (args[0].equalsIgnoreCase("confirm")) {

            if(player.getInventory().getItemInMainHand().equals(players.get(player))) {
                Item item = MarketManager.getInstance().getItem(player.getInventory().getItemInMainHand().getType().toString());

                item.sellItem(handItems.getAmount(), player.getUniqueId(), true);
            } else {
                Lang.get().message(player, Message.SELLHAND_ERROR_HAND);
            }
        }
        return false;
    }

}
