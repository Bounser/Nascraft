package me.bounser.nascraft.database.commands.resources;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DayInfo {

    private int day;

    private float flow;
    private float tax;

    public DayInfo(int day, float flow, float tax) {
        this.day = day;
        this.flow = flow;
        this.tax = tax;
    }

    public LocalDateTime getTime() {
        return LocalDate.of(2023, 1, 1).plusDays(day).atStartOfDay();
    }

    public float getFlow() { return flow; }

    public float getTax() { return tax; }
}
