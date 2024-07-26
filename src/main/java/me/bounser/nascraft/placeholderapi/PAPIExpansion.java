package me.bounser.nascraft.placeholderapi;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.market.unit.Item;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.formatter.RoundUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

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

        String[] dividedParams = params.split("_", 3);

        if (dividedParams.length == 0) return "Invalid format.";

        Item item;

        switch (dividedParams[0].toLowerCase()) {

            case "cpi":
                return String.valueOf(Math.round((MarketManager.getInstance().getConsumerPriceIndex()-100)*100.0)/100.0);

            case "linked":
                return String.valueOf(LinkManager.getInstance().getUserDiscordID(player.getUniqueId()) != null);

            case "discordid":
                String id = LinkManager.getInstance().getUserDiscordID(player.getUniqueId());
                if (id == null) return "Not linked";
                return LinkManager.getInstance().getUserDiscordID(player.getUniqueId());

            case "price":

                String[] dividedParamsPrice = params.split("_", 2);

                if (dividedParamsPrice.length != 2) return "Invalid format";

                item = getItemFromString(dividedParamsPrice[1], player.getPlayer());

                if (item == null) return "0";

                return String.valueOf(RoundUtils.roundTo(item.getPrice().getValue(), Config.getInstance().getPlaceholderPrecission()));

            case "stock":

                String[] dividedParamsStock = params.split("_", 2);

                if (dividedParamsStock.length != 2) return "Invalid format";

                item = getItemFromString(dividedParamsStock[1], player.getPlayer());

                if (item == null) return "0";

                return String.valueOf(RoundUtils.roundTo(item.getPrice().getStock(), Config.getInstance().getPlaceholderPrecission()));

            case "change":

                if (dividedParams.length != 2) return "Invalid format";

                item = getItemFromString(dividedParams[2], player.getPlayer());

                if (item == null) return "Invalid item";

                return String.valueOf(RoundUtils.roundToOne(-100 + item.getPrice().getValue() *100/item.getPrice().getValueAnHourAgo()));
        }

        if (dividedParams.length < 2) { return "Invalid format."; }

        int quantity;

        if (dividedParams[1].equalsIgnoreCase("mainhand")) {
            item = MarketManager.getInstance().getItem(player.getPlayer().getInventory().getItemInMainHand());
            quantity = player.getPlayer().getInventory().getItemInMainHand().getAmount();
        } else {
            if (dividedParams.length != 3) { return "Invalid format."; }
            item = MarketManager.getInstance().getItem(dividedParams[2]);
            try {
                quantity = Integer.parseInt(dividedParams[1]);
            } catch (NumberFormatException e) {
                return "Invalid quantity.";
            }
        }

        if (item == null) return "0";

        switch (dividedParams[0]) {
            case "buyprice": return String.valueOf(RoundUtils.roundTo(item.getPrice().getProjectedCost(-quantity, item.getPrice().getBuyTaxMultiplier()), Config.getInstance().getPlaceholderPrecission()));
            case "sellprice": return String.valueOf(RoundUtils.roundTo(item.getPrice().getProjectedCost(quantity, item.getPrice().getSellTaxMultiplier()), Config.getInstance().getPlaceholderPrecission()));
        }

        return "0";
    }

    public Item getItemFromString(String itemIdentifier, Player player) {

        Item item;

        if (itemIdentifier.equalsIgnoreCase("mainhand")) {
            item = MarketManager.getInstance().getItem(player.getInventory().getItemInMainHand());
        } else {
            item = MarketManager.getInstance().getItem(itemIdentifier);
        }
        return item;
    }
}
