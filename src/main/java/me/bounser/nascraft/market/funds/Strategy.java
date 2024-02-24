package me.bounser.nascraft.market.funds;

import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.market.unit.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public enum Strategy {

    CASH,
    EQUAL_WEIGHTED,
    PRICE_WEIGHTED,
    MINIMUM_VOLATILITY,
    HIGH_VOLATILITY,
    WINNERS,
    LOSERS,
    RANDOM;

    public static HashMap<Item, Float> getItems(Strategy strategy, int cap, float totalWeight) {

        HashMap<Item, Float> items = new HashMap<>();

        List<Item> allItems = new ArrayList<>(MarketManager.getInstance().getAllItems());

        switch (strategy) {

            case EQUAL_WEIGHTED:

                for (Item item : allItems){
                    items.put(item, totalWeight/allItems.size());
                    if (items.keySet().size() == cap) return items;
                }

                return items;

            case PRICE_WEIGHTED:

                float totalPrice = 0;

                for (Item item : allItems)
                    totalPrice += item.getPrice().getValue();

                for (Item item : allItems) {
                    items.put(item, (item.getPrice().getValue()/totalPrice) * totalWeight);
                    if (items.keySet().size() == cap) return items;
                }

                return items;

            case MINIMUM_VOLATILITY:

                for (int i = 0; i < cap; i++) {

                    float minVol = 999;
                    Item minVolItem = null;

                    for (Item item : allItems) {

                        float volatility = Math.abs(RoundUtils.roundToOne(-1 + item.getPrice().getValue()/item.getPrices(TimeSpan.HOUR).get(0)));

                        if (minVol > volatility) {
                            minVolItem = item;
                            minVol = volatility;
                        }
                    }

                    items.put(minVolItem, totalWeight/cap);
                    allItems.remove(minVolItem);
                }

                return items;

            case HIGH_VOLATILITY:

                for (int i = 0; i < cap; i++) {

                    float highVol = 999;
                    Item highVolItem = null;

                    for (Item item : allItems) {

                        float volatility = Math.abs(RoundUtils.roundToOne(-1 + item.getPrice().getValue()/item.getPrices(TimeSpan.HOUR).get(0)));

                        if (highVol > volatility) {
                            highVolItem = item;
                            highVol = volatility;
                        }
                    }

                    items.put(highVolItem, totalWeight/cap);
                    allItems.remove(highVolItem);
                }

                return items;

            case WINNERS:

                for (int i = 0; i < cap; i++) {

                    float highiestWin = -999;
                    Item winnerItem = null;

                    for (Item item : allItems) {

                        float win = RoundUtils.roundToOne(-1 + item.getPrice().getValue()/item.getPrices(TimeSpan.HOUR).get(0));

                        if (highiestWin < win) {
                            winnerItem = item;
                            highiestWin = win;
                        }
                    }

                    items.put(winnerItem, totalWeight/cap);
                    allItems.remove(winnerItem);
                }

                return items;

            case LOSERS:

                for (int i = 0; i < cap; i++) {

                    float lowestWin = 999;
                    Item winnerItem = null;

                    for (Item item : allItems) {

                        float win = RoundUtils.roundToOne(-1 + item.getPrice().getValue()/item.getPrices(TimeSpan.HOUR).get(0));

                        if (lowestWin > win) {
                            winnerItem = item;
                            lowestWin = win;
                        }
                    }

                    items.put(winnerItem, totalWeight/cap);
                    allItems.remove(winnerItem);
                }

                return items;

            case RANDOM:

                for (int i = 0; i < cap; i++) {

                    Random random = new Random();
                    int randomIndex = random.nextInt(allItems.size());

                    items.put(allItems.get(randomIndex), totalWeight/cap);
                }

                return items;
        }

        return items;
    }

}
