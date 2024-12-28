package me.bounser.nascraft.chart.portfolio;

import me.bounser.nascraft.database.Database;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.commands.resources.NormalisedDate;
import me.bounser.nascraft.portfolio.Portfolio;
import org.bukkit.Bukkit;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleInsets;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.UUID;

public class PortfolioEvolutionChart {

    public static BufferedImage getImage(Portfolio portfolio, int width, int height) {

        if (portfolio == null) return new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

        return createChart(portfolio).createBufferedImage(width, height);
    }

    private static JFreeChart createChart(Portfolio portfolio) {

        TimeSeries contributionSeries = createContributionDataset(portfolio.getOwnerUUID());
        TimeSeries realValueSeries = createRealValueDataset(portfolio.getOwnerUUID());

        TimeSeriesCollection contributionDataset = new TimeSeriesCollection(contributionSeries);
        TimeSeriesCollection realValueDataset = new TimeSeriesCollection(realValueSeries);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,
                null,
                null,
                contributionDataset,
                false,
                false,
                false
        );

        XYPlot plot = chart.getXYPlot();

        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        dateAxis.setVisible(false);
        rangeAxis.setVisible(false);

        plot.setBackgroundPaint(null);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        XYStepRenderer stepRenderer = new XYStepRenderer();
        float[] dotPattern = new float[] { 1.0f, 2.0f };
        stepRenderer.setSeriesPaint(0, Color.BLACK);
        stepRenderer.setSeriesStroke(0, new BasicStroke(
                1f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND,
                1.0f,
                dotPattern,
                0.0f
        ));

        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer();
        lineRenderer.setSeriesPaint(0, Color.BLUE);
        lineRenderer.setSeriesShapesVisible(0, false);
        lineRenderer.setSeriesStroke(0, new BasicStroke(1f));

        plot.setDataset(0, contributionDataset);
        plot.setRenderer(0, stepRenderer);

        plot.setDataset(1, realValueDataset);
        plot.setRenderer(1, lineRenderer);

        plot.setInsets(new RectangleInsets(10, 0, 0, 0), true);

        chart.setPadding(new RectangleInsets(0, 0, 0, 0));
        plot.setOutlineVisible(false);

        chart.setBackgroundPaint(null);

        return chart;
    }

    private static TimeSeries createContributionDataset(UUID uuid) {

        TimeSeries series = new TimeSeries("Contributions");

        HashMap<Integer, Double> contributions = DatabaseManager.get().getDatabase().getContributionChangeEachDay(uuid);

        double lastContribution = 0;
        int lastDay = 0;

        int firstDay = DatabaseManager.get().getDatabase().getFirstDay(uuid);

        series.addOrUpdate(new Day(NormalisedDate.getDateFromDay(firstDay-1)), 0.0);

        for (int dayOfContribution : contributions.keySet()) {
            Day day = new Day(NormalisedDate.getDateFromDay(dayOfContribution));
            series.addOrUpdate(day, contributions.get(dayOfContribution));

            if (dayOfContribution > lastDay) {
                lastDay = dayOfContribution;
                lastContribution = contributions.get(dayOfContribution);
            }
        }

        if (!contributions.containsKey(NormalisedDate.getDays())) {
            Day day = new Day(NormalisedDate.getDateFromDay(NormalisedDate.getDays()));
            series.addOrUpdate(day, lastContribution);
        }

        series.getItems().forEach(item ->
                Bukkit.broadcastMessage(item.toString())
        );

        return series;
    }

    private static TimeSeries createRealValueDataset(UUID uuid) {

        TimeSeries series = new TimeSeries("Real Value");

        Database database = DatabaseManager.get().getDatabase();

        HashMap<Integer, HashMap<String, Integer>> composition = database.getCompositionEachDay(uuid);

        if (composition.isEmpty()) return series;

        HashMap<String, Integer> dayComposition = new HashMap<>();

        int firstDay = database.getFirstDay(uuid);

        series.addOrUpdate(new Day(NormalisedDate.getDateFromDay(firstDay-1)), 0.0);

        for (int i = firstDay ; i <= NormalisedDate.getDays() ; i++) {

            if (composition.get(i) != null)
                dayComposition = composition.get(i);

            double worth = 0;

            for (String identifier : dayComposition.keySet()) {
                worth += database.getPriceOfDay(identifier, i) * dayComposition.get(identifier);
            }

            Day day = new Day(NormalisedDate.getDateFromDay(i));

            series.addOrUpdate(day, worth);
        }

        series.getItems().forEach(item ->
                Bukkit.broadcastMessage(item.toString())
        );

        return series;
    }

}
