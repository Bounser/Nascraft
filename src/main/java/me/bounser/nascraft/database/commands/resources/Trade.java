package me.bounser.nascraft.database.commands.resources;

import me.bounser.nascraft.market.unit.Item;

import java.time.LocalDateTime;
import java.util.UUID;

public class Trade {

    private Item item;

    private LocalDateTime date;

    private double value;
    private int amount;

    private boolean buy;
    private boolean discord;

    private UUID uuid;

    public Trade(Item item, LocalDateTime date, double value, int amount, boolean buy, boolean discord, UUID uuid) {
        this.item = item;
        this.date = date;
        this.value = value;
        this.amount = amount;
        this.buy = buy;
        this.discord = discord;
        this.uuid = uuid;
    }

    public Item getItem() { return item; }

    public UUID getUuid() { return uuid; }

    public LocalDateTime getDate() { return date; }

    public double getValue() { return value; }

    public int getAmount() { return amount; }

    public boolean isBuy() { return buy; }

    public boolean throughDiscord() { return discord; }

}
