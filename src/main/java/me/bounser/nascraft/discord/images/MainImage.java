package me.bounser.nascraft.discord.images;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.managers.ImagesManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.plot.PlotData;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MainImage {

    private static Lang lang = Lang.get();

    public static BufferedImage getImage() {


        BufferedImage image;
        if (MarketManager.getInstance().getAllItems().size() > 25) {
            image = new BufferedImage(915, 490, BufferedImage.TYPE_INT_ARGB);
        } else {
            image = new BufferedImage(915, 440, BufferedImage.TYPE_INT_ARGB);
        }

        Graphics graphics = image.getGraphics();

        int[] offset = {1, 25};

        graphics.setFont(new Font("Arial", Font.BOLD, 23));

        graphics.setColor(new Color(80, 210, 80));
        graphics.drawString(lang.message(Message.DISCORD_TOP_GAINERS), 8, 18);
        graphics.setColor(new Color(80, 80, 240));
        graphics.drawString(lang.message(Message.DISCORD_MOST_POPULAR), 313, 18);
        graphics.setColor(new Color(220, 80, 80));
        graphics.drawString(lang.message(Message.DISCORD_BIG_DIPPERS), 618, 18);

        graphics.setColor(new Color(0, 0, 0));
        graphics.drawRoundRect(offset[0], 25+128*3, 300, 65, 15, 15);

        int intensity = 100;
        int intraoffset = 0;

        for (Item item : MarketManager.getInstance().getTopGainers(8)) {

            if (intensity >= 40) {
                setSegment(item, offset, graphics, new Color(10, intensity, 10));
                intensity -= 30;
                offset[1] += 128;
            } else {
                setItem(item, offset, intraoffset, graphics, new Color(100, 200, 100));
                intraoffset += 60;
            }
        }

        offset[0] += 305;
        offset[1] = 25;
        intensity = 100;
        intraoffset = 0;

        graphics.setColor(new Color(0, 0, 0));
        graphics.drawRoundRect(offset[0], 25+128*3, 300, 65, 15, 15);

        for(Item item : MarketManager.getInstance().getMostTraded(8)) {

            if (intensity >= 40) {
                setSegment(item, offset, graphics, new Color(10, 10, intensity));
                intensity -= 30;
                offset[1] += 128;
            } else {
                setItem(item, offset, intraoffset, graphics, new Color(100, 100, 200));
                intraoffset += 60;
            }
        }

        offset[0] += 305;
        offset[1] = 25;
        intensity = 100;
        intraoffset = 0;

        graphics.setColor(new Color(0, 0, 0));
        graphics.drawRoundRect(offset[0], 25+128*3, 300, 65, 15, 15);

        for(Item item : MarketManager.getInstance().getTopDippers(8)) {

            if (intensity >= 40) {
                setSegment(item, offset, graphics, new Color(intensity, 10, 10));
                intensity -= 30;
                offset[1] += 128;
            } else {
                setItem(item, offset, intraoffset, graphics, new Color(200, 100, 100));
                intraoffset += 60;
            }
        }

        graphics.dispose();

        return image;
    }

    public static void setSegment(Item item, int[] offset, Graphics graphics, Color color) {

        PlotData pd = new PlotData(item);

        if(pd.isGoingUp()) {
            graphics.setColor(new Color(10, 250, 10));
        } else {
            graphics.setColor(new Color(250, 10, 10));
        }

        try {
            graphics.drawPolyline(pd.getXPositions(295, offset[0] + 2, false), pd.getYPositions(90, 30 + offset[1], false, false), pd.getNPoints(false));
            graphics.drawPolyline(pd.getXPositions(295, offset[0] + 3, false), pd.getYPositions(90, 30 + offset[1], false, false), pd.getNPoints(false));
            graphics.drawPolyline(pd.getXPositions(295, offset[0] + 2, false), pd.getYPositions(90, 31 + offset[1], false, false), pd.getNPoints(false));
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        }

        graphics.setColor(new Color(255, 255, 255));

        graphics.setFont(new Font("Arial", Font.BOLD, 18));

        graphics.drawString(item.getName(), 55 + offset[0],  20 + offset[1]);

        graphics.drawString(Formatter.plainFormat(item.getCurrency(), item.getPrice().getValue(), Style.ROUND_BASIC), 55 + offset[0], 50 + offset[1]);

        graphics.drawLine(52 + offset[0], 30 + offset[1], 150 + offset[0], 30 + offset[1]);
        graphics.drawLine(52 + offset[0], 29 + offset[1], 150 + offset[0], 29 + offset[1]);

        if(pd.isGoingUp()) {
            graphics.setColor(new Color(100, 250, 100));
        } else {
            graphics.setColor(new Color(250, 100, 100));
        }

        graphics.setFont(new Font("Arial", Font.BOLD, 15));

        graphics.drawString(pd.getChange(), 15 + offset[0], 110 + offset[1]);

        graphics.setColor(color);

        graphics.drawRoundRect(offset[0], offset[1], 300, 120, 15, 15);
        graphics.drawRoundRect(offset[0]+1, offset[1]+1, 298, 118, 15, 15);
        graphics.drawRoundRect(offset[0]+2, offset[1]+2, 296, 116, 15, 15);

        graphics.drawImage(ImagesManager.getInstance().getImage(item.getIdentifier()), offset[0]+2, offset[1], 48, 48, null);

    }

    public static void setItem(Item item, int[] offset, int intraOffset, Graphics graphics, Color color) {

        graphics.drawImage(item.getIcon(), offset[0]+7+intraOffset, offset[1], 45, 45, null);

        graphics.setColor(color);

        PlotData pd = new PlotData(item);

        graphics.drawString(pd.getChange(), 15 - Math.round(pd.getChange().getBytes().length) + offset[0] + intraOffset, offset[1]+60);
    }
}
