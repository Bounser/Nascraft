package me.bounser.nascraft.market.managers;

import me.bounser.nascraft.market.managers.resources.TimeSpan;
import me.bounser.nascraft.market.unit.GraphData;
import me.bounser.nascraft.market.unit.Item;

public class GraphManager {

    private final int width = 370, height = 200;
    private final int offsetX = 370, offsetY = 200;

    private static GraphManager instance;

    public static GraphManager getInstance() {
        return instance == null ? instance = new GraphManager() : instance;
    }

    private GraphManager() {
        outdatedCollector();
    }

    public int[] getSize() {
        return new int[]{width, height};
    }
    public int[] getOffset() {
        return new int[]{offsetX, offsetY};
    }

    public void outdatedCollector() {

        for(Item item : MarketManager.getInstance().getAllItems()) {

            for(GraphData gd : item.getGraphData()) {

                switch (gd.getTimeSpan()) {
                    case MINUTE:
                        gd.clear();
                        gd.setValues(item.getPrices(TimeSpan.MINUTE));
                        gd.changeState();
                        break;
                    case DAY:
                        break;
                    case MONTH:
                        break;
                    case YEAR:
                        break;
                }
            }
        }
    }

}
