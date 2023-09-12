package me.bounser.nascraft.formatter;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;

public class Formatter {

    private static Separator separator;

    public static String format(float number, Style style) {

        String formattedText = null;

        switch (style) {

            case ROUND_TO_ONE:

                formattedText = String.valueOf(RoundUtils.roundToOne(number));

                break;

            case ROUND_TO_TWO:

                formattedText = String.valueOf(RoundUtils.round(number));

                break;
        }

        switch (separator) {

            case COMMA:
                return formattedText.replace(".", ",") + Lang.get().message(Message.CURRENCY);

            default:
            case POINT: return formattedText + Lang.get().message(Message.CURRENCY);
        }
    }

    public static void setSeparator(Separator separator) { Formatter.separator = separator; }

}
