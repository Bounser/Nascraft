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

    private static Lang lang = Lang.get();

    public static BufferedImage getImage(Item item) {

        BufferedImage image = new BufferedImage(8*128, 4*128, BufferedImage.TYPE_INT_ARGB);

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

        graphics2D.drawPolyline(item.getPlotData().getXPositions(backgroundImage.getWidth(), 0, false) , item.getPlotData().getYPositions(backgroundImage.getHeight(), 1, false), item.getPlotData().getNPoints(false));
        graphics2D.drawPolyline(item.getPlotData().getXPositions(backgroundImage.getWidth(), 1, false) , item.getPlotData().getYPositions(backgroundImage.getHeight(), 2, false), item.getPlotData().getNPoints(false));
        graphics2D.drawPolyline(item.getPlotData().getXPositions(backgroundImage.getWidth(), -1, false) , item.getPlotData().getYPositions(backgroundImage.getHeight(), 2, false), item.getPlotData().getNPoints(false));

        graphics2D.setComposite(AlphaComposite.Clear);

        graphics2D.fillPolygon(
                item.getPlotData().getXPositions(backgroundImage.getWidth(), 0, true),
                item.getPlotData().getYPositions(backgroundImage.getHeight(), 0, true),
                item.getPlotData().getNPoints(true));

        graphics2D.setComposite(AlphaComposite.SrcOver);

        graphics2D.dispose();

        graphics.drawImage(tempImage, 0, 50, 8*128, 175*2, null);

        graphics.setColor(new Color(41, 43, 55));

        graphics.fillRect(0, 200*2, 8*128, 56*2);

        graphics.drawImage(Images.getInstance().getImage(item.getMaterial(), 100, 100, false), 18, 0, 100, 100, null);

        graphics.setColor(new Color(100, 255, 100));
        graphics.setFont(new Font("Arial", Font.BOLD, 35));

        graphics.drawString(Formatter.format(item.getPrice().getValue(), Style.ROUND_TO_TWO), 20, 440);

        graphics.setFont(new Font("Arial", Font.BOLD, 22));
        graphics.setColor(new Color(80, 155, 80));

        graphics.drawString(lang.message(Message.DISCORD_BUY) + Formatter.format(item.getPrice().getBuyPrice(), Style.ROUND_TO_TWO), 25, 473);

        graphics.setColor(new Color(155, 80, 80));

        graphics.drawString(lang.message(Message.DISCORD_SELL) + Formatter.format(item.getPrice().getSellPrice(), Style.ROUND_TO_TWO), 25, 500);


        graphics.setFont(new Font("Arial", Font.BOLD, 45));

        graphics.setColor(new Color(255, 255, 255));
        graphics.drawString(item.getName(), 67*2, 33*2);

        graphics.setFont(new Font("Arial", Font.BOLD, 22));

        graphics.drawString(lang.message(Message.DISCORD_DAY_HIGH) + Formatter.format(item.getPrice().getDayHigh(), Style.ROUND_TO_TWO), 250, 431);
        graphics.drawString(lang.message(Message.DISCORD_DAY_LOW) + Formatter.format(item.getPrice().getDayLow(), Style.ROUND_TO_TWO), 250, 465);
        graphics.drawString(lang.message(Message.DISCORD_HISTORICAL_HIGH) + Formatter.format(item.getPrice().getHistoricalHigh(), Style.ROUND_TO_TWO), 250, 499);
        graphics.drawString(lang.message(Message.DISCORD_DAILY_VOLUME) + Formatter.format(item.getVolume(), Style.ROUND_TO_TWO), 650, 431);
        graphics.drawString(lang.message(Message.DISCORD_POSITION) + MarketManager.getInstance().getPositionByVolume(item), 650, 465);
        NumberFormat formatter = new DecimalFormat("#0.0");
        graphics.drawString(lang.message(Message.DISCORD_TREND) + formatter.format((-100 + item.getPrice().getValue()*100/item.getPrices(TimeSpan.DAY).get(0))) + "%" , 650, 499);

        graphics.dispose();

        return image;
    }

}
