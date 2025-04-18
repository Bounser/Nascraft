package me.bounser.nascraft.web.dto;

public class TimeSeriesDTO {

    private long time;
    private double value;

    public TimeSeriesDTO(long time, double value) {
        this.time = time;
        this.value = value;
    }

    public long getTime() {
        return time;
    }

    public double getValue() {
        return value;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setValue(double value) {
        this.value = value;
    }

}
