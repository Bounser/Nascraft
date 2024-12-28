package me.bounser.nascraft.chart.portfolio;

import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.portfolio.Portfolio;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class PortfolioCompositionChart {

    public static Color[] colorPalette = { new Color(77,74,174), new Color(129,73,175), new Color(243,199,64), new Color(247,140,67), new Color(211,80,98), new Color(108,171,1), new Color(209,206,236) };

    public static BufferedImage getImage(Portfolio portfolio, int width, int height) {

        if (portfolio.getContent().isEmpty()) return new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

        return createChart(portfolio).createBufferedImage(width, height);
    }

    private static JFreeChart createChart(Portfolio portfolio) {

        PieDataset dataset = createDataset(portfolio);

        JFreeChart chart = ChartFactory.createPieChart(
                null,
                dataset,
                false,
                true,
                false);

        PiePlot plot = (PiePlot) chart.getPlot();

        plot.setLabelGenerator(null);

        chart.setBackgroundPaint(null);
        plot.setBackgroundPaint(null);

        plot.setShadowPaint(null);

        plot.setInteriorGap(0.0);
        plot.setOutlineVisible(false);

        List<Comparable> keys = dataset.getKeys();

        for (int i = 0; i < keys.size(); i++) {
            plot.setSectionPaint(keys.get(i), colorPalette[i % colorPalette.length]);
        }

        if (keys.get(keys.size()-1) instanceof String && ((String) keys.get(keys.size()-1)).equalsIgnoreCase("Other")) plot.setSectionPaint(keys.get(keys.size()-1), colorPalette[6]);

        return chart;
    }

    public static PieDataset createDataset(Portfolio portfolio) {

        DefaultPieDataset dataset = new DefaultPieDataset();
        HashMap<Item, Integer> content = portfolio.getContent();

        double totalWorth = 0;
        List<Map.Entry<Item, Double>> itemWorthList = new ArrayList<>();

        for (Item item : content.keySet()) {
            double worth = item.getPrice().getValue() * content.get(item);
            itemWorthList.add(new AbstractMap.SimpleEntry<>(item, worth));
            totalWorth += worth;
        }

        itemWorthList.sort((entry1, entry2) -> Double.compare(entry2.getValue(), entry1.getValue()));

        double threshold = 0.03 * totalWorth;
        double otherWorth = 0;
        int count = 0;

        for (Map.Entry<Item, Double> entry : itemWorthList) {
            double worth = entry.getValue();
            if (worth >= threshold && count < 6) {
                dataset.setValue(entry.getKey().getName(), worth*100/totalWorth);
                count++;
            } else {
                otherWorth += worth;
            }
        }

        if (otherWorth > 0) {
            dataset.setValue("Other", otherWorth*100/totalWorth);
        }

        return dataset;
    }

}
