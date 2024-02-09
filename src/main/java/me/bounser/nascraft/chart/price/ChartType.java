package me.bounser.nascraft.chart.price;

public enum ChartType {

    DAY,
    MONTH,
    YEAR,
    ALL;

    public static ChartType getChartType(char numberOfChart) {

        switch (numberOfChart) {

            case '1':
                return ChartType.DAY;
            case '2':
                return ChartType.MONTH;
            case '3':
                return ChartType.YEAR;
            case '4':
                return ChartType.ALL;

            default:
                return null;
        }
    }

}
