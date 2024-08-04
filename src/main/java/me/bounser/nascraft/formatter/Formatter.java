package me.bounser.nascraft.formatter;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;

import java.text.DecimalFormat;

public class Formatter {

    private static Separator separator;

    private static final boolean after = Lang.get().after();

    public static String format(float number, Style style) {

        String formattedText = null;

        switch (style) {

            case ROUND_TO_ONE:

                formattedText = String.format("%.1f", number);

                break;

            case ROUND_BASIC:
                DecimalFormat decimalFormat;
                if (number >= 0.1) decimalFormat = new DecimalFormat("#,###.##");
                else decimalFormat = new DecimalFormat("#.###");
                formattedText = decimalFormat.format(number);

                break;

            case REDUCED_LENGTH:

                DecimalFormat numFormat = new DecimalFormat("0.#");

                if (number <= 0.1) {
                    formattedText = String.format("%.3f", number);
                } else if (number < 100) {
                    formattedText = String.format("%.2f", number);
                } else if (number < 1000) {
                    formattedText = numFormat.format(number);
                } else if (number < 1_000_000) {
                    formattedText = numFormat.format(number / 1000) + "k";
                } else {
                    formattedText = numFormat.format(number / 1_000_000) + "m";
                }

                break;
        }

        if (after)
            formattedText = formattedText + Lang.get().message(Message.CURRENCY);
        else
            formattedText = Lang.get().message(Message.CURRENCY) + formattedText;

        switch (separator) {

            case COMMA:
                return formattedText.replace(".", "a").replace(",", ".").replace("a", ",");

            default:
            case POINT: return formattedText;
        }
    }

    public static String formatDouble(double number) {

        DecimalFormat decimalFormat = new DecimalFormat("#,###.000");
        String formattedText = decimalFormat.format(number);

        if (after)
            formattedText = formattedText + Lang.get().message(Message.CURRENCY);
        else
            formattedText = Lang.get().message(Message.CURRENCY) + formattedText;

        switch (separator) {

            case COMMA:
                return formattedText.replace(".", "a").replace(",", ".").replace("a", ",");

            default:
            case POINT: return formattedText;
        }
    }

    public static void setSeparator(Separator separator) { Formatter.separator = separator; }

}
