package me.bounser.nascraft.placeholderapi;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.resources.TimeSpan;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.RoundUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PAPIExpansion extends PlaceholderExpansion {

    private final Nascraft main;

    public PAPIExpansion(Nascraft main) { this.main = main; }

    @Override
    public String getAuthor() { return "Bounser"; }

    @Override
    public String getIdentifier() { return "nascraft"; }

    @Override
    public String getVersion() { return "1.3.3"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String identifier) {

        // Parses placeholders between braces
        String params = PlaceholderAPI.setBracketPlaceholders(player, identifier);

        Item item;

        if (params.substring(0, params.indexOf("_")).equalsIgnoreCase("change")) {

            item = MarketManager.getInstance().getItem(params.substring(params.indexOf("_", params.indexOf("_") + 1) + 1));

        } else if(params.substring(params.indexOf("_") + 1).equalsIgnoreCase("mainhand")) {

            item = MarketManager.getInstance().getItem(player.getPlayer().getInventory().getItemInMainHand().getType().toString());

        } else {

            item = MarketManager.getInstance().getItem(params.substring(params.indexOf("_") + 1));
        }

        if (item == null) {
            return "0.00";
        } else {
            TimeSpan timeSpan = null;

            switch (params.substring(0, params.indexOf("_")).toLowerCase()) {

                case "buyprice": return String.valueOf(item.getPrice().getBuyPrice());
                case "sellprice": return String.valueOf(item.getPrice().getSellPrice());
                case "price": return String.valueOf(item.getPrice().getValue());
                case "stock": return String.valueOf(item.getPrice().getStock());
                case "change":
                    switch (params.substring(params.indexOf("_") + 1, params.indexOf("_", params.indexOf("_") + 1)).toLowerCase()) {

                        case "30m": timeSpan = TimeSpan.MINUTE; break;
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

}
