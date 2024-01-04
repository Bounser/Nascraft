package me.bounser.nascraft.sellwand;

import me.bounser.nascraft.config.Config;

import java.util.HashMap;

public class WandsManager {

    private HashMap<String, Wand> wands = new HashMap<>();

    public static WandsManager instance;

    public static WandsManager getInstance() { return instance == null ? instance = new WandsManager() : instance; }

    private WandsManager() {
        for (Wand wand : Config.getInstance().getWands()) {
            wands.put(wand.getName(), wand);
        }
    }

    public HashMap<String, Wand> getWands() { return wands; }

}
