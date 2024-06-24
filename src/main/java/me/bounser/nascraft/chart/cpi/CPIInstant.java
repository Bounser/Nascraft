package me.bounser.nascraft.chart.cpi;

import java.time.LocalDateTime;

public class CPIInstant {

    private float indexValue;

    private LocalDateTime localDateTime;

    public CPIInstant(float indexValue, LocalDateTime localDateTime) {
        this.indexValue = indexValue;
        this.localDateTime = localDateTime;
    }

    public float getIndexValue() {
        return indexValue;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

}
