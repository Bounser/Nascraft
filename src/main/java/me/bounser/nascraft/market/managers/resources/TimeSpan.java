package me.bounser.nascraft.market.managers.resources;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public enum TimeSpan {

    MINUTE, // 30 minutes
    DAY, // 1 day
    MONTH, // 1 month
    YEAR; // 1 year

    public static String getTime(TimeSpan timeFrame, float substract) {

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("mm");

        switch (timeFrame) {
            // 30 min
            case MINUTE:
                sdf = new SimpleDateFormat("HH:mm");
                cal.add(Calendar.MINUTE, Math.round(-30 * substract));
                break;
            // 1 day
            case DAY:
                sdf = new SimpleDateFormat("HH:mm");
                cal.add(Calendar.HOUR, Math.round(-24 * substract));
                break;
            // 1 Month
            case MONTH:
                sdf = new SimpleDateFormat("dd MMMM");
                cal.add(Calendar.DATE, Math.round(-30 * substract));
                break;
            // 1 Year
            case YEAR:
                sdf = new SimpleDateFormat("dd/MM/yyyy");
                cal.add(Calendar.DATE, Math.round(-365 * substract));
                break;
        }
        return sdf.format(cal.getTime());
    }
}
