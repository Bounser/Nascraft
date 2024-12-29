package me.bounser.nascraft.managers.currencies;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.formatter.Formatter;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class Currency {

    final private String currencyIdentifier;

    final private CurrencyType currencyType;

    final private String depositCommand;
    final private String withdrawCommand;

    final private String balancePlaceholder;

    final private String notEnoughMessage;

    final private String format;
    final private String plainFormat;

    final int decimalPrecision;

    final double topLimit;
    final double lowLimit;

    public Currency(String currencyIdentifier, CurrencyType currencyType, String depositCommand, String withdrawCommand, String notEnoughMessage, String format, String balancePlaceholder, int decimalPrecision, double topLimit, double lowLimit) {
            this.currencyIdentifier = currencyIdentifier;
            this.currencyType = currencyType;
            this.depositCommand = depositCommand;
            this.withdrawCommand = withdrawCommand;
            this.notEnoughMessage = notEnoughMessage;
            this.format = format;
            this.plainFormat = Formatter.extractPlainText(MiniMessage.miniMessage().deserialize(format));
            this.balancePlaceholder = balancePlaceholder;
            this.decimalPrecision = decimalPrecision;
            this.topLimit = topLimit;
            this.lowLimit = lowLimit;
    }

    public String getCurrencyIdentifier() { return currencyIdentifier; }

    public CurrencyType getCurrencyType() { return currencyType; }

    public String getDepositCommand() { return depositCommand; }
    public String getWithdrawCommand() { return withdrawCommand; }

    public String getBalancePlaceholder() { return balancePlaceholder; }

    public String getNotEnoughMessage() { return notEnoughMessage; }

    public String getFormat() { return format; }
    public String getPlainFormat() { return plainFormat; }

    public int getDecimalPrecission() { return decimalPrecision; }

    public double getTopLimit() { return topLimit; }

    public double getLowLimit() { return lowLimit; }

}
