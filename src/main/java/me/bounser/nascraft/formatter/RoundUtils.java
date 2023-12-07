package me.bounser.nascraft.formatter;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RoundUtils {

    public static float round(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(3, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float preciseRound(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(5, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float roundToOne(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float roundToTwo(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

}
