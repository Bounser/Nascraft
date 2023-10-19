package me.bounser.nascraft.database;

import me.bounser.nascraft.market.unit.Item;

import java.time.LocalDateTime;

public class Trade {

    Item item;

    LocalDateTime date;

    float value;
    int amount;

    boolean buy;
    boolean discord;

    public Trade(Item item, LocalDateTime date, float value, int amount, boolean buy, boolean discord) {
        this.item = item;
        this.date = date;
        this.value = value;
        this.amount = amount;
        this.buy = buy;
        this.discord = discord;
    }

    public Item getItem() { return item; }

    public LocalDateTime getDate() { return date; }
    public float getValue() { return value; }

    public float getAmount() { return amount; }
    public boolean isBuy() { return buy; }

    public boolean throughDiscord() { return discord; }

}
