package me.bounser.nascraft.portfolio;

import me.bounser.nascraft.discord.linking.LinkManager;

import java.util.HashMap;
import java.util.UUID;

public class PortfoliosManager {


    private final HashMap<UUID, Portfolio> inventories = new HashMap<>();

    private static PortfoliosManager instance;

    public static PortfoliosManager getInstance() { return instance == null ? instance = new PortfoliosManager() : instance; }

    public Portfolio getPortfolio(UUID uuid) {

        if (inventories.containsKey(uuid)) return inventories.get(uuid);

        inventories.put(uuid, new Portfolio(uuid));

        return inventories.get(uuid);
    }

    public Portfolio getPortfolio(String userid) {

        UUID uuid = LinkManager.getInstance().getUUID(userid);

        if (inventories.containsKey(uuid)) return inventories.get(uuid);

        inventories.put(uuid, new Portfolio(uuid));

        return inventories.get(uuid);
    }

}
