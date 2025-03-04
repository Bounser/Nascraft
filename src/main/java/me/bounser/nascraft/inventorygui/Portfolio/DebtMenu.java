package me.bounser.nascraft.inventorygui.Portfolio;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.inventorygui.MarketMenuManager;
import me.bounser.nascraft.inventorygui.MenuPage;
import me.bounser.nascraft.managers.DebtManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.managers.currencies.Currency;
import me.bounser.nascraft.portfolio.PortfoliosManager;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DebtMenu implements MenuPage {

    private Inventory gui;

    private final Player player;

    public DebtMenu(Player player) {
        this.player = player;

        open();
    }

    @Override
    public void open() {

        Config config = Config.getInstance();

        Component title = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_DEBT_TITLE));

        gui = Bukkit.createInventory(null, config.getDebtSize(), BukkitComponentSerializer.legacy().serialize(title));

        // Back button

        if (config.getDebtBackEnabled()) {
            Component backComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_DEBT_BACK_NAME));

            gui.setItem(
                    config.getDebtBackSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getDebtBackMaterial(),
                            BukkitComponentSerializer.legacy().serialize(backComponent)
                    ));
        }

        // Fillers

        Component fillerComponent = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.GUI_FILLERS_NAME));

        ItemStack filler = MarketMenuManager.getInstance().generateItemStack(
                config.getDebtFillersMaterial(),
                BukkitComponentSerializer.legacy().serialize(fillerComponent)
        );

        for (int i : config.getDebtFillersSlots())
            gui.setItem(i, filler);


        // Explanation
        if (config.getDebtExpEnabled()) {

            Component information = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_DEBT_EXP_NAME));

            List<String> lore = new ArrayList<>();
            for (String line : Lang.get().message(Message.PORTFOLIO_DEBT_EXP_LORE).split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            gui.setItem(
                    config.getDebtExpSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getDebtExpMaterial(),
                            BukkitComponentSerializer.legacy().serialize(information),
                            lore
                    ));
        }

        update();

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "debt"));

    }

    @Override
    public void close() {

    }

    @Override
    public void update() {

        UUID uuid = player.getUniqueId();

        Config config = Config.getInstance();

        double currentDebt = DebtManager.getInstance().getDebtOfPlayer(uuid);
        double value = PortfoliosManager.getInstance().getPortfolio(uuid).getInventoryValue();
        double maxLoan = DebtManager.getInstance().getMaximumLoan(uuid);
        Currency currency = CurrenciesManager.getInstance().getDefaultCurrency();

        // Info
        if (config.getDebtInfoEnabled()) {

            Component information = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_DEBT_INFO_NAME));

            List<String> lore = new ArrayList<>();

            LocalTime nextInterestPayment = Config.getInstance().getInterestPaymentHour();

            Duration duration = Duration.between(LocalTime.now(), nextInterestPayment);

            if (duration.isNegative()) {
                duration = duration.plusDays(1);
            }

            long hoursLeft = duration.toHours();
            long minutesLeft = duration.toMinutes() % 60;

            StringBuilder timeLeft = new StringBuilder();

            if (hoursLeft > 0) {
                timeLeft.append(hoursLeft).append(" hour");
                if (hoursLeft > 1) {
                    timeLeft.append("s");
                }
                timeLeft.append(", ");
            }

            if (minutesLeft > 0) {
                timeLeft.append(minutesLeft).append(" min");
                if (minutesLeft > 1) {
                    timeLeft.append("s");
                }
            }

            double interest = 0;

            if (currentDebt != 0) {
                interest = Math.max(currentDebt * config.getLoansDailyInterest(), config.getLoansMinimumInterest());
            }

            String infoLore = Lang.get().message(Message.PORTFOLIO_DEBT_INFO_LORE)
                    .replace("[CURR-DEBT]", Formatter.format(currency, currentDebt, Style.ROUND_BASIC))
                    .replace("[CURR-PER]", String.valueOf(Formatter.roundToDecimals((currentDebt/value) * 100, 2)))
                    .replace("[MAX]", Formatter.format(currency, maxLoan, Style.ROUND_BASIC))
                    .replace("[MAX-PER]", String.valueOf(Formatter.roundToDecimals((maxLoan/value) * 100, 2)))
                    .replace("[TIME]", timeLeft.toString())
                    .replace("[INTEREST]", Formatter.format(currency, interest, Style.ROUND_BASIC))
                    .replace("[LIFETIME]", Formatter.format(currency, DebtManager.getInstance().getLifeTimeInterests(uuid), Style.ROUND_BASIC));

            for (String line : infoLore.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            gui.setItem(
                    config.getDebtInfoSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getDebtInfoMaterial(),
                            BukkitComponentSerializer.legacy().serialize(information),
                            lore
                    ));
        }

        // Repay everything
        if (config.getDebtRepayAllEnabled()) {

            Component information = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_DEBT_REPAY_EVERYTHING_NAME));

            List<String> lore = new ArrayList<>();
            String repayAllLore = Lang.get().message(Message.PORTFOLIO_DEBT_REPAY_EVERYTHING_LORE)
                    .replace("[AMOUNT]", Formatter.format(currency, currentDebt, Style.ROUND_BASIC));

            for (String line : repayAllLore.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            gui.setItem(
                    config.getDebtRepayAllSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getDebtRepayAllMaterial(),
                            BukkitComponentSerializer.legacy().serialize(information),
                            lore
                    ));
        }

        // Repay custom
        if (config.getDebtRepayEnabled()) {

            Component information = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_DEBT_REPAY_NAME));

            List<String> lore = new ArrayList<>();
            String repayCustom = Lang.get().message(Message.PORTFOLIO_DEBT_REPAY_LORE)
                    .replace("[DEBT]", Formatter.format(currency, currentDebt, Style.ROUND_BASIC));

            for (String line : repayCustom.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            gui.setItem(
                    config.getDebtRepaySlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getDebtRepayMaterial(),
                            BukkitComponentSerializer.legacy().serialize(information),
                            lore
                    ));
        }

        // Get max loan
        if (config.getDebtMaxLoanEnabled()) {

            Component information = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_DEBT_TAKE_ALL_NAME));

            double amount = (maxLoan * 0.9) - currentDebt;
            if (amount < 0) amount = 0;

            List<String> lore = new ArrayList<>();
            String getMaxLoan = Lang.get().message(Message.PORTFOLIO_DEBT_TAKE_ALL_LORE)
                    .replace("[AMOUNT]", Formatter.format(currency, amount, Style.ROUND_BASIC))
                    .replace("[INTEREST]", String.valueOf(Formatter.roundToDecimals(config.getLoansDailyInterest()*100, 2)));

            for (String line : getMaxLoan.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            gui.setItem(
                    config.getDebtMaxLoanSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getDebtMaxLoanMaterial(),
                            BukkitComponentSerializer.legacy().serialize(information),
                            lore
                    ));
        }

        // Get custom loan
        if (config.getDebtCustomEnabled()) {

            Component information = MiniMessage.miniMessage().deserialize(Lang.get().message(Message.PORTFOLIO_DEBT_CUSTOM_NAME));

            List<String> lore = new ArrayList<>();
            String customString = Lang.get().message(Message.PORTFOLIO_DEBT_CUSTOM_LORE)
                    .replace("[INTEREST]", String.valueOf(Formatter.roundToDecimals(config.getLoansDailyInterest()*100, 2)))
                    .replace("[ADDITIONAL]", Formatter.format(currency, maxLoan-currentDebt, Style.ROUND_BASIC));

            for (String line : customString.split("\\n")) {
                Component loreComponent = MiniMessage.miniMessage().deserialize(line);
                lore.add(BukkitComponentSerializer.legacy().serialize(loreComponent));
            }

            gui.setItem(
                    config.getDebtCustomSlot(),
                    MarketMenuManager.getInstance().generateItemStack(
                            config.getDebtCustomMaterial(),
                            BukkitComponentSerializer.legacy().serialize(information),
                            lore
                    ));
        }

        player.openInventory(gui);
        player.setMetadata("NascraftMenu", new FixedMetadataValue(Nascraft.getInstance(), "debt"));
        MarketMenuManager.getInstance().setMenuOfPlayer(player, this);
    }
}
