package me.bounser.nascraft.formatter;

import me.bounser.nascraft.config.Config;
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

                formattedText = String.valueOf(RoundUtils.roundToOne(number));

                break;

            case ROUND_TO_TWO:

                formattedText = String.valueOf(RoundUtils.round(number));

                break;

            case REDUCED_LENGTH:

                DecimalFormat numFormat = new DecimalFormat("0.#");

                if (number < 100) {
                    formattedText = String.valueOf(number);
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
                return formattedText.replace(".", ",");

            default:
            case POINT: return formattedText;
        }
    }

    public static void setSeparator(Separator separator) { Formatter.separator = separator; }

}
