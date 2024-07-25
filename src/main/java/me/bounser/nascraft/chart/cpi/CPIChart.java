package me.bounser.nascraft.chart.cpi;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class CPIChart {

    public static BufferedImage getImage(int width, int height) {
        return createChart().createBufferedImage(width, height);
    }

    private static JFreeChart createChart() {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d-MMM-yyyy");
        List<CPIInstant> data = DatabaseManager.get().getDatabase().getCPIHistory();


        XYDataset priceData = createPriceDataset(data);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                null,
                Lang.get().message(Message.DISCORD_CPI_Y),
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

        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();

        dateAxis.setTickLabelPaint(new Color(255, 255, 255));
        dateAxis.setAxisLinePaint(new Color(255, 255, 255));
        dateAxis.setLabelPaint(new Color(255, 255, 255));
        dateAxis.setTickMarkPaint(new Color(255, 255, 255));

        return chart;
    }

    private static XYDataset createPriceDataset(List<CPIInstant> intants) {

        TimeSeries series1 = new TimeSeries("Price");

        for (CPIInstant instant : intants) {
            Minute minute = new Minute(instant.getLocalDateTime().getMinute(),
                    instant.getLocalDateTime().getHour(),
                    instant.getLocalDateTime().getDayOfMonth(),
                    instant.getLocalDateTime().getMonthValue(),
                    instant.getLocalDateTime().getYear());

            series1.addOrUpdate(minute,  instant.getIndexValue());
        }

        return new TimeSeriesCollection(series1);
    }

}