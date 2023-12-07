package me.bounser.nascraft.placeholderapi;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.formatter.RoundUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PAPIExpansion extends PlaceholderExpansion {

    @Override
    public String getAuthor() { return "Bounser"; }

    @Override
    public String getIdentifier() { return "nascraft"; }

    @Override
    public String getVersion() { return Nascraft.getInstance().getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {

        String params = PlaceholderAPI.setBracketPlaceholders(player, identifier);

        Item item;

        int quantity = 1;

        if (params.substring(0, params.indexOf("_")).equalsIgnoreCase("change")) {

            String argument = params.substring(params.indexOf("_", params.indexOf("_") + 1) + 1);

            if (argument.equalsIgnoreCase("mainhand")) {
                item = MarketManager.getInstance().getItem(player.getPlayer().getInventory().getItemInMainHand().getType().toString());
                quantity = player.getPlayer().getInventory().getItemInMainHand().getAmount();
            } else {
                item = MarketManager.getInstance().getItem(argument);
            }

        } else if (params.substring(params.indexOf("_") + 1).equalsIgnoreCase("mainhand")) {

            item = MarketManager.getInstance().getItem(player.getPlayer().getInventory().getItemInMainHand().getType().toString());
            quantity = player.getPlayer().getInventory().getItemInMainHand().getAmount();

        } else {

            item = MarketManager.getInstance().getItem(params.substring(params.indexOf("_") + 1));
        }

        if (item == null) return "0.00";

        TimeSpan timeSpan = null;

        switch (params.substring(0, params.indexOf("_")).toLowerCase()) {

            case "buyprice": return String.valueOf(RoundUtils.round(item.getPrice().getBuyPrice()*quantity));
            case "sellprice": return String.valueOf(RoundUtils.round(item.getPrice().getSellPrice()*quantity));
            case "price": return String.valueOf(RoundUtils.round(item.getPrice().getValue()*quantity));
            case "stock": return String.valueOf(item.getPrice().getStock());
            case "change":

                switch (params.substring(params.indexOf("_") + 1, params.indexOf("_", params.indexOf("_") + 1)).toLowerCase()) {

                    case "1h": timeSpan = TimeSpan.HOUR; break;
                    case "1d": timeSpan = TimeSpan.DAY; break;
                    case "1m": timeSpan = TimeSpan.MONTH; break;
                    case "1y": timeSpan = TimeSpan.YEAR; break;
                }

                if(timeSpan != null)
                    return String.valueOf(RoundUtils.roundToOne(-100 + item.getPrice().getValue() *100/item.getPrices(timeSpan).get(0)));

            default: return "0.00";
        }
    }
}
