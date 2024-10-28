package me.bounser.nascraft.chart.price;

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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.List;

public class ItemChartReduced {

    public static BufferedImage getImage(Item item, ChartType chartType) {

        Item finalItem = item.isParent() ? item : item.getParent();

        BufferedImage image = createChart(finalItem, chartType).createBufferedImage(128, 128);

        Graphics2D graphics = image.createGraphics();

        graphics.setColor(Color.BLACK);
        graphics.drawRect(0, 0, 127, 127);

        drawDottedLine(graphics, 2, 31, 125, 31, Color.GRAY, 1, 2);
        drawDottedLine(graphics, 2, 62, 125, 62, Color.GRAY, 1, 2);
        drawDottedLine(graphics, 2, 93, 125, 93, Color.GRAY, 1, 2);

        graphics.dispose();

        return image;
    }

    public static void drawDottedLine(Graphics2D g, int x1, int y1, int x2, int y2, Color color, float dotLength, float spaceLength) {
        g.setColor(color);
        float[] dashPattern = { dotLength, spaceLength };
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, dashPattern, 0));

        g.drawLine(x1, y1, x2, y2);
    }

    private static JFreeChart createChart(Item item, ChartType chartType) {

        List<Instant> data;

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

        plot.setBackgroundPaint(new Color(0, 0, 0, 0));
        chart.setBackgroundPaint(new Color(0, 0, 0, 0));

        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        dateAxis.setVisible(false);
        rangeAxis.setVisible(false);

        plot.setInsets(new RectangleInsets(0, 0, 0, 0));

        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (int i = 0; i < series.getItemCount(); i++) {
            double value = series.getDataItem(i).getValue().doubleValue();
            if (value < minY) {
                minY = value;
            }
            if (value > maxY) {
                maxY = value;
            }
        }

        if (series.getItemCount() > 0) {
            dateAxis.setRange(series.getTimePeriod(0).getStart().getTime(), series.getTimePeriod(series.getItemCount() - 1).getEnd().getTime());
            rangeAxis.setRange(minY, maxY);
        }


        XYAreaRenderer areaRenderer = new XYAreaRenderer();

        double initialPrice = series.getDataItem(0).getValue().doubleValue();
        double finalPrice = series.getDataItem(series.getItemCount() - 1).getValue().doubleValue();

        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer();
        lineRenderer.setSeriesShapesVisible(0, false);
        lineRenderer.setSeriesStroke(0, new BasicStroke(1f));

        Color areaColor;
        if (finalPrice >= initialPrice) {
            areaColor = new Color(137,172,76, 150);
            lineRenderer.setSeriesPaint(0, new Color(10, 150, 10));
        } else {
            areaColor = new Color(225,133,163, 150);
            lineRenderer.setSeriesPaint(0, new Color(150, 10, 10));
        }
        areaRenderer.setSeriesPaint(0, areaColor);

        plot.setDataset(1, dataset);
        plot.setRenderer(0, lineRenderer);
        plot.setRenderer(1, areaRenderer);

        plot.mapDatasetToRangeAxis(1, 0);
        lineRenderer.setSeriesVisibleInLegend(0, false);

        return chart;
    }

    private static TimeSeries createPriceDataset(List<Instant> instants, Item item, ChartType type) {

        TimeSeries series1 = new TimeSeries("Price");

        float firstValue = 0;
        LocalDateTime timeOld = LocalDateTime.now();

        float lastValue = 0;
        LocalDateTime timeRecent = LocalDateTime.of(2023, 1, 1, 1, 1);

        float high = 0, low = -1;

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
                item.getPrice().setDayChange(-1 + lastValue / firstValue);
                break;
            case MONTH:
                item.getPrice().setMonthHigh(high);
                item.getPrice().setMonthLow(low);
                item.getPrice().setMonthChange(-1 + lastValue / firstValue);
                break;
            case YEAR:
                item.getPrice().setYearHigh(high);
                item.getPrice().setYearLow(low);
                item.getPrice().setYearChange(-1 + lastValue / firstValue);
                break;
            case ALL:
                item.getPrice().setAllHigh(high);
                item.getPrice().setAllLow(low);
                item.getPrice().setAllChange(-1 + lastValue / firstValue);
                break;
        }

        return series1;
    }
}
