package me.bounser.nascraft.chart.price;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public class ItemChartReduced {

    private static BufferedImage ditheredUp;
    private static BufferedImage ditheredDown;

    public static void load() {
        ditheredUp = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        ditheredDown = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

        Graphics graphicsUp = ditheredUp.getGraphics();
        Graphics graphicsDown = ditheredDown.getGraphics();

        try {
            graphicsUp.drawImage(ImageIO.read(Nascraft.getInstance().getResource("images/gradient-dithered-up.png")), 0, 0, null);
            graphicsDown.drawImage(ImageIO.read(Nascraft.getInstance().getResource("images/gradient-dithered-down.png")), 0, 0, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        graphicsUp.dispose();
        graphicsDown.dispose();
    }

    public static BufferedImage getImage(Item item, ChartType chartType) {

        Item finalItem = item.isParent() ? item : item.getParent();

        BufferedImage image = createChart(finalItem, chartType).createBufferedImage(128, 128);

        boolean up = false;

        switch (chartType) {
            case DAY:
                up = item.getPrice().getDayChange() > 0; break;
            case MONTH:
                up = item.getPrice().getMonthChange() > 0; break;
            case YEAR:
                up = item.getPrice().getYearChange() > 0; break;
            case ALL:
                up = item.getPrice().getAllChange() > 0; break;

            default: up = false;
        }

        BufferedImage background;

        if (up) {
            background = mergeImages(image, ditheredUp);
        } else {
            background = mergeImages(image, ditheredDown);
        }

        Graphics2D imageGraphics = background.createGraphics();

        drawDottedLine(imageGraphics, 2, 31, 125, 31, Color.GRAY, 1, 2);
        drawDottedLine(imageGraphics, 2, 62, 125, 62, Color.GRAY, 1, 2);
        drawDottedLine(imageGraphics, 2, 93, 125, 93, Color.GRAY, 1, 2);

        imageGraphics.dispose();

        return background;
    }

    public static void drawDottedLine(Graphics2D g, int x1, int y1, int x2, int y2, Color color, float dotLength, float spaceLength) {
        g.setColor(color);
        float[] dashPattern = { dotLength, spaceLength };
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, dashPattern, 0));

        g.drawLine(x1, y1, x2, y2);
    }

    public static BufferedImage mergeImages(BufferedImage img1, BufferedImage img2) {
        int width = Math.max(img1.getWidth(), img2.getWidth());
        int height = Math.max(img1.getHeight(), img2.getHeight());
        BufferedImage mergedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel1 = (x < img1.getWidth() && y < img1.getHeight()) ? img1.getRGB(x, y) : 0;
                int pixel2 = (x < img2.getWidth() && y < img2.getHeight()) ? img2.getRGB(x, y) : 0;

                Color color1 = new Color(pixel1, true);
                Color color2 = new Color(pixel2, true);

                if (color1.getAlpha() == 0) {
                    mergedImage.setRGB(x, y, new Color(0, 0, 0, 0).getRGB());
                } else if (areColorsSimilar(pixel1, (new Color(0, 155, 0).getRGB()), 15) ||  areColorsSimilar(pixel1, (new Color(155, 0, 0).getRGB()), 15)) {
                    mergedImage.setRGB(x, y, pixel1);
                } else {
                    mergedImage.setRGB(x, y, pixel2);
                }
            }
        }

        return mergedImage;
    }

    public static boolean areColorsSimilar(int color1, int color2, int tolerance) {
        int red1 = (color1 >> 16) & 0xFF;
        int green1 = (color1 >> 8) & 0xFF;
        int blue1 = color1 & 0xFF;

        int red2 = (color2 >> 16) & 0xFF;
        int green2 = (color2 >> 8) & 0xFF;
        int blue2 = color2 & 0xFF;

        int redDiff = Math.abs(red1 - red2);
        int greenDiff = Math.abs(green1 - green2);
        int blueDiff = Math.abs(blue1 - blue2);

        return redDiff <= tolerance && greenDiff <= tolerance && blueDiff <= tolerance;
    }

    private static JFreeChart createChart(Item item, ChartType chartType) {

        List<Instant> data;

        boolean up;

        switch (chartType) {
            case DAY:
                data = DatabaseManager.get().getDatabase().getDayPrices(item);
                break;
            case MONTH:
                data = DatabaseManager.get().getDatabase().getMonthPrices(item);
                break;
            case YEAR:
                data = DatabaseManager.get().getDatabase().getYearPrices(item);
                break;
            case ALL:
                data = DatabaseManager.get().getDatabase().getAllPrices(item);
                break;
            default:
                data = DatabaseManager.get().getDatabase().getDayPrices(item);
        }

        TimeSeries series = createPriceDataset(data, item, chartType);
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);

        switch (chartType) {
            case DAY:
                up = (item.getPrice().getDayChange() > 0);
                break;
            case MONTH:
                up = (item.getPrice().getMonthChange() > 0);
                break;
            case YEAR:
                up = (item.getPrice().getYearChange() > 0);
                break;
            case ALL:
                up = (item.getPrice().getAllChange() > 0);
                break;
            default:
                up = (item.getPrice().getDayChange() > 0);
        }

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                null,
                null,
                dataset,
                false,
                false,
                false
        );

        XYPlot plot = chart.getXYPlot();

        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        dateAxis.setVisible(false);
        rangeAxis.setVisible(false);

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        plot.setBackgroundPaint(new Color(0,0 ,0,0));

        XYAreaRenderer areaRenderer = new XYAreaRenderer();

        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer();
        lineRenderer.setSeriesShapesVisible(0, false);
        lineRenderer.setSeriesStroke(0, new BasicStroke(1.5f));

        Color areaColor;
        if (up) {
            chart.setBackgroundPaint(new Color(0, 0, 0, 0));
            areaColor = new Color(255, 255, 255);
            lineRenderer.setSeriesPaint(0, new Color(0, 155, 0));
        } else {
            chart.setBackgroundPaint(new Color(0, 0, 0, 0));
            areaColor = new Color(255, 255, 255);
            lineRenderer.setSeriesPaint(0, new Color(155, 0, 0));
        }

        plot.setInsets(new RectangleInsets(7, 0, 0, 0), true);

        areaRenderer.setSeriesPaint(0, areaColor);
        areaRenderer.setOutline(false);

        dateAxis.setLowerMargin(0);
        dateAxis.setUpperMargin(0);

        plot.setDataset(1, dataset);
        plot.setRenderer(0, lineRenderer);
        plot.setRenderer(1, areaRenderer);

        plot.setOutlineVisible(false);
        chart.setPadding(new RectangleInsets(0, 0, 0, 0));

        plot.mapDatasetToRangeAxis(1, 0);
        lineRenderer.setSeriesVisibleInLegend(0, false);

        return chart;
    }

    private static TimeSeries createPriceDataset(List<Instant> instants, Item item, ChartType type) {

        TimeSeries series1 = new TimeSeries("Price");

        double firstValue = 0;
        LocalDateTime timeOld = LocalDateTime.now();

        double lastValue = 0;
        LocalDateTime timeRecent = LocalDateTime.of(2023, 1, 1, 1, 1);

        double high = 0, low = -1;

        for (Instant instant : instants) {

            LocalDateTime time = instant.getLocalDateTime();

            Minute minute = new Minute(time.getMinute(),
                    time.getHour(),
                    time.getDayOfMonth(),
                    time.getMonthValue(),
                    time.getYear());

            if (instant.getPrice() != 0) {
                series1.addOrUpdate(minute, instant.getPrice());

                if (high == 0 || high < instant.getPrice()) high = instant.getPrice();
                if (instant.getPrice() != 0 && (low == -1 || low > instant.getPrice())) low = instant.getPrice();

                if (time.isAfter(timeRecent)) {
                    timeRecent = time;
                    lastValue = instant.getPrice();
                }

                if (time.isBefore(timeOld)) {
                    timeOld = time;
                    firstValue = instant.getPrice();
                }
            }
        }

        switch (type) {
            case DAY:
                item.getPrice().setDayHigh(high);
                item.getPrice().setDayLow(low);
                item.getPrice().setDayChange((float) (-1 + lastValue / firstValue));
                break;
            case MONTH:
                item.getPrice().setMonthHigh(high);
                item.getPrice().setMonthLow(low);
                item.getPrice().setMonthChange((float) (-1 + lastValue / firstValue));
                break;
            case YEAR:
                item.getPrice().setYearHigh(high);
                item.getPrice().setYearLow(low);
                item.getPrice().setYearChange((float) (-1 + lastValue / firstValue));
                break;
            case ALL:
                item.getPrice().setAllHigh(high);
                item.getPrice().setAllLow(low);
                item.getPrice().setAllChange((float) (-1 + lastValue / firstValue));
                break;
        }

        return series1;
    }
}
