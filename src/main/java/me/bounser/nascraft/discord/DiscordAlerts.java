package me.bounser.nascraft.discord;

import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;

import java.util.HashMap;

public class DiscordAlerts {

    private HashMap<String, HashMap<Item, Float>> alerts = new HashMap<>();

    private static DiscordAlerts instance;

    public static DiscordAlerts getInstance() { return instance == null ? instance = new DiscordAlerts() : instance; }


    public String setAlert(String userID, String material, Float price) {

        Item item = MarketManager.getInstance().getItem(material.replace(" ", "_"));

        if (item == null) return "not_valid";

        if (alerts.containsKey(userID) && alerts.get(userID).containsKey(item)) return "repeated";

        HashMap<Item, Float> content;
        if (alerts.get(userID) == null) {
            content = new HashMap<>();
        } else {
            content = alerts.get(userID);
        }

        if (price < item.getPrice().getValue()) { content.put(item, -price); } else { content.put(item, price); }

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

                    DiscordBot.getInstance().getJDA().retrieveUserById(userID).queue(user ->
                            user.openPrivateChannel()
                                    .queue(privateChannel -> privateChannel
                                            .sendMessage("Alert reached (low) for item: " + item.getName() + " with price: " + item.getPrice().getValue()).queue()));

                    alerts.get(userID).remove(item);

                } else {

                    if (!(item.getPrice().getValue() > alerts.get(userID).get(item))) return;

                    DiscordBot.getInstance().getJDA().retrieveUserById(userID).queue(user ->
                            user.openPrivateChannel()
                                    .queue(privateChannel -> privateChannel
                                            .sendMessage("Alert reached (high) for item: " + item.getName() + " with price: " + item.getPrice().getValue()).queue()));


                    alerts.get(userID).remove(item);
                }
            }
        }
    }

    public HashMap<String, HashMap<Item, Float>> getAlerts() { return alerts; }
}
