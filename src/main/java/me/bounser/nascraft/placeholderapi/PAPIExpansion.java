package me.bounser.nascraft.placeholderapi;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.managers.MarketManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PAPIExpansion extends PlaceholderExpansion {

    private final Nascraft main;

    public PAPIExpansion(Nascraft main) {
        this.main = main;
    }

    @Override
    public String getAuthor() {
        return "Bounser";
    }

    @Override
    public String getIdentifier() {
        return "nascraft";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {

        Item item = MarketManager.getInstance().getItem(params.substring(params.indexOf("_") + 1));

        if (item == null) {
            return "Error: Material not recognized.";
        } else {
            switch (params.substring(0, params.indexOf("_"))) {

                case "buyprice": return String.valueOf(item.getPrice().getBuyPrice());
                case "sellprice": return String.valueOf(item.getPrice().getSellPrice());
                case "price": return String.valueOf(item.getPrice().getValue());
                case "stock": return String.valueOf(item.getPrice().getStock());

                default: return null;

            }
        }
    }

}
