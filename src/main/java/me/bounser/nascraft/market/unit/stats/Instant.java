package me.bounser.nascraft.market.unit.stats;

import java.time.LocalDateTime;

public class Instant {

    private LocalDateTime localDateTime;
    private double price;
    private int volume;

    public Instant(LocalDateTime localDateTime, double price, int volume) {
        this.localDateTime = localDateTime;
        this.price = price;
        this.volume = volume;
    }

    public double getPrice() { return price; }

    public int getVolume() { return volume; }

    public LocalDateTime getLocalDateTime() { return localDateTime; }
}
