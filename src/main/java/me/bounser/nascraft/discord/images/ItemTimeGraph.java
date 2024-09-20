package me.bounser.nascraft.discord.images;

import me.bounser.nascraft.chart.price.ChartType;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.chart.price.ItemChart;
import me.bounser.nascraft.market.unit.Item;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ItemTimeGraph {

    public static BufferedImage getImage(Item item, ChartType chartType, String userid) {

        BufferedImage image = new BufferedImage(610, 290, BufferedImage.TYPE_INT_ARGB);

        Graphics graphics = image.getGraphics();

        graphics.drawImage(ItemChart.getImage(item, chartType, userid, 600, 250), 5, 40, null);

        graphics.drawImage(item.getIcon(), 40, 0, 50, 50, null);

        graphics.setFont(new Font("Arial", Font.BOLD, 23));

        graphics.setColor(new Color(255, 255, 255));
        graphics.drawString(item.getName() + " | " + Formatter.plainFormat(item.getCurrency(), item.getPrice().getValue(), Style.ROUND_BASIC), 95, 38);

        graphics.dispose();

        return image;
    }

}
