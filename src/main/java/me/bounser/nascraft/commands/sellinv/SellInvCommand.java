package me.bounser.nascraft.commands.sellinv;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

public class SellInvCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        Player player;

        if(!(sender instanceof Player)) {

            if (args.length != 1) {
                Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Wrong usage of command. /nsell [playerName]");
                return false;
            }

            player = Bukkit.getPlayer(args[0]);

            if (player == null) {
                Nascraft.getInstance().getLogger().info(ChatColor.RED  + "Player not found");
                return false;
            }

        } else {
            player = (Player) sender;

            if (!player.hasPermission("nascraft.sellinv")) {
                Lang.get().message(player, Message.NO_PERMISSION);
                return false;
            }
        }

        Inventory inventory = Bukkit.createInventory(player, 45, Lang.get().message(Message.SELL_TITLE));

        insertFillingPanes(inventory);
        insertSellButton(inventory);
        insertCloseButton(inventory);
        insertHelpHead(inventory);

        player.openInventory(inventory);
        return false;
    }

    public void insertFillingPanes(Inventory inventory) {

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for(int i : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 36, 37, 38, 39, 41, 42, 43, 44}) {
            inventory.setItem(i, filler);
        }
    }

    public void insertSellButton(Inventory inventory) {

        ItemStack sellButton = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta meta = sellButton.getItemMeta();
        meta.setDisplayName(Lang.get().message(Message.SELL_BUTTON_NAME));
        meta.setLore(Collections.singletonList(Lang.get().message(Message.SELL_BUTTON_LORE, "0", "", "")));
        sellButton.setItemMeta(meta);

        inventory.setItem(40, sellButton);
    }

    public void insertCloseButton(Inventory inventory) {

        ItemStack closeButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = closeButton.getItemMeta();
        meta.setDisplayName(Lang.get().message(Message.SELL_CLOSE));
        closeButton.setItemMeta(meta);

        inventory.setItem(8, closeButton);
    }

    public void insertHelpHead(Inventory inventory) {

        String TEXTURE = "bc8ea1f51f253ff5142ca11ae45193a4ad8c3ab5e9c6eec8ba7a4fcb7bac40";

        PlayerProfile profile = getProfile(TEXTURE);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setDisplayName(Lang.get().message(Message.SELL_HELP_TITLE));
        meta.setLore(Arrays.asList(Lang.get().message(Message.SELL_HELP_LORE).split("\\n")));
        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);

        inventory.setItem(4, head);
    }

    private static PlayerProfile getProfile(String texture) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();
        URL urlObject;
        try {
            urlObject = new URL("https://textures.minecraft.net/texture/" + texture);
        } catch (MalformedURLException exception) {
            throw new RuntimeException("Invalid URL", exception);
        }
        textures.setSkin(urlObject);
        profile.setTextures(textures);
        return profile;
    }
}
