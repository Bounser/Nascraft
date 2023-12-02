package me.bounser.nascraft.discord.images;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.advancedgui.Images;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.market.unit.Item;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;

public class ItemBasicImage {

    private static final Lang lang = Lang.get();

    public static BufferedImage getImage(Item item) {

        BufferedImage image = new BufferedImage(8*128, 5*128, BufferedImage.TYPE_INT_ARGB);

        Graphics graphics = image.getGraphics();

        BufferedImage backgroundImage;

        try {
            if(item.getPlotData().isGoingUp()) {
                backgroundImage = ImageIO.read(Objects.requireNonNull(Nascraft.getInstance().getResource("images/gradient_up.png")));
            } else {
                backgroundImage = ImageIO.read(Objects.requireNonNull(Nascraft.getInstance().getResource("images/gradient_down.png")));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BufferedImage tempImage = new BufferedImage(backgroundImage.getWidth(), backgroundImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D graphics2D = tempImage.createGraphics();

        graphics2D.drawImage(backgroundImage, 0, 0, null);

        if(item.getPlotData().isGoingUp()) {
            graphics2D.setColor(new Color(30, 150, 30));
        } else {
            graphics2D.setColor(new Color(150, 30, 30));
        }

        graphics2D.drawPolyline(item.getPlotData().getXPositions(backgroundImage.getWidth(), 0, false) , item.getPlotData().getYPositions(backgroundImage.getHeight(), 1, false, false), item.getPlotData().getNPoints(false));
        graphics2D.drawPolyline(item.getPlotData().getXPositions(backgroundImage.getWidth(), 1, false) , item.getPlotData().getYPositions(backgroundImage.getHeight(), 2, false, false), item.getPlotData().getNPoints(false));
        graphics2D.drawPolyline(item.getPlotData().getXPositions(backgroundImage.getWidth(), -1, false) , item.getPlotData().getYPositions(backgroundImage.getHeight(), 2, false, false), item.getPlotData().getNPoints(false));

        graphics2D.setComposite(AlphaComposite.Clear);

        graphics2D.fillPolygon(
                item.getPlotData().getXPositions(backgroundImage.getWidth(), 0, true),
                item.getPlotData().getYPositions(backgroundImage.getHeight(), 0, true, false),
                item.getPlotData().getNPoints(true));

        graphics2D.setComposite(AlphaComposite.SrcOver);

        graphics2D.dispose();

        graphics.setColor(new Color(43,45,49));
        graphics.fillRect(0, 450, 8*128, 100);

        graphics.drawImage(tempImage, 0, 128, 1024, 370, null);

        int[] height = item.getPlotData().getExtremePositions(0, backgroundImage.getHeight());
        if (height[0] != 0 && height[1] != 0) {

            if(item.getPlotData().isGoingUp())
                graphics.setColor(new Color(100, 250, 100));
            else
                graphics.setColor(new Color(250, 100, 100));

            graphics.setFont(new Font("Arial", Font.BOLD, 22));

            float[] high = item.getPlotData().getHighestValue(8*128, item.getPrices(TimeSpan.HOUR).size());
            float[] low = item.getPlotData().getLowestValue(8*128, item.getPrices(TimeSpan.HOUR).size());

            drawCenteredString(graphics, Formatter.format(high[0], Style.ROUND_BASIC), Math.round(high[1]), 135, 8*128);
            drawCenteredString(graphics, Formatter.format(low[0], Style.ROUND_BASIC), Math.round(low[1]), 475, 8*128);
        }

        graphics.setColor(new Color(69, 69, 79));
        graphics.fillRoundRect(5, 0, 8*128-5, 100, 40, 40);
        graphics.fillRoundRect(0, 195*2+128, 8*128, 61*2, 40, 40);

        graphics.setColor(new Color(99, 99, 119));
        graphics.fillRoundRect(0, 0, 110, 100, 40, 40);
        graphics.fillRoundRect(0, 195*2+128, 270, 61*2, 40, 40);

        graphics.drawImage(Images.getInstance().getImage(item.getMaterial(), 100, 100, false), 5, 0, 100, 100, null);

        graphics.setFont(new Font("Arial", Font.BOLD, 26));
        graphics.setColor(new Color(150, 255, 150));

        graphics.drawString(lang.message(Message.DISCORD_BUY) + Formatter.format(item.getPrice().getBuyPrice(), Style.ROUND_BASIC), 20, 440+128);

        graphics.setColor(new Color(255, 150, 150));

        graphics.drawString(lang.message(Message.DISCORD_SELL) + Formatter.format(item.getPrice().getSellPrice(), Style.ROUND_BASIC), 20, 480+128);


        graphics.setFont(new Font("Arial", Font.BOLD, 47));

        graphics.setColor(new Color(255, 255, 255));
        graphics.drawString(item.getName() + " | " + Formatter.format(item.getPrice().getValue(), Style.ROUND_BASIC), 67*2, 34*2);

        graphics.setFont(new Font("Arial", Font.BOLD, 21));

        graphics.drawString(lang.message(Message.DISCORD_DAY_HIGH) + Formatter.format(item.getPrice().getDayHigh(), Style.ROUND_BASIC), 290, 420+128);
        graphics.drawString(lang.message(Message.DISCORD_DAY_LOW) + Formatter.format(item.getPrice().getDayLow(), Style.ROUND_BASIC), 290, 460+128);
        graphics.drawString(lang.message(Message.DISCORD_HISTORICAL_HIGH) + Formatter.format(item.getPrice().getHistoricalHigh(), Style.ROUND_BASIC), 290, 500+128);
        graphics.drawString(lang.message(Message.DISCORD_DAILY_VOLUME) + Formatter.format(item.getVolume(), Style.REDUCED_LENGTH), 670, 420+128);
        graphics.drawString(lang.message(Message.DISCORD_POSITION) + MarketManager.getInstance().getPositionByVolume(item), 670, 460+128);
        NumberFormat formatter = new DecimalFormat("#0.0");
        graphics.drawString(lang.message(Message.DISCORD_TREND) + formatter.format((-100 + item.getPrice().getValue()*100/item.getPrices(TimeSpan.DAY).get(0))) + "%" , 670, 500+128);

        graphics.dispose();

        return image;
    }

    public static void drawCenteredString(Graphics g, String text, int centerX, int y, int canvasWidth) {
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);

        int x = centerX - (textWidth / 2);

        if (x < 0) {
            x = 0;
        } else if (x + textWidth > canvasWidth) {
            x = canvasWidth - textWidth;
        }

        g.drawString(text, x, y);
    }

}
