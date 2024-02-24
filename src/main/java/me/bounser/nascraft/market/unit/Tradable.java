package me.bounser.nascraft.market.unit;

import java.util.UUID;

public interface Tradable {

    void buy(int amount, UUID uuid, boolean feedback);

    float sell(int amount, UUID uuid, boolean feedback);

}
