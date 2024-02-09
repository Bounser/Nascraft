package me.bounser.nascraft.discord;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;

import java.util.HashMap;

public class DiscordAlerts {

    private HashMap<String, HashMap<Item, Float>> alerts = new HashMap<>();

    private static DiscordAlerts instance;

    public static DiscordAlerts getInstance() { return instance == null ? instance = new DiscordAlerts() : instance; }


    public String setAlert(String userID, String identifier, Float price) {

        Item item = MarketManager.getInstance().getItem(identifier);

        if (item == null) return "not_valid";

        if (alerts.containsKey(userID) && alerts.get(userID).size() > 20) return "limit_reached";

        if (alerts.containsKey(userID) && alerts.get(userID).containsKey(item)) return "repeated";

        HashMap<Item, Float> content;
        if (alerts.get(userID) == null) content = new HashMap<>();
        else content = alerts.get(userID);

        if (price < item.getPrice().getValue()) content.put(item, -price);
        else content.put(item, price);

        alerts.put(userID, content);

        return "success";
    }

    public String removeAlert(String userID, Item item) {

        HashMap<Item, Float> content = alerts.get(userID);

        if (content == null ||!content.containsKey(item)) {
            return "not_found";
        }

        content.remove(item);

        alerts.put(userID, content);
        return "success";
    }

    public void updateAlerts() {

        for (String userID : alerts.keySet()) {

            for (Item item : alerts.get(userID).keySet()) {

                if (alerts.get(userID).get(item) < 0) {

                    if (!(item.getPrice().getValue() < Math.abs(alerts.get(userID).get(item)))) return;

                    reachedMessage(userID, item, Math.abs(alerts.get(userID).get(item)), ":chart_with_downwards_trend:" );
                    alerts.get(userID).remove(item);

                } else {

                    if (!(item.getPrice().getValue() > alerts.get(userID).get(item))) return;

                    reachedMessage(userID, item, Math.abs(alerts.get(userID).get(item)), ":chart_with_upwards_trend:" );
                    alerts.get(userID).remove(item);
                }
            }
        }
    }

    public HashMap<String, HashMap<Item, Float>> getAlerts() { return alerts; }

    public void reachedMessage(String userId, Item item, float price, String emoji) {

        DiscordBot.getInstance().getJDA().retrieveUserById(userId).queue(user ->
                user.openPrivateChannel()
                        .queue(privateChannel -> privateChannel
                                .sendMessage(Lang.get().message(Message.DISCORD_ALERT_REACHED_SEGMENT)
                                        .replace("[EMOJI]", emoji)
                                        .replace("[MATERIAL]", item.getName())
                                        .replace("[PRICE1]", Formatter.format(price, Style.ROUND_BASIC))
                                        .replace("[PRICE2]", Formatter.format(item.getPrice().getValue(), Style.ROUND_BASIC)))
                                .queue()));

    }

}
