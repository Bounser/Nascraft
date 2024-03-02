package me.bounser.nascraft.managers;

import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.market.unit.plot.GraphData;
import me.bounser.nascraft.market.unit.Item;

public class GraphManager {

    private final int width = 363, height = 126;
    private final int offsetX = 10, offsetY = 54;

    private int cycle = 0;

    private static GraphManager instance;

    public static GraphManager getInstance() { return instance == null ? instance = new GraphManager() : instance; }

    private GraphManager() { outdatedCollector(); }

    public int[] getSize() { return new int[]{width, height}; }
    public int[] getOffset() { return new int[]{offsetX, offsetY}; }

    public void outdatedCollector() {

        cycle++;
        for(Item item : MarketManager.getInstance().getAllItems()) {

            for(GraphData gd : item.getGraphData()) {

                switch (gd.getTimeSpan()) {
                    case HOUR:
                        gd.clear();
                        gd.setValues(item.getPrices(TimeSpan.HOUR));
                        gd.changeState();
                        break;
                    case DAY:
                        if(cycle%60==0) {
                            gd.clear();
                            gd.setValues(item.getPrices(TimeSpan.DAY));
                            gd.changeState();
                        }
                        break;
                    case MONTH:
                        if(cycle%(60*24)==0) {
                            gd.clear();
                            gd.setValues(item.getPrices(TimeSpan.DAY));
                            gd.changeState();
                        }
                        break;
                    case YEAR:
                        if(cycle%(60*24*365)==0) {
                            gd.clear();
                            gd.setValues(item.getPrices(TimeSpan.DAY));
                            gd.changeState();
                        }
                        break;
                }
            }
        }
    }
}
