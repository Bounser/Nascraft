package me.bounser.nascraft.discord.images;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.portfolio.Portfolio;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.market.unit.Item;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InventoryImage {

    private static final Lang lang = Lang.get();

    public static BufferedImage getImage(Portfolio discordInventory) {

        HashMap<Item, Integer> inventory = discordInventory.getContent();

        int capacity = discordInventory.getCapacity();

        BufferedImage image = new BufferedImage(854, 470, BufferedImage.TYPE_INT_ARGB);

        Graphics graphics = image.getGraphics();

        try {
            graphics.drawImage(ImageIO.read(Nascraft.getInstance().getResource("images/discord_inventory.png")), 0, 0, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        graphics.setFont(new Font("Helvetica", Font.BOLD, 32));
        graphics.setColor(new Color(64,65,65));

        graphics.drawString(lang.message(Message.DISCORD_INVENTORY_VALUE) + Formatter.plainFormat(CurrenciesManager.getInstance().getVaultCurrency(), discordInventory.getInventoryValue(), Style.ROUND_BASIC), 30, 53);

        List<Item> items = new ArrayList<>(inventory.keySet());

        int slot = 0;

        for (int j = 0 ; j < 4 ; j++) {

            for (int i = 0; i < 10; i++) {

                slot++;
                if (slot > capacity) {

                    graphics.setColor(new Color(198, 199, 199));

                    graphics.fillRect(i*79+32, j*81+114, 83, 83);

                } else {

                    if (items.size() >= slot) {
                        Item item = items.get(slot-1);

                        graphics.drawImage(item.getIcon(), i*79+40, j*81+124, 63, 63, null);

                        graphics.setFont(new Font("Garamond", Font.BOLD, 26));
                        graphics.setColor(new Color(0, 0, 0));
                        graphics.drawString(inventory.get(item).toString(), i*79+110-inventory.get(item).toString().length()*20, j*81+188);

                        graphics.setColor(new Color(255, 255, 255));
                        graphics.drawString(inventory.get(item).toString(), i*79+107-inventory.get(item).toString().length()*20, j*81+185);
                    }
                }
            }
        }

        graphics.dispose();

        return image;
    }

}
