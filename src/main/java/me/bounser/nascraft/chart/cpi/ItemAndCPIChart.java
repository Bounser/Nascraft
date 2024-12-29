package me.bounser.nascraft.chart.cpi;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.unit.stats.Instant;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.VerticalAlignment;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class ItemAndCPIChart {

    public static BufferedImage getImage(int width, int height, Item item) {
        return createChart(item).createBufferedImage(width, height);
    }

    private static JFreeChart createChart(Item item) {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d-MMM-yyyy");
        List<CPIInstant> data = DatabaseManager.get().getDatabase().getCPIHistory();

        XYDataset priceData = createPriceDataset(data, item);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                null,
                Lang.get().message(Message.DISCORD_CPI_VS_ITEM).replace("[NAME]", item.getName()),
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
        DecimalFormat format = new DecimalFormat("0.###");
        rangeAxis1.setNumberFormatOverride(format);

        XYItemRenderer renderer1 = plot.getRenderer();
        renderer1.setBaseToolTipGenerator(new StandardXYToolTipGenerator(
                StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                simpleDateFormat, new DecimalFormat("0.###")));

        renderer1.setSeriesPaint(0, new Color(100, 200, 255));
        renderer1.setSeriesPaint(1, new Color(255, 100, 100));

        plot.setRenderer(renderer1);

        renderer1.setSeriesStroke(0, new BasicStroke(1.5f));
        renderer1.setSeriesStroke(1, new BasicStroke(1.5f));

        plot.setBackgroundPaint(new Color(30, 32, 32));
        chart.setBackgroundPaint(new Color(0, 0, 0, 0));

        rangeAxis1.setLabelPaint(new Color(255, 255, 255));
        rangeAxis1.setTickLabelPaint(new Color(255, 255, 255));

        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();

        dateAxis.setTickLabelPaint(new Color(255, 255, 255));
        dateAxis.setAxisLinePaint(new Color(255, 255, 255));
        dateAxis.setLabelPaint(new Color(255, 255, 255));
        dateAxis.setTickMarkPaint(new Color(255, 255, 255));

        LegendTitle legend = new LegendTitle(plot);
        legend.setPosition(RectangleEdge.BOTTOM);
        legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
        legend.setVerticalAlignment(VerticalAlignment.BOTTOM);
        legend.setBackgroundPaint(new Color(0, 0, 0, 0));
        legend.setItemPaint(new Color(255, 255, 255));

        XYTitleAnnotation ta = new XYTitleAnnotation(0.01, 0.01, legend, RectangleAnchor.BOTTOM_LEFT);
        plot.addAnnotation(ta);

        return chart;
    }

    private static XYDataset createPriceDataset(List<CPIInstant> instants, Item item) {

        TimeSeries series1 = new TimeSeries(Lang.get().message(Message.DISCORD_CPI));
        TimeSeries series2 = new TimeSeries(item.getName());

        for (CPIInstant instant : instants) {
            Minute minute = new Minute(instant.getLocalDateTime().getMinute(),
                    instant.getLocalDateTime().getHour(),
                    instant.getLocalDateTime().getDayOfMonth(),
                    instant.getLocalDateTime().getMonthValue(),
                    instant.getLocalDateTime().getYear());
            series1.addOrUpdate(minute, instant.getIndexValue());
        }

        Instant firstInstant = null;

        List<Instant> itemInstants = DatabaseManager.get().getDatabase().getPriceAgainstCPI(item);

        for (Instant instant : itemInstants) {

            if (instant.getPrice() == 0) continue;

            if (firstInstant == null) firstInstant = instant;
            else {
                if (instant.getLocalDateTime().isBefore(firstInstant.getLocalDateTime())) {
                    firstInstant = instant;
                }
            }
        }

        if (firstInstant == null)
            return new TimeSeriesCollection(series1);

        double initial = firstInstant.getPrice();

        for (Instant instant : itemInstants) {

            if (instant.getPrice() == 0) continue;

            Minute minute = new Minute(instant.getLocalDateTime().getMinute(),
                    instant.getLocalDateTime().getHour(),
                    instant.getLocalDateTime().getDayOfMonth(),
                    instant.getLocalDateTime().getMonthValue(),
                    instant.getLocalDateTime().getYear());
            series2.addOrUpdate(minute, instant.getPrice()*100.0/initial);
        }

        Minute firstMinute = new Minute(firstInstant.getLocalDateTime().getMinute(),
                firstInstant.getLocalDateTime().getHour(),
                firstInstant.getLocalDateTime().getDayOfMonth(),
                firstInstant.getLocalDateTime().getMonthValue(),
                firstInstant.getLocalDateTime().getYear());
        series2.addOrUpdate(firstMinute, 100);

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series1);
        dataset.addSeries(series2);

        return dataset;
    }

}
