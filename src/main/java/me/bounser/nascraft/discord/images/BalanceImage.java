package me.bounser.nascraft.discord.images;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.portfolio.PortfoliosManager;
import me.bounser.nascraft.discord.linking.LinkManager;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.awt.image.BufferedImage;

public class BalanceImage {

    public static BufferedImage getImage(User user) {

        BufferedImage image = new BufferedImage(600, 50, BufferedImage.TYPE_INT_ARGB);

        OfflinePlayer player = Bukkit.getOfflinePlayer(LinkManager.getInstance().getUUID(user.getId()));
        double purse = Nascraft.getEconomy().getBalance(player);
        double inventory = PortfoliosManager.getInstance().getPortfolio(user.getId()).getInventoryValue();
        float brokerValue = 0;

        float total = (float) (purse + inventory + brokerValue);

        Graphics graphics = image.getGraphics();

        graphics.setFont(new Font("Helvetica", Font.BOLD, 23));

        graphics.setColor(new Color(100,250,100));
        int purseWidth = (int) Math.round(600.0*(purse/total));
        graphics.fillRect(0, 0, purseWidth, 50);
        graphics.setColor(new Color(60,100,60));
        graphics.drawString("Purse", 7, 34);

        graphics.setColor(new Color(250,250,100));
        int inventoryWidth = (int) Math.round(600.0*(inventory/total));
        graphics.fillRect(purseWidth, 0, inventoryWidth, 50);
        graphics.setColor(new Color(100,100,60));
        graphics.drawString("Inventory", 7 + purseWidth, 34);

        graphics.setColor(new Color(250,100,100));
        int brokerWidth = (int) Math.round(600.0*(brokerValue/total));
        graphics.fillRect(purseWidth+inventoryWidth, 0, brokerWidth, 50);
        graphics.setColor(new Color(100,60,60));
        graphics.drawString("Broker", 7 + purseWidth + inventoryWidth, 34);

        if (purse + inventory + brokerValue == 0) {
            graphics.setColor(new Color(255,255,255));
            graphics.fillRect(0, 0, 600, 50);
        }

        graphics.dispose();

        return image;
    }

}
