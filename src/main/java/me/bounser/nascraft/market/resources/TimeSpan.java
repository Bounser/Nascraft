package me.bounser.nascraft.market.resources;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public enum TimeSpan {

    HOUR, // 60 minutes
    DAY; // 24 hours

    public static String getTime(TimeSpan timeFrame, float subtract) {

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("mm");

        switch (timeFrame) {
            // 30 min
            case HOUR:
                sdf = new SimpleDateFormat("HH:mm");
                cal.add(Calendar.MINUTE, Math.round(-60 * subtract));
                break;
            // 1 day
            case DAY:
                sdf = new SimpleDateFormat("HH:mm");
                cal.add(Calendar.HOUR, Math.round(-24 * subtract));
                break;
        }
        return sdf.format(cal.getTime());
    }
}
