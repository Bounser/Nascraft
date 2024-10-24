package me.bounser.nascraft.chart.price;

import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

public class AdvancedGUIChart {

    public static BufferedImage getImage(Item item, ChartType chartType, UUID uuid) {

        Item finalItem = item.isParent() ? item : item.getParent();

        return createChart(finalItem, chartType, uuid).createBufferedImage(390, 140);
    }

    private static JFreeChart createChart(Item item, ChartType chartType, UUID uuid) {

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

        TimeSeries series = createPriceDataset(data);
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

        dateAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm"));
        dateAxis.setLabelPaint(Color.WHITE);
        dateAxis.setTickLabelPaint(Color.WHITE);
        dateAxis.setAxisLinePaint(Color.WHITE);

        rangeAxis.setLabelPaint(Color.WHITE);
        rangeAxis.setTickLabelPaint(Color.WHITE);

        plot.setBackgroundPaint(Color.BLACK);
        chart.setBackgroundPaint(Color.BLACK);

        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);

        XYAreaRenderer areaRenderer = new XYAreaRenderer();

        double initialPrice = series.getDataItem(0).getValue().doubleValue();
        double finalPrice = series.getDataItem(series.getItemCount() - 1).getValue().doubleValue();

        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer();

        if (finalPrice >= initialPrice) {
            GradientPaint gradientPaint = new GradientPaint(
                    0, 0, new Color(34, 177, 76),
                    0, 130, new Color(0, 10, 0)
            );
            lineRenderer.setSeriesPaint(0, Color.GREEN);
            areaRenderer.setSeriesPaint(0, gradientPaint);
        } else {
            GradientPaint gradientPaint = new GradientPaint(
                    0, 0, new Color(237, 28, 36),
                    0, 130, new Color(10, 0, 0)
            );
            lineRenderer.setSeriesPaint(0, Color.RED);
            areaRenderer.setSeriesPaint(0, gradientPaint);
        }

        areaRenderer.setOutlinePaint(null);

        lineRenderer.setSeriesShapesVisible(0, false);
        lineRenderer.setSeriesStroke(0, new BasicStroke(1.5f));

        plot.setDataset(1, dataset);
        plot.setRenderer(0, lineRenderer);
        plot.setRenderer(1, areaRenderer);

        plot.mapDatasetToRangeAxis(1, 0);
        lineRenderer.setSeriesVisibleInLegend(0, false);

        return chart;
    }

    private static TimeSeries createPriceDataset(List<Instant> intants) {

        TimeSeries series1 = new TimeSeries("Price");

        for (Instant instant : intants) {
            Minute minute = new Minute(instant.getLocalDateTime().getMinute(),
                    instant.getLocalDateTime().getHour(),
                    instant.getLocalDateTime().getDayOfMonth(),
                    instant.getLocalDateTime().getMonthValue(),
                    instant.getLocalDateTime().getYear());

            if (instant.getPrice() != 0)
                series1.addOrUpdate(minute,  instant.getPrice());
        }

        return series1;
    }
}