package me.bounser.nascraft.tools;

import me.bounser.nascraft.Nascraft;

public class Config {

    private static Config instance;
    private static Nascraft main;

    public static Config getInstance(){
        if(instance == null){
            main = Nascraft.getInstance();

            main.getConfig().options().copyDefaults();
            main.saveDefaultConfig();
            instance = new Config;

            return instance;
        }
        return instance;
    }

    public Boolean getCheckResources(){ return main.getConfig().getBoolean("AutoResourcesInjection"); }

    public boolean getDebug(){ return main.getConfig().getBoolean("Debug"); }



}

