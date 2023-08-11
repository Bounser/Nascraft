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
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class SellHandCommand implements CommandExecutor {

    private HashMap<Player, ItemStack> players = new HashMap();

    private Config lang = Config.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Command not available through console.");
        }

        Player player = ((Player) sender).getPlayer();

        if (!player.hasPermission("nascraft.sellhand")) {
            player.sendMessage(lang.getPermissionText());
            return false;
        }

        assert player != null;
        ItemStack handItems = player.getInventory().getItemInMainHand();

        if (args.length == 0) {

            Item item = MarketManager.getInstance().getItem(handItems.getType().toString());

            if(item == null) {
                player.sendMessage(lang.getSellHandInvalidItem());
                return false;
            }

            ItemMeta meta = handItems.getItemMeta();

            if(meta.hasDisplayName() || meta.hasEnchants() || meta.hasLore() || meta.hasAttributeModifiers() || meta.hasCustomModelData()) {
                player.sendMessage(lang.getSellHandInvalidItem());
                return false;
            }

            String text = lang.getSellHandEstimatedValue() + ChatColor.GRAY + "\n> " + ChatColor.GOLD + item.getName() +" x "+ handItems.getAmount() +" = "+ RoundUtils.round(handItems.getAmount()*item.getPrice().getSellPrice()) + Config.getInstance().getCurrency() + "\n  ";

            TextComponent message = new TextComponent(Config.getInstance().getClickToConfirmText());
            message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nsellhand confirm"));
            message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(text).create()));

            player.spigot().sendMessage(message);
            players.put(player, handItems);

        } else if (args[0].equalsIgnoreCase("confirm")) {

            if(player.getInventory().getItemInMainHand().equals(players.get(player))) {
                Item item = MarketManager.getInstance().getItem(player.getInventory().getItemInMainHand().getType().toString());

                item.sellItem(handItems.getAmount(), player, 1);
            } else {
                Bukkit.broadcastMessage(lang.getSellHandErrorText());
            }
        }


        return false;
    }

}
