package me.bounser.nascraft.market.unit.stats;

import me.bounser.nascraft.database.sqlite.SQLite;
import me.bounser.nascraft.market.unit.Item;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemStats {

    private List<Instant> dataMinute = new ArrayList<>();

    private List<Instant> dataDay = new ArrayList<>();


    private Item item;

    public ItemStats(Item item) { this.item = item; }

    public void addInstant(Instant instant) {

        dataMinute.add(instant);

        if (dataMinute.size() % 5 == 0) {

            while (dataMinute.size() > 60)  dataMinute.remove(0);

            Instant dayInstant = new Instant(
                    getLocalDateTimeBetween(LocalDateTime.now(), dataMinute.get(dataMinute.size()-5).getLocalDateTime()),
                    priceAverage(dataMinute.subList(dataMinute.size()-5, dataMinute.size()-1)),
                    volumeAdder(dataMinute.subList(dataMinute.size()-5, dataMinute.size()-1)));

            dataDay.add(dayInstant);

            SQLite.getInstance().saveDayPrice(item, dayInstant);

            while (dataDay.size() > 288)  dataDay.remove(0);

            // if (dataDay.size() < 12) return;

            Instant bigDayInstant = new Instant(
                    LocalDateTime.now(),
                    priceAverage(dataDay),
                    volumeAdder(dataDay));

            SQLite.getInstance().saveMonthPrice(item, bigDayInstant);

            SQLite.getInstance().saveHistoryPrices(item, bigDayInstant);
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
