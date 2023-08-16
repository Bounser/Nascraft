package me.bounser.nascraft.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.advancedgui.Images;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.RoundUtils;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.market.unit.Item;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.util.Objects;

public class ImageBuilder implements ImageObserver{


    static ImageBuilder instance;

    public static ImageBuilder getInstance() { return instance == null ? instance = new ImageBuilder() : instance; }

    public BufferedImage getMainImage() {

        BufferedImage image = new BufferedImage(915, 533, BufferedImage.TYPE_INT_ARGB);

        Graphics graphics = image.getGraphics();

        int[] offset = {1, 25};

        graphics.setFont(new Font("Arial", Font.BOLD, 23));

        graphics.setColor(new Color(80, 210, 80));
        graphics.drawString("Top gainers:", 8, 18);
        graphics.setColor(new Color(80, 80, 240));
        graphics.drawString("Most popular:", 313, 18);
        graphics.setColor(new Color(220, 80, 80));
        graphics.drawString("Big dippers:", 618, 18);

        int intensity = 100;

        for(Item item : MarketManager.getInstance().getTopGainers(4)) {

            setSegment(item.getMaterial(), offset, graphics, new Color(20, intensity, 20));

            intensity -= 20;
            offset[1] += 128;
        }

        offset[0] += 305;
        offset[1] = 25;
        intensity = 100;

        for(Item item : MarketManager.getInstance().getMostTraded(4)) {

            setSegment(item.getMaterial(), offset, graphics, new Color(20, 20, intensity));

            intensity -= 20;
            offset[1] += 128;
        }

        offset[0] += 305;
        offset[1] = 25;
        intensity = 100;

        for(Item item : MarketManager.getInstance().getTopDippers(4)) {

            setSegment(item.getMaterial(), offset, graphics, new Color(intensity, 20, 20));

            intensity -= 20;
            offset[1] += 128;
        }

        graphics.dispose();

        return image;
    }

    public void setSegment(String material, int[] offset, Graphics graphics, Color color) {

        Item item = MarketManager.getInstance().getItem(material);

        if(item.getPlotData().isGoingUp()) {
            graphics.setColor(new Color(10, 250, 10));
        } else {
            graphics.setColor(new Color(250, 10, 10));
        }

        graphics.drawPolyline(item.getPlotData().getXPositions(295, offset[0] + 2, false) , item.getPlotData().getYPositions(90, 30+ offset[1], false), item.getPlotData().getNPoints(false));
        graphics.drawPolyline(item.getPlotData().getXPositions(295, offset[0] + 3, false) , item.getPlotData().getYPositions(90, 30+ offset[1], false), item.getPlotData().getNPoints(false));
        graphics.drawPolyline(item.getPlotData().getXPositions(295, offset[0] + 2, false) , item.getPlotData().getYPositions(90, 31+ offset[1], false), item.getPlotData().getNPoints(false));

        graphics.drawImage(Images.getInstance().getImage(material, 45, 45, false), offset[0]+2, offset[1], 45, 45, this);

        graphics.setColor(new Color(255, 255, 255));

        graphics.setFont(new Font("Arial", Font.BOLD, 18));

        graphics.drawString(item.getName(), offset[0] + 50,  30 + offset[1]);

        graphics.drawString(item.getPrice().getValue() + "€", 8 + offset[0], 110 + offset[1]);

        if(item.getPlotData().isGoingUp()) {
            graphics.setColor(new Color(100, 250, 100));
        } else {
            graphics.setColor(new Color(250, 100, 100));
        }

        graphics.setFont(new Font("Arial", Font.BOLD, 15));

        graphics.drawString(item.getPlotData().getChange(), 255- (int) Math.round(item.getPlotData().getChange().getBytes().length*3.5) + offset[0], 110 + offset[1]);

        graphics.setColor(color);

        graphics.drawRoundRect(offset[0], offset[1], 300, 120, 15, 15);
        graphics.drawRoundRect(offset[0]+1, offset[1]+1, 298, 118, 15, 15);
        graphics.drawRoundRect(offset[0]+2, offset[1]+2, 296, 116, 15, 15);

    }

    public BufferedImage getImageOfItem(Item item) {

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

        graphics2D.drawImage(backgroundImage, 0, 0, this);

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

        graphics.drawImage(tempImage, 0, 50, 8*128, 175*2, this);

        graphics.setColor(new Color(41, 43, 55));

        graphics.fillRect(0, 200*2, 8*128, 56*2);

        graphics.drawImage(Images.getInstance().getImage(item.getMaterial(), 100, 100, false), 18, 0, 100, 100, this);

        graphics.setColor(new Color(100, 255, 100));
        graphics.setFont(new Font("Arial", Font.BOLD, 35));

        graphics.drawString(item.getPrice().getValue() + Config.getInstance().getCurrency(), 20, 470);

        graphics.setFont(new Font("Arial", Font.BOLD, 45));

        graphics.setColor(new Color(255, 255, 255));
        graphics.drawString(item.getName(), 67*2, 33*2);

        graphics.setFont(new Font("Arial", Font.BOLD, 22));

        graphics.drawString("1 day low: " + item.getLow(TimeSpan.DAY) + Config.getInstance().getCurrency(), 250, 431);
        graphics.drawString("1 day high: " + item.getHigh(TimeSpan.DAY) + Config.getInstance().getCurrency(), 250, 465);
        graphics.drawString("Historical high: " + RoundUtils.round(item.getPrices(TimeSpan.DAY).get(23)*3) + Config.getInstance().getCurrency(), 250, 499);
        graphics.drawString("Volume (24h): " + item.getVolume(), 650, 431);
        graphics.drawString("Position in market: #10", 650, 465);
        graphics.drawString("Trend (24h): +24.3%" , 650, 499);

        graphics.setColor(new Color(150, 150, 255));

        graphics.dispose();

        return image;
    }

    @Override
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
        return false;
    }

}
