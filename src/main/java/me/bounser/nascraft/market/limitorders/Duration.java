package me.bounser.nascraft.market.limitorders;

public class Duration {

    private int days;
    private String display;
    private float fee;
    private float minimumFee;

    public Duration(int days, String display, float fee, float minimumFee) {
        this.days = days;
        this.display = display;
        this.fee = fee;
        this.minimumFee = minimumFee;
    }

    public int getDurationInDays() {
        return days;
    }

    public String getDisplay() {
        return display;
    }

    public float getFee() {
        return fee;
    }

    public float getMinimumFee() {
        return minimumFee;
    }

}
