package me.bounser.nascraft.formatter;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RoundUtils {

    public static float round(Number value) {
        double number = value.doubleValue();
        
        if (!Double.isFinite(number)) {
            return 0.0f;
        }
        
        BigDecimal bd = new BigDecimal(number);
        bd = bd.setScale(3, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float preciseRound(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(5, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float roundToOne(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float roundToOne(double value) {
        if (!Double.isFinite(value)) {
            return 0.0f;
        }
        
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float roundToTwo(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float roundTo(Number value, int precision) {
        double number = value.doubleValue();
        
        if (!Double.isFinite(number)) {
            return 0.0f;
        }
        
        BigDecimal bd = new BigDecimal(number);
        bd = bd.setScale(precision, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

}
