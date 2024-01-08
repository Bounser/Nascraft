package me.bounser.nascraft.market.unit;

import me.bounser.nascraft.database.SQLite;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemStats {

    private List<Instant> dataMinute = new ArrayList<>();

    private List<Instant> dataDay = new ArrayList<>();

    private List<Instant> dataMonth = new ArrayList<>();

    private List<Instant> dataYear = new ArrayList<>();

    private Item item;

    public ItemStats(Item item) { this.item = item; }

    public void addInstant(Instant instant) {

        dataMinute.add(instant); // Each minute

        if (dataMinute.size() % 5 == 0) {

            while (dataMinute.size() > 60)  dataMinute.remove(0);

            Instant dayInstant = new Instant(
                    getLocalDateTimeBetween(LocalDateTime.now(), dataMinute.get(dataMinute.size()-5).getLocalDateTime()),
                    priceAverage(dataMinute.subList(dataMinute.size()-5, dataMinute.size()-1)),
                    volumeAdder(dataMinute.subList(dataMinute.size()-5, dataMinute.size()-1)));

            dataDay.add(dayInstant); // Each five minutes

            SQLite.getInstance().saveDayPrice(item, dayInstant);

            while (dataDay.size() > 288)  dataDay.remove(0);

            if (dataDay.size() % 32 == 0) {

                Instant monthInstant = new Instant(
                        getLocalDateTimeBetween(LocalDateTime.now(), dataDay.get(dataDay.size()-6).getLocalDateTime()),
                        priceAverage(dataDay.subList(dataDay.size()-6, dataDay.size()-1)),
                        volumeAdder(dataDay.subList(dataDay.size()-6, dataDay.size()-1)));

                dataMonth.add(monthInstant); // Each 160 minutes

                SQLite.getInstance().saveMonthPrice(item, monthInstant);

                while (dataMonth.size() > 31*9)  dataMonth.remove(0);

                if (dataMonth.size() % 9 == 0) {

                    dataYear.add(new Instant(
                            getLocalDateTimeBetween(LocalDateTime.now(), dataMonth.get(dataMonth.size()-12).getLocalDateTime()),
                            priceAverage(dataDay.subList(dataDay.size()-12, dataDay.size()-1)),
                            volumeAdder(dataDay.subList(dataDay.size()-12, dataDay.size()-1))));

                    SQLite.getInstance().saveHistoryPrices(item, monthInstant); // Each day

                }
            }
        }
    }

    public float priceAverage(List<Instant> instants) {
        if (instants.isEmpty())
            return 0;

        float price = 0;
        for (Instant instantPrice : instants) {
            price += instantPrice.getPrice();
        }

        return price / instants.size();
    }

    public int volumeAdder(List<Instant> instants) {
        if (instants.isEmpty())
            return 0;

        int volume = 0;
        for (Instant instantVolume : instants) {
            volume += instantVolume.getVolume();
        }

        return volume;
    }

    public LocalDateTime getLocalDateTimeBetween(LocalDateTime fecha1, LocalDateTime fecha2) {
        Duration diferencia = Duration.between(fecha1, fecha2);

        Duration mitadDiferencia = diferencia.dividedBy(2);

        LocalDateTime menorFecha = (fecha1.isBefore(fecha2)) ? fecha1 : fecha2;

        return menorFecha.plus(mitadDiferencia);
    }

}
