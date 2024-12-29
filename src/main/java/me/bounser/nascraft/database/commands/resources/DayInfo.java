package me.bounser.nascraft.database.commands.resources;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DayInfo {

    private int day;

    private double flow;
    private double tax;

    public DayInfo(int day, double flow, double tax) {
        this.day = day;
        this.flow = flow;
        this.tax = tax;
    }

    public LocalDateTime getTime() {
        return LocalDate.of(2023, 1, 1).plusDays(day).atStartOfDay();
    }

    public double getFlow() { return flow; }

    public double getTax() { return tax; }
}
