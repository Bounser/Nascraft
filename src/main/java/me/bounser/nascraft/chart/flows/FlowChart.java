package me.bounser.nascraft.chart.flows;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.commands.resources.DayInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.VerticalAlignment;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

public class FlowChart {

    private static IntervalXYDataset dataset1;
    private static IntervalXYDataset dataset2;

    public static BufferedImage getImage(int width, int height) {
        return createChart().createBufferedImage(width, height);
    }

    private static JFreeChart createChart() {

        createDatasets();

        IntervalXYDataset data1 = dataset1;
        XYAreaRenderer  renderer1 = new XYAreaRenderer(XYAreaRenderer.AREA);
        renderer1.setBaseToolTipGenerator(
                new StandardXYToolTipGenerator(
                        StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                        new SimpleDateFormat("d-MMM-yyyy"),
                        new DecimalFormat("0.00")));

        renderer1.setSeriesPaint(0, new Color(150, 255, 215, 130));
        renderer1.setSeriesStroke(0, new BasicStroke(1.5f));
        renderer1.setSeriesPaint(1, new Color(250, 100, 100, 130));
        renderer1.setSeriesStroke(1, new BasicStroke(1.5f));

        DateAxis domainAxis = new DateAxis("");
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.02);
        ValueAxis rangeAxis = new NumberAxis(Lang.get().message(Message.DISCORD_FLOW_Y_1));
        XYPlot plot1 = new XYPlot(data1, null, rangeAxis, renderer1);

        plot1.setDomainGridlinePaint(Color.WHITE);
        plot1.setRangeGridlinesVisible(true);
        plot1.setRangeGridlinePaint(Color.WHITE);

        plot1.setBackgroundPaint(new Color(30,32,32));
        plot1.setDomainGridlinePaint(Color.WHITE);
        plot1.setRangeGridlinePaint(Color.WHITE);

        IntervalXYDataset data2 = dataset2;
        XYBarRenderer renderer2 =
                new XYBarRenderer() {
                    public Paint getItemPaint(int series, int item) {
                        XYDataset dataset = getPlot().getDataset();
                        if (dataset.getYValue(series, item) >= 0.0) {
                            return Color.green;
                        } else {
                            return Color.red;
                        }
                    }
                };
        renderer2.setSeriesPaint(0, Color.WHITE);
        renderer2.setSeriesStroke(0, new BasicStroke(1.5f));
        renderer2.setDrawBarOutline(false);
        renderer2.setBaseToolTipGenerator(
                new StandardXYToolTipGenerator(
                        StandardXYToolTipGenerator.DEFAULT_TOOL_TIP_FORMAT,
                        new SimpleDateFormat("d-MMM-yyyy"),
                        new DecimalFormat("0.00")));

        XYPlot plot2 = new XYPlot(data2, null, new NumberAxis(Lang.get().message(Message.DISCORD_FLOW_Y_2)), renderer2);

        plot2.setBackgroundPaint(new Color(30,32,32));
        plot2.setDomainGridlinePaint(Color.WHITE);
        plot2.setRangeGridlinePaint(Color.WHITE);

        CombinedDomainXYPlot cplot = new CombinedDomainXYPlot(domainAxis);
        cplot.add(plot1, 3);
        cplot.add(plot2, 2);
        cplot.setGap(8.0);
        cplot.setDomainGridlinePaint(Color.WHITE);
        cplot.setDomainGridlinesVisible(true);
        cplot.setDomainPannable(true);

        JFreeChart chart =
                new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, cplot, false);

        chart.setBackgroundPaint(new Color(0,0,0,0));

        plot2.getRangeAxis().setLabelPaint(new Color(255, 255, 255));
        plot2.getRangeAxis().setTickLabelPaint(new Color(255, 255, 255));

        rangeAxis.setLabelPaint(new Color(255, 255, 255));
        rangeAxis.setTickLabelPaint(new Color(255, 255, 255));

        renderer2.setBarPainter(new StandardXYBarPainter());
        renderer2.setShadowVisible(false);

        domainAxis.setTickLabelPaint(Color.WHITE);
        domainAxis.setAxisLinePaint(Color.WHITE);
        domainAxis.setLabelPaint(Color.WHITE);
        domainAxis.setTickMarkPaint(Color.WHITE);

        LegendTitle legend = new LegendTitle(plot1);
        legend.setPosition(RectangleEdge.BOTTOM);
        legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
        legend.setVerticalAlignment(VerticalAlignment.BOTTOM);
        legend.setBackgroundPaint(new Color(0, 0, 0, 0));
        legend.setItemPaint(new Color(255, 255, 255));

        XYTitleAnnotation ta = new XYTitleAnnotation(0.01, 0.01, legend, RectangleAnchor.BOTTOM_LEFT);
        plot1.addAnnotation(ta);

        return chart;
    }


    public static void createDatasets() {

        TimeSeries series1 = new TimeSeries(Lang.get().message(Message.DISCORD_FLOW_LEGEND_1));
        TimeSeries series2 = new TimeSeries("Variation");
        TimeSeries series3 = new TimeSeries(Lang.get().message(Message.DISCORD_FLOW_LEGEND_2));

        float value = 0;
        float taxes = 0;

        for (DayInfo info : DatabaseManager.get().getDatabase().getDayInfos()) {

            value += info.getFlow();
            taxes += info.getTax();

            LocalDateTime time = info.getTime();

            Day day = new Day(time.getDayOfMonth(), time.getMonthValue(), time.getYear());

            series1.add(day, value);

            series2.add(day, info.getFlow());

            series3.add(day, taxes);

        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series1);
        dataset.addSeries(series3);
        dataset1 = dataset;

        TimeSeriesCollection dataset3 = new TimeSeriesCollection();
        dataset3.addSeries(series2);
        dataset2 = dataset3;
    }
}
