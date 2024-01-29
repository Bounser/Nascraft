package me.bounser.nascraft.market.unit;

import java.time.LocalDateTime;

public class Instant {

    private LocalDateTime localDateTime;
    private float price;
    private int volume;

    public Instant(LocalDateTime localDateTime, float price, int volume) {
        this.localDateTime = localDateTime;
        this.price = price;
        this.volume = volume;
    }

    public float getPrice() { return price; }

    public int getVolume() { return volume; }

    public LocalDateTime getLocalDateTime() { return localDateTime; }
}
