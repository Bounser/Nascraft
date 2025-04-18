package me.bounser.nascraft.formatter;

import me.bounser.nascraft.managers.currencies.Currency;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.text.DecimalFormat;

public class Formatter {

    private static Separator separator = Separator.POINT;

    public static String format(Currency currency, Number toFormat, Style style) {

        double number = toFormat.doubleValue();

        String formattedText = null;

        number = roundToDecimals(number, currency.getDecimalPrecission());

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

        assert formattedText != null;
        String result = currency.getFormat().replace("[AMOUNT]", formattedText);

        if (separator == null || separator == Separator.POINT) return result;

        return result.replace(".", "_").replace(",", ".").replace("_", ",");
    }

    public static String plainFormat(Currency currency, Number toFormat, Style style) {

        double number = toFormat.doubleValue();

        String formattedText = null;

        number = roundToDecimals(number, currency.getDecimalPrecission());

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

                if (currency.getDecimalPrecission() == 0) {
                    if (number < 1000) {
                        formattedText = numFormat.format(number);
                    } else if (number < 1_000_000) {
                        formattedText = numFormat.format(number / 1000) + "k";
                    } else {
                        formattedText = numFormat.format(number / 1_000_000) + "m";
                    }
                    break;
                }

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

        assert formattedText != null;
        String result = currency.getPlainFormat().replace("[AMOUNT]", formattedText);

        if (separator == null || separator == Separator.POINT) return result;

        return result.replace(".", "_").replace(",", ".").replace("_", ",");

    }

    public static String formatDouble(double number, Currency currency) {

        DecimalFormat decimalFormat = new DecimalFormat("#,###.000");
        String formattedText = currency.getFormat().replace("[AMOUNT]", decimalFormat.format(number));

        switch (separator) {

            case COMMA:
                return formattedText.replace(".", "_").replace(",", ".").replace("_", ",");

            default:
            case POINT: return formattedText;
        }
    }

    public static void setSeparator(Separator separator) { Formatter.separator = separator; }

    public static double roundToDecimals(Number toFormat, int decimalPlaces) {
        double number = toFormat.doubleValue();
        double scale = Math.pow(10, decimalPlaces);
        return Math.round(number * scale) / scale;
    }

    public static String extractPlainText(Component component) {
        StringBuilder plainText = new StringBuilder();

        if (component instanceof TextComponent) {
            plainText.append(((TextComponent) component).content());
        }

        for (Component child : component.children()) {
            plainText.append(extractPlainText(child));
        }

        return plainText.toString();
    }
}
