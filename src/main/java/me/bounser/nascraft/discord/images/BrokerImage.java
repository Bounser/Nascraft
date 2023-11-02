package me.bounser.nascraft.discord.images;

import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.market.Plot;
import me.bounser.nascraft.market.brokers.Broker;
import me.bounser.nascraft.market.managers.MarketManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class BrokerImage {

    public static BufferedImage getImage(Broker broker) {

        BufferedImage image = new BufferedImage(854, 470, BufferedImage.TYPE_INT_ARGB);

        Graphics graphics = image.getGraphics();

        graphics.setColor(new Color(30,30,30));
        graphics.fillRoundRect(5, 5, 600, 260, 20, 20);
        graphics.fillRoundRect(5, 275, 600, 195, 20, 20);

        graphics.setColor(new Color(79, 81, 88));
        graphics.fillRoundRect(610, 5, 244, 465, 20, 20);

        List<Float> values = broker.getPrices();
        List<Float> benchmark = MarketManager.getInstance().getBenchmark1h(values.get(0));
        graphics.setColor(new Color(250, 160, 70));
        graphics.drawPolyline(Plot.getXPositions(600, 5, false, benchmark.size()),
                Plot.getYYPositions(260, 5, false, benchmark, values), benchmark.size());

        graphics.setFont(new Font("Helvetica", Font.BOLD, 15));
        graphics.drawString("Benchmark", 20, 255);

        graphics.setColor(new Color(100, 100, 250));
        graphics.drawPolyline(Plot.getXPositions(600, 5, false, values.size()),
                Plot.getYYPositions(260, 5, false, values, benchmark), values.size());

        graphics.drawString("Broker's return", 130, 255);

        graphics.setColor(new Color(250, 250, 250));
        graphics.setFont(new Font("Helvetica", Font.BOLD, 25));
        graphics.drawString("60 minutes:", 15, 35);
        graphics.drawString("All time:", 15, 305);
        graphics.setFont(new Font("Helvetica", Font.BOLD, 23));
        graphics.drawString(broker.getAlias(), 630, 40);
        graphics.setFont(new Font("Helvetica", Font.BOLD, 13));
        graphics.drawString("Share price: " + RoundUtils.preciseRound(broker.getValue()), 630, 70);
        graphics.drawString("Change (60m): " + RoundUtils.preciseRound(-100 + 100*broker.getPrices().get(59)/broker.getPrices().get(0)), 630, 100);
        graphics.drawString("Benchmark (60m): " + RoundUtils.preciseRound((-100 + (MarketManager.getInstance().getBenchmark1h(100).get(59)))), 630, 130);

        return image;
    }

}
