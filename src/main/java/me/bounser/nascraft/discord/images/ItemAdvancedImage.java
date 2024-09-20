package me.bounser.nascraft.discord.images;

import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.plot.PlotData;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ItemAdvancedImage {

    public static BufferedImage getImage(Item item) {

        BufferedImage image = new BufferedImage(8*128, 4*124, BufferedImage.TYPE_INT_ARGB);

        Graphics graphics = image.getGraphics();

        graphics.setColor(new Color(30, 30, 30));

        graphics.fillRoundRect(0,0,8*128-1, 3*128-1, 20, 20);

        graphics.setColor(new Color(79, 81, 88));

        graphics.fillRoundRect(0,3*128+9,8*128-1, 102, 20, 20);

        PlotData pd = new PlotData(item);

        int[] xPositions = pd.getXPositions(8*128-3, -11, false);
        int[] yPositions = pd.getYPositions(3*128-5, 5, false, true);

        int prevY = yPositions[0];
        for (int i = 1; i < 60 ; i++) {

            if (prevY<yPositions[i]) {
                graphics.setColor(new Color(250, 50, 50));
                graphics.fillRect(xPositions[i], yPositions[i]-Math.abs(prevY-yPositions[i]), 9, Math.abs(prevY-yPositions[i]));
            } else if (prevY>yPositions[i]) {
                graphics.setColor(new Color(50, 250, 50));
                graphics.fillRect(xPositions[i], yPositions[i], 9, Math.abs(prevY-yPositions[i]));
            } else {
                graphics.setColor(new Color(50, 250, 50));
                graphics.fillRect(xPositions[i], yPositions[i]-1, 9, 2);
            }

            prevY = yPositions[i];
        }

        graphics.setColor(new Color(80, 80, 250));
        int[] smoothPath = new int[60];
        smoothPath[0] = yPositions[0];
        smoothPath[1] = (yPositions[0] + yPositions[1])/2;
        smoothPath[2] = (yPositions[0] + yPositions[1] + yPositions[3])/3;
        for (int i = 3; i<60 ; i++) {
            smoothPath[i] = (yPositions[i] + yPositions[i-1] + yPositions[i-2])/3;
        }
        graphics.drawPolyline(xPositions, smoothPath, 60);

        graphics.setColor(new Color(230, 100, 100));
        smoothPath[0] = yPositions[0];
        smoothPath[1] = (yPositions[0] + yPositions[1])/2;
        smoothPath[2] = (yPositions[0] + yPositions[1] + yPositions[2])/3;
        smoothPath[3] = (yPositions[0] + yPositions[1] + yPositions[2] + yPositions[3])/4;
        smoothPath[4] = (yPositions[0] + yPositions[1] + yPositions[2] + yPositions[3] + yPositions[4])/5;
        for (int i = 6; i<60 ; i++) {
            smoothPath[i] = (yPositions[i] + yPositions[i-1] + yPositions[i-2] + yPositions[i-3] + yPositions[i-4] + yPositions[i-5])/6;
        }
        graphics.drawPolyline(xPositions, smoothPath, 60);

        graphics.drawImage(item.getIcon(), 18, 3*128+15, 90, 90, null);
        graphics.setFont(new Font("Arial", Font.BOLD, 41));
        graphics.setColor(new Color(250, 250, 250));
        graphics.drawString(item.getName() + " | " + Formatter.plainFormat(item.getCurrency(), item.getPrice().getValue(), Style.ROUND_BASIC), 135, 3*128+75);

        graphics.dispose();

        return image;
    }

}
