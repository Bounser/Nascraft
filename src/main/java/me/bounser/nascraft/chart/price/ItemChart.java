package me.bounser.nascraft.chart.price;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import me.bounser.nascraft.database.SQLite;
import me.bounser.nascraft.market.unit.Instant;
import me.bounser.nascraft.market.unit.Item;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.time.Day;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;

import java.util.List;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ItemChart {

    public static BufferedImage getImage(Item item, ChartType chartType, int width, int height) {
        return createChart(item, chartType).createBufferedImage(width, height);
    }

    private static JFreeChart createChart(Item item, ChartType chartType) {

        SimpleDateFormat simpleDateFormat;
        List<Instant> data;

        switch (chartType) {

            case DAY:
                simpleDateFormat = new SimpleDateFormat("HH:mm");
                data = SQLite.getInstance().getDayPrices(item);
                break;
            case MONTH:
                simpleDateFormat = new SimpleDateFormat("d-MMM");
                data = SQLite.getInstance().getMonthPrices(item);
                break;
            case YEAR:
                simpleDateFormat = new SimpleDateFormat("d-MMM-yyyy");
                data = SQLite.getInstance().getYearPrices(item);
                break;
            case ALL:
                simpleDateFormat = new SimpleDateFormat("d-MMM-yyyy");
                data = SQLite.getInstance().getAllPrices(item);
                break;

            default:
                simpleDateFormat = new SimpleDateFormat("HH:mm");
                data = SQLite.getInstance().getDayPrices(item);

        }

        XYDataset priceData = createPriceDataset(data, chartType);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                null,
                "Price ($)",
                priceData,
                false,
                false,
                false
        );

        XYPlot plot = (XYPlot) chart.getPlot();

        plot.setDomainGridlinePaint(new Color(255, 255, 255));
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(new Color(255, 255, 255));

        NumberAxis rangeAxis1 = (NumberAxis) plot.getRangeAxis();
        rangeAxis1.setLowerMargin(0.40);
        DecimalFormat format = new DecimalFormat("0.###");
        rangeAxis1.setNumberFormatOverride(format);

        XYItemRenderer renderer1 = plot.getRenderer();
        renderer1.setBaseToolTipGenerator(new StandardXYToolTipGenerator(
                StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                simpleDateFormat, new DecimalFormat("0.###")));

        renderer1.setSeriesPaint(0, new Color(100, 200, 255));

        NumberAxis rangeAxis2 = new NumberAxis("Volume");

        rangeAxis2.setUpperMargin(1.00);  // to leave room for price line
        plot.setRangeAxis(1, rangeAxis2);
        plot.setDataset(1, createVolumeDataset(data, chartType));
        plot.setRangeAxis(1, rangeAxis2);
        plot.mapDatasetToRangeAxis(1, 1);
        XYBarRenderer renderer2 = new XYBarRenderer(0.20);
        renderer2.setBaseToolTipGenerator(
                new StandardXYToolTipGenerator(
                        StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                        simpleDateFormat,
                        new DecimalFormat("0,000.##")));

        DecimalFormat format2 = new DecimalFormat("##.###");
        rangeAxis1.setNumberFormatOverride(format2);

        plot.setRenderer(1, renderer2);
        ChartUtilities.applyCurrentTheme(chart);
        renderer2.setBarPainter(new StandardXYBarPainter());
        renderer2.setShadowVisible(false);

        renderer1.setSeriesStroke(0, new BasicStroke(1.5f));

        plot.setBackgroundPaint(new Color(30,32,32));
        chart.setBackgroundPaint(new Color(0,0,0,0));

        rangeAxis1.setLabelPaint(new Color(255, 255, 255));
        rangeAxis1.setTickLabelPaint(new Color(255, 255, 255));

        rangeAxis2.setLabelPaint(new Color(255, 255, 255));
        rangeAxis2.setTickLabelPaint(new Color(255, 255, 255));

        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();

        dateAxis.setTickLabelPaint(new Color(255, 255, 255));
        dateAxis.setAxisLinePaint(new Color(255, 255, 255));
        dateAxis.setLabelPaint(new Color(255, 255, 255));
        dateAxis.setTickMarkPaint(new Color(255, 255, 255));

        return chart;
    }

    private static XYDataset createPriceDataset(List<Instant> intants, ChartType chartType) {

        TimeSeries series1 = new TimeSeries("Price");

        for (Instant instant : intants) {
            Minute minute = new Minute(instant.getLocalDateTime().getMinute(),
                    instant.getLocalDateTime().getHour(),
                    instant.getLocalDateTime().getDayOfMonth(),
                    instant.getLocalDateTime().getMonthValue(),
                    instant.getLocalDateTime().getYear());

            series1.addOrUpdate(minute,  instant.getPrice() == 0 ? null : instant.getPrice());
        }

        return new TimeSeriesCollection(series1);

    }

    private static IntervalXYDataset createVolumeDataset(List<Instant> intants, ChartType chartType) {

        TimeSeries series1 = new TimeSeries("Volume");

        switch (chartType) {

            case DAY:
            case MONTH:

                for (Instant instant : intants) {
                    Minute time = new Minute(instant.getLocalDateTime().getMinute(),
                            instant.getLocalDateTime().getHour(),
                            instant.getLocalDateTime().getDayOfMonth(),
                            instant.getLocalDateTime().getMonthValue(),
                            instant.getLocalDateTime().getYear());

                    series1.addOrUpdate(time,  instant.getVolume());
                }
                break;

            case YEAR:
            case ALL:

                for (Instant instant : intants) {
                    Day time = new Day(
                            instant.getLocalDateTime().getDayOfMonth(),
                            instant.getLocalDateTime().getMonthValue(),
                            instant.getLocalDateTime().getYear());

                    series1.addOrUpdate(time,  instant.getVolume());
                }
                break;

        }

        return new TimeSeriesCollection(series1);

    }

}