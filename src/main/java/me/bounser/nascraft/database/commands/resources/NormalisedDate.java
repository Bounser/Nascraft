package me.bounser.nascraft.database.commands.resources;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class NormalisedDate {

    public static String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }

    public static LocalDateTime parseDateTime(String dateTimeString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(dateTimeString, formatter);
    }

    public static int getDays() {
        LocalDate startDate = LocalDate.of(2023, 1, 1);

        LocalDate currentDate = LocalDate.now();
        long daysDifference = ChronoUnit.DAYS.between(startDate, currentDate);
        int daysDifferenceInt = (int) daysDifference;

        return daysDifferenceInt;
    }

    public static Date getDateFromDay(int day) {

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        startDate.plusDays(day);

        return Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

}
