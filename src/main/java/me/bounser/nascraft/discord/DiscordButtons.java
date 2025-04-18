package me.bounser.nascraft.discord;

import github.scarsz.discordsrv.DiscordSRV;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.chart.cpi.CPIChart;
import me.bounser.nascraft.chart.flows.FlowChart;
import me.bounser.nascraft.chart.price.ChartType;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.Database;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.discord.images.*;
import me.bounser.nascraft.managers.DebtManager;
import me.bounser.nascraft.portfolio.PortfoliosManager;
import me.bounser.nascraft.portfolio.Portfolio;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.managers.ImagesManager;
import me.bounser.nascraft.managers.MoneyManager;
import me.bounser.nascraft.managers.currencies.CurrenciesManager;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class DiscordButtons extends ListenerAdapter {


    public static DiscordButtons instance;

    private Lang lang = Lang.get();

    private Database database;

    public DiscordButtons() {
        instance = this;
        this.database = DatabaseManager.get().getDatabase();
    }

    public static DiscordButtons getInstance() { return instance; }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        if (!event.getChannel().getId().equals(Config.getInstance().getChannel())) { return; }

        switch (event.getComponentId()) {

            case "alerts":
                HashMap<Item, Double> alerts = DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId());

                if (alerts == null || alerts.size() == 0) {
                    event.reply(Lang.get().message(Message.DISCORD_NO_ALERTS_SETUP))
                            .setEphemeral(true)
                            .addActionRow(Arrays.asList(
                                    Button.success("addalert", Lang.get().message(Message.DISCORD_ADD_ALERT_BUTTON)).withEmoji(Emoji.fromFormatted("U+1F514")),
                                    Button.danger("removealert", Lang.get().message(Message.DISCORD_REMOVE_ALERT_BUTTON)).withEmoji(Emoji.fromFormatted("U+1F515"))))
                            .queue(message -> message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS));

                    return;
                }

                String alertsMessage = Lang.get().message(Message.DISCORD_ALERT_HEADER);

                for (Item item : DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).keySet()) {
                    alertsMessage = alertsMessage + Lang.get().message(Message.DISCORD_ALERT_SEGMENT)
                            .replace("[MATERIAL]", item.getName())
                            .replace("[PRICE]", Formatter.plainFormat(item.getCurrency(), Math.abs(alerts.get(item)), Style.ROUND_BASIC)) + "\n";
                }

                event.reply(alertsMessage)
                        .setEphemeral(true)
                        .addActionRow(Arrays.asList(
                                Button.success("addalert", Lang.get().message(Message.DISCORD_ADD_ALERT_BUTTON)).withEmoji(Emoji.fromFormatted("U+1F514")),
                                Button.danger("removealert", Lang.get().message(Message.DISCORD_REMOVE_ALERT_BUTTON)).withEmoji(Emoji.fromFormatted("U+1F515"))))
                        .queue(message -> message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS));

                return;

            case "addalert":

                TextInput material = TextInput.create("material", Lang.get().message(Message.MATERIAL), TextInputStyle.SHORT)
                        .setPlaceholder(Lang.get().message(Message.DISCORD_ADD_ALERT_ARGUMENT_MATERIAL))
                        .setRequiredRange(1, 40)
                        .build();

                TextInput price = TextInput.create("price", Lang.get().message(Message.PRICE), TextInputStyle.SHORT)
                        .setPlaceholder(Lang.get().message(Message.DISCORD_ADD_ALERT_ARGUMENT_PRICE))
                        .setMinLength(1)
                        .setMaxLength(10)
                        .build();

                Modal modal = Modal.create("createalert", Lang.get().message(Message.DISCORD_CREATE_ALERT))
                        .addComponents(ActionRow.of(material), ActionRow.of(price))
                        .build();

                event.replyModal(modal).queue();
                return;

            case "removealert":

                if (DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()) == null ||
                        DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).size() == 0) {
                    event.reply(Lang.get().message(Message.DISCORD_NO_ALERTS_SETUP))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }

                StringSelectMenu.Builder builder = StringSelectMenu.create("menu:id");

                for (Item item : DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).keySet())
                    builder.addOption(item.getName(), "alert-" + item.getIdentifier(),
                            Lang.get().message(Message.DISCORD_ALERT_AT_PRICE)
                                    .replace("[PRICE]", Formatter.plainFormat(item.getCurrency(), Math.abs(DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).get(item)), Style.ROUND_BASIC)));

                event.reply(Lang.get().message(Message.DISCORD_ALERT_REMOVE_SELECT))
                        .setEphemeral(true)
                        .addActionRow(builder.build())
                        .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
                return;

            case "advanced":

                Config config = Config.getInstance();

                List<ItemComponent> firstRow = new ArrayList<>();
                List<ItemComponent> secondRow = new ArrayList<>();

                if (config.getOptionAlertEnabled()) firstRow.add(Button.primary("nothing1", Lang.get().message(Message.DISCORD_ADVANCED_TOOLS)).asDisabled());
                if (config.getOptionAlertEnabled()) firstRow.add(Button.secondary("alerts", Lang.get().message(Message.DISCORD_ALERTS_NAME)).withEmoji(Emoji.fromFormatted("U+1F514")));

                if (config.getOptionCPIComparisonEnabled() ||
                    config.getOptionCPIComparisonEnabled() ||
                    config.getOptionFlowsEnabled())
                    secondRow.add(Button.primary("nothing2", Lang.get().message(Message.DISCORD_ADVANCED_GRAPHS)).asDisabled());

                if (config.getOptionCPIEnabled()) secondRow.add(Button.secondary("cpi", Lang.get().message(Message.DISCORD_CPI_EVOLUTION)).withEmoji(Emoji.fromFormatted("U+1F4C8")));
                if (config.getOptionCPIEnabled()) secondRow.add(Button.secondary("compare-to-cpi", Lang.get().message(Message.DISCORD_COMPARE_CPI)).withEmoji(Emoji.fromFormatted("U+1F4C8")));
                if (config.getOptionFlowsEnabled()) secondRow.add(Button.secondary("flows", Lang.get().message(Message.DISCORD_FLOWS)).withEmoji(Emoji.fromFormatted("U+1F4C8")));

                event.reply(".")
                        .addActionRow(firstRow)
                        .addActionRow(secondRow)
                        .setEphemeral(true)
                        .queue();

                return;

            case "cpi":

                EmbedBuilder embedBuilder = new EmbedBuilder();

                embedBuilder.setColor(new Color(100, 50, 150));

                embedBuilder.setImage("attachment://image.png");

                event.replyEmbeds(embedBuilder.build())
                        .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(CPIChart.getImage(500, 250)), "image.png"))
                        .setEphemeral(true)
                        .queue();

                return;

            case "compare-to-cpi":

                TextInput material2 = TextInput.create("material", Lang.get().message(Message.DISCORD_COMPARISON_LABEL), TextInputStyle.SHORT)
                        .setPlaceholder(Lang.get().message(Message.DISCORD_MATERIAL_TO_COMPARE))
                        .setRequiredRange(1, 40)
                        .build();

                Modal modal2 = Modal.create("compare-item-cpi", Lang.get().message(Message.DISCORD_COMPARISON_TITLE))
                        .addComponents(ActionRow.of(material2))
                        .build();

                event.replyModal(modal2).queue();

                return;

            case "flows":

                EmbedBuilder embedBuilderFlow = new EmbedBuilder();

                embedBuilderFlow.setColor(new Color(100, 50, 150));

                embedBuilderFlow.setImage("attachment://image.png");

                event.replyEmbeds(embedBuilderFlow.build())
                        .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(FlowChart.getImage(700, 400)), "image.png"))
                        .setEphemeral(true)
                        .queue();

                return;

            case "manager":

                List<ItemComponent> brokers = new ArrayList<>();

                brokers.add(Button.primary("data-broker", Emoji.fromFormatted("U+2754")));

                String brokerText = "## Available brokers:\n" +
                        "> :man_office_worker: **Aggresive broker**: Assing a quantity of money to him and he will automatically try to earn more, doing even complex operations with derivatives. \n" +
                        "> **Fee**: 5% daily\n" +
                        "\n" +
                        "> :man_teacher: **Regular broker**: Assing a quantity of money to him and he will automatically try to earn more in a conservative manner. \n" +
                        "> **Fee**: 1% daily\n" +
                        "\n" +
                        "> :man_technologist: **Lazy broker**: Assing a quantity of money to him will guarantee the market returns (Average).\n" +
                        "> **Fee**: 0.4% daily" +
                        "\n\n:warning: **The value of your investment may fall as well as rise and you may get back less than you originally invested.**";

                event.reply(brokerText)
                        .addActionRow(brokers)
                        .setEphemeral(true)
                        .queue();
                return;

            case "opensession":

                TextInput material1 = TextInput.create("material", "Item material", TextInputStyle.SHORT)
                        .setPlaceholder("Material of the item you want to operate with.")
                        .setRequiredRange(1, 40)
                        .build();

                Modal modal1 = Modal.create("advanced", "Advanced options")
                        .addComponents(ActionRow.of(material1))
                        .build();

                event.replyModal(modal1).queue();

                return;

            case "link":

                switch (LinkManager.getInstance().getLinkingMethod()) {

                    case DISCORDSRV:

                        if (DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getUser().getId()) == null) {

                            event.reply(Lang.get().message(Message.DISCORD_LINK_DISCORDSRV_EXTENSIVE))
                                    .setEphemeral(true)
                                    .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                        } else {

                            event.reply( Lang.get().message(Message.DISCORD_LINK_DISCORDSRV_ALREADY, "[UUID]", DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getUser().getId()).toString()))
                                    .setEphemeral(true)
                                    .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                        }
                        return;

                    case NATIVE:

                        if (LinkManager.getInstance().getUUID(event.getUser().getId()) == null) {

                            event.reply(Lang.get().message(Message.DISCORD_LINK_NATIVE_EXTENSIVE, "[CODE]", String.valueOf(LinkManager.getInstance().startLinkingProcess(event.getUser().getId()))))
                                    .setEphemeral(true)
                                    .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                        } else {

                            event.reply(Lang.get().message(Message.DISCORD_LINK_NATIVE_ALREADY, "[NICKNAME]", database.getNickname(event.getUser().getId())))
                                    .setEphemeral(true)
                                    .addActionRow(Button.danger("unlink", Lang.get().message(Message.DISCORD_UNLINK_BUTTON)))
                                    .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                        }
                        return;
                }

            case "unlink":

                event.editButton(Button.danger("unlinkc", Lang.get().message(Message.DISCORD_CONFIRM))).queue(); return;

            case "unlinkc":

                String nickname = database.getNickname(event.getUser().getId());
                String text;
                if (LinkManager.getInstance().unlink(event.getUser().getId())) {
                    text = Lang.get().message(Message.DISCORD_UNLINKED, "[NICKNAME]", nickname);
                } else {
                    text = Lang.get().message(Message.DISCORD_ALREADY_UNLINKED);
                }

                event.reply(text)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                return;

            case "search":

                TextInput material3 = TextInput.create("material", Lang.get().message(Message.MATERIAL), TextInputStyle.SHORT)
                        .setPlaceholder(Lang.get().message(Message.DISCORD_MATERIAL_TO_OPERATE))
                        .setRequiredRange(1, 40)
                        .build();

                Modal modal3 = Modal.create("basic", Lang.get().message(Message.DISCORD_BUTTON_1))
                        .addComponents(ActionRow.of(material3))
                        .build();

                event.replyModal(modal3).queue();

                return;

            case "data":

                List<ItemComponent> componentListData1 = new ArrayList<>();
                List<ItemComponent> componentListData2 = new ArrayList<>();

                componentListData1.add(Button.primary("data-inventory", lang.message(Message.DISCORD_WIKI_1)).withEmoji(Emoji.fromFormatted("U+1F392")));
                componentListData1.add(Button.primary("data-balance", lang.message(Message.DISCORD_WIKI_2)).withEmoji(Emoji.fromFormatted("U+1FA99")));
                componentListData1.add(Button.primary("data-alerts", lang.message(Message.DISCORD_WIKI_3)).withEmoji(Emoji.fromFormatted("U+1F514")).asDisabled());
                componentListData2.add(Button.primary("data-dynamic", lang.message(Message.DISCORD_WIKI_4)).withEmoji(Emoji.fromFormatted("U+1F3AF")).asDisabled());
                componentListData2.add(Button.primary("data-session", lang.message(Message.DISCORD_WIKI_5)).withEmoji(Emoji.fromFormatted("U+1F5A5")).asDisabled());
                componentListData2.add(Button.primary("data-broker", lang.message(Message.DISCORD_WIKI_6)).withEmoji(Emoji.fromFormatted("U+1F4BC")).asDisabled());

                event.reply(lang.message(Message.DISCORD_WIKI))
                        .addActionRow(componentListData1)
                        .addActionRow(componentListData2)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(30, TimeUnit.SECONDS));

                return;

            case "data-inventory":
                sendMessage(event, lang.message(Message.DISCORD_WIKI_INVENTORY), 35); return;

            case "data-balance":
                sendMessage(event, lang.message(Message.DISCORD_WIKI_BALANCE), 35); return;

            case "data-alerts":
                sendMessage(event, lang.message(Message.DISCORD_WIKI_ALERTS), 35); return;

            case "data-dynamic":
                sendMessage(event, lang.message(Message.DISCORD_WIKI_DYNAMIC), 35); return;

            case "data-session":
               sendMessage(event, lang.message(Message.DISCORD_WIKI_SESSIONS), 35); return;

            case "data-broker":
                sendMessage(event, lang.message(Message.DISCORD_WIKI_BROKERS), 25); return;

        }

        if (LinkManager.getInstance().getUUID(event.getUser().getId()) == null) {

            switch (LinkManager.getInstance().getLinkingMethod()) {

                case DISCORDSRV:
                    event.reply(Lang.get().message(Message.DISCORD_LINK_DISCORDSRV))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;

                case NATIVE:
                    event.reply(Lang.get().message(Message.DISCORD_LINK_NATIVE, "[CODE]", String.valueOf(LinkManager.getInstance().startLinkingProcess(event.getUser().getId()))))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;

            }
        }

        UUID uuid = LinkManager.getInstance().getUUID(event.getUser().getId());

        switch (event.getComponentId()) {

            case "inventory":

                Portfolio discordInventory = PortfoliosManager.getInstance().getPortfolio(uuid);

                if (discordInventory.getCapacity() < 40) {

                    List<Button> actionRow = new ArrayList<>();

                    actionRow.add(Button.success("i_buy", Lang.get().message(Message.DISCORD_BUY_SLOT) + Formatter.plainFormat(CurrenciesManager.getInstance().getDefaultCurrency(), discordInventory.getNextSlotPrice(), Style.REDUCED_LENGTH)));
                    actionRow.add(Button.danger("all", Lang.get().message(Message.DISCORD_SELL_ALL)));

                    event.replyFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(InventoryImage.getImage(discordInventory)) , "image.png"))
                            .setEphemeral(true)
                            .addActionRow(actionRow)
                            .queue(message -> message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS));

                } else {

                    event.replyFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(InventoryImage.getImage(discordInventory)), "image.png"))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS));
                }
                return;

            case "all":

                event.editButton(Button.danger("sellallconfirmed", Lang.get().message(Message.DISCORD_CONFIRM))).queue(); return;

            case "sellallconfirmed":

                if (DebtManager.getInstance().getDebtOfPlayer(uuid) != 0) {
                    event.reply(Lang.get().message(Message.PORTFOLIO_DEBT_DIS_LOCKED))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));

                    return;
                }

                if (!MarketManager.getInstance().getActive()) {
                    event.reply(Lang.get().message(Message.DISCORD_MARKET_CLOSED))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(25, TimeUnit.SECONDS));
                    return;
                }

                PortfoliosManager.getInstance().getPortfolio(uuid).sellAll(
                        value -> event.reply( Lang.get().message(Message.DISCORD_SOLD_EVERYTHING, "[VALUE]", Formatter.plainFormat
                                        (CurrenciesManager.getInstance().getVaultCurrency(), value, Style.ROUND_BASIC)))
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(25, TimeUnit.SECONDS))
                );
                return;

            case "balance":
                OfflinePlayer player2 = Bukkit.getOfflinePlayer(uuid);

                double purse = Nascraft.getEconomy().getBalance(player2);
                double inventory = PortfoliosManager.getInstance().getPortfolio(event.getUser().getId()).getInventoryValue();
                float brokerValue = 0;
                double total = purse + inventory + brokerValue;

                String report = Lang.get().message(Message.DISCORD_BALANCE_REPORT)
                        .replace("[PURSE]", Formatter.plainFormat(CurrenciesManager.getInstance().getDefaultCurrency(), (float) purse, Style.ROUND_BASIC))
                        .replace("[INVENTORY-VALUE]", Formatter.plainFormat(CurrenciesManager.getInstance().getDefaultCurrency(), inventory, Style.ROUND_BASIC))
                        .replace("[TOTAL]", Formatter.plainFormat(CurrenciesManager.getInstance().getDefaultCurrency(), (float) total, Style.ROUND_BASIC));

                /*
                String text =
                        "\n> :green_circle: :dollar: **Purse** (Minecraft): ``" + Formatter.formatDouble(purse) +
                        "``\n> :yellow_circle: :school_satchel: **Discord Inventory**: ``" + Formatter.format(inventory, Style.ROUND_BASIC) +
                        "``\n> :red_circle: :man_office_worker: **Broker-Managed**: ``" + Formatter.format(brokerValue, Style.ROUND_BASIC) +
                        "``\n> \n" +
                        ">  :abacus: **Total**: ``" + Formatter.formatDouble(total) + "``\n";
                        */

                EmbedBuilder eb = new EmbedBuilder();

                eb.setImage("attachment://image.png");

                eb.setTitle(Lang.get().message(Message.DISCORD_BALANCE_TITLE));

                eb.setFooter(Lang.get().message(Message.DISCORD_PURSE) + ": " + RoundUtils.roundToOne((float) (purse*100/total)) + "% " + Lang.get().message(Message.DISCORD_INVENTORY) + ": " + RoundUtils.roundToOne((float) (inventory*100/total)) + "%");

                eb.setDescription(report);

                eb.setColor(DiscordBot.mixColors(new Color(100,250,100),
                                      new Color(250,250,100),
                                      new Color(250,100,100),
                                      purse/total, inventory/total, brokerValue/total));

                event.replyEmbeds(eb.build())
                        .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(BalanceImage.getImage(event.getUser())) , "image.png"))
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS));
                return;

            case "hback":
            case "hnext":
            case "history":

                int offset = 0;
                if (event.getMessage().getContentRaw().length() != 0)
                    try {
                        offset = Integer.parseInt(event.getMessage().getContentRaw().substring(33, 34))-1;
                    } catch (NumberFormatException ignored) { }

                if (event.getComponentId().equals("hback")) { offset--; }

                if (event.getComponentId().equals("hnext")) { offset++; }

                List<Trade> trades = database.retrieveTrades(uuid, 15*offset, 16);

                String history = Lang.get().message(Message.DISCORD_TRADE_HISTORY_TITLE)
                        .replace("[PAGE]", String.valueOf(1+offset))
                        .replace("[NUM-TRADES]", String.valueOf(trades.size() == 16 ? 15 : trades.size())) + "\n";

                for (Trade trade : trades) {

                    if (trade.getItem() != null) {
                        if (trade.isBuy())
                            history = history + "\n> ``" + getFormatedDate(trade.getDate()) + "`` :inbox_tray: **" + Lang.get().message(Message.DISCORD_BUY) + " " + trade.getAmount() + "** x **" + trade.getItem().getName() + "** :arrow_right: **-" + Formatter.plainFormat(trade.getItem().getCurrency(), trade.getValue(), Style.ROUND_BASIC) + "**";
                        else
                            history = history + "\n> ``" + getFormatedDate(trade.getDate()) + "`` :outbox_tray: **" + Lang.get().message(Message.DISCORD_SELL) + trade.getAmount() + "** x **" + trade.getItem().getName() + "** :arrow_right: **+" + Formatter.plainFormat(trade.getItem().getCurrency(), trade.getValue(), Style.ROUND_BASIC) + "**";

                        if (trade.throughDiscord()) {
                            history = history + " (Discord)";
                        }
                    }
                }

                List<ItemComponent> componentList = new ArrayList<>();

                if (offset == 0 && trades.size() < 16) {
                    componentList.add(Button.primary("hback", Emoji.fromFormatted("U+2B05")).asDisabled());
                    componentList.add(Button.primary("hnext", Emoji.fromFormatted("U+27A1")).asDisabled());
                } else if (offset == 0) {
                    componentList.add(Button.primary("hback", Emoji.fromFormatted("U+2B05")).asDisabled());
                    componentList.add(Button.primary("hnext", Emoji.fromFormatted("U+27A1")));
                } else if (offset == 8 || trades.size() < 16) {
                    componentList.add(Button.primary("hback", Emoji.fromFormatted("U+2B05")));
                    componentList.add(Button.primary("hnext", Emoji.fromFormatted("U+27A1")).asDisabled());
                } else {
                    componentList.add(Button.primary("hback", Emoji.fromFormatted("U+2B05")));
                    componentList.add(Button.primary("hnext", Emoji.fromFormatted("U+27A1")));
                }

                if (event.getComponentId().equals("hback") || event.getComponentId().equals("hnext")) {
                    event.getInteraction().editMessage(history)
                            .setActionRow(componentList)
                            .queue();
                    return;
                }

                event.reply(history+"\n")
                        .setEphemeral(true)
                        .addActionRow(componentList)
                        .queue();

                return;
        }

        if (event.getComponentId().startsWith("time")) {

            ChartType chartType = ChartType.getChartType(event.getComponentId().charAt(4));

            EmbedBuilder embedBuilder = new EmbedBuilder();

            embedBuilder.setColor(new Color(100, 50, 150));

            embedBuilder.setImage("attachment://image.png");

            event.replyEmbeds(embedBuilder.build())
                    .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(ItemTimeGraph.getImage(MarketManager.getInstance().getItem(event.getComponentId().substring(5)), chartType, event.getUser().getId())), "image.png"))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (!MarketManager.getInstance().getActive()) {
            event.reply(Lang.get().message(Message.DISCORD_MARKET_CLOSED))
                    .setEphemeral(true)
                    .queue(message -> message.deleteOriginal().queueAfter(25, TimeUnit.SECONDS));
            return;
        }

        String initial = String.valueOf(event.getComponentId().charAt(0));

        Item item = null;
        int quantity = 0;
                
        if (initial.equals("b") || initial.equals("s")) {

            item = MarketManager.getInstance().getItem(event.getComponentId().substring(3));
            quantity = Integer.parseInt(event.getComponentId().substring(1, 3));
            
        } else if (!initial.equals("i")) { return; }
        
        double value;

        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        Portfolio discordInventory = PortfoliosManager.getInstance().getPortfolio(uuid);

        boolean limitReached = !item.getPrice().canStockChange(quantity, true);

        switch (String.valueOf(event.getComponentId().charAt(0))) {

            case "b":

                if (limitReached && item.isPriceRestricted()) {
                    event.reply(lang.message(Message.DISCORD_BUY_LIMIT_FEEDBACK))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));
                    return;
                }

                value = item.getPrice().getProjectedCost(-quantity, DiscordBot.getInstance().getDiscordBuyTax());

                if (!MoneyManager.getInstance().hasEnoughMoney(player, item.getCurrency(), value)) {

                    event.reply(lang.message(Message.DISCORD_INSUFFICIENT_BALANCE)
                                    .replace("[VALUE1]", Formatter.plainFormat(item.getCurrency(), MoneyManager.getInstance().getBalance(player, item.getCurrency()), Style.ROUND_BASIC))
                                    .replace("[VALUE2]", Formatter.plainFormat(item.getCurrency(), value, Style.ROUND_BASIC)))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));


                    return;
                }

                if (!discordInventory.hasSpace(item, quantity)) {
                    event.reply(lang.message(Message.DISCORD_WITHOUT_SPACE))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));

                    return;
                }

                MoneyManager.getInstance().withdraw(player, item.getCurrency(), value, (1 - Config.getInstance().getDiscordBuyTax()));

                String buyText;
                if (quantity == 1) {
                    buyText = Lang.get().message(Message.DISCORD_BUY_FEEDBACK)
                            .replace("[QUANTITY]", String.valueOf(quantity))
                            .replace("[ALIAS]", item.getName())
                            .replace("[WORTH]", Formatter.plainFormat(item.getCurrency(), value, Style.ROUND_BASIC))
                            .replace("[BALANCE]", Formatter.plainFormat(item.getCurrency(), MoneyManager.getInstance().getBalance(player, item.getCurrency()), Style.ROUND_BASIC));
                } else {
                    buyText = Lang.get().message(Message.DISCORD_BUY_FEEDBACK)
                            .replace("[QUANTITY]", String.valueOf(quantity))
                            .replace("[ALIAS]", item.getName())
                            .replace("[WORTH]", Formatter.plainFormat(item.getCurrency(), value, Style.ROUND_BASIC))
                            .replace("[BALANCE]", Formatter.plainFormat(item.getCurrency(), MoneyManager.getInstance().getBalance(player, item.getCurrency()), Style.ROUND_BASIC))
                            .replace("[WORTH-EACH]", Formatter.plainFormat(item.getCurrency(), value/quantity, Style.ROUND_BASIC));
                }

                Trade buyTrade = new Trade(item, LocalDateTime.now(), value, quantity, true, true, uuid);

                database.saveTrade(buyTrade);
                if (Config.getInstance().getLogChannelEnabled()) DiscordLog.getInstance().sendTradeLog(buyTrade);

                event.reply(buyText)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(6, TimeUnit.SECONDS));

                if (!limitReached) item.ghostBuyItem(quantity);
                discordInventory.addItem(item, quantity);

                break;

            case "s":

                if (limitReached && item.isPriceRestricted()) {
                    event.reply(lang.message(Message.DISCORD_SELL_LIMIT_FEEDBACK))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));
                    return;
                }

                if (!discordInventory.hasItem(item, quantity)) {
                    event.reply(Lang.get().message(Message.DISCORD_NOT_ENOUGH_ITEMS))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));

                    return;
                }

                if (DebtManager.getInstance().getDebtOfPlayer(uuid) != 0) {
                    event.reply(Lang.get().message(Message.PORTFOLIO_DEBT_DIS_LOCKED))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));

                    return;
                }

                value = item.getPrice().getProjectedCost(quantity, DiscordBot.getInstance().getDiscordSellTax());

                MoneyManager.getInstance().deposit(player, item.getCurrency(), value, Config.getInstance().getDiscordSellTax());

                String sellText;
                if(quantity == 1) {
                    sellText = Lang.get().message(Message.DISCORD_SELL_FEEDBACK)
                            .replace("[QUANTITY]", String.valueOf(quantity))
                            .replace("[ALIAS]", item.getName())
                            .replace("[WORTH]", Formatter.plainFormat(item.getCurrency(), value, Style.ROUND_BASIC))
                            .replace("[BALANCE]", Formatter.plainFormat(item.getCurrency(), (float) Nascraft.getEconomy().getBalance(player), Style.ROUND_BASIC));
                } else {
                    sellText = Lang.get().message(Message.DISCORD_SELL_MORE_FEEDBACK)
                            .replace("[QUANTITY]", String.valueOf(quantity))
                            .replace("[ALIAS]", item.getName())
                            .replace("[WORTH]", Formatter.plainFormat(item.getCurrency(), value, Style.ROUND_BASIC))
                            .replace("[BALANCE]", Formatter.plainFormat(item.getCurrency(), (float) Nascraft.getEconomy().getBalance(player), Style.ROUND_BASIC))
                            .replace("[WORTH-EACH]", Formatter.plainFormat(item.getCurrency(), value/quantity, Style.ROUND_BASIC));
                }

                Trade sellTrade = new Trade(item, LocalDateTime.now(), value, quantity, false, true, uuid);

                database.saveTrade(sellTrade);
                if (Config.getInstance().getLogChannelEnabled()) DiscordLog.getInstance().sendTradeLog(sellTrade);

                event.reply(sellText)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(6, TimeUnit.SECONDS));

                if (!limitReached) item.ghostSellItem(quantity);
                discordInventory.removeItem(item, quantity);

                break;

           // Buying capacity expansion
            case "i":

                float price = discordInventory.getNextSlotPrice();

                if (!MoneyManager.getInstance().hasEnoughMoney(player, CurrenciesManager.getInstance().getDefaultCurrency(), price)) {

                    event.reply(Lang.get().message(Message.DISCORD_NOT_ENOUGH_SLOT, "[PRICE]", Formatter.plainFormat(CurrenciesManager.getInstance().getDefaultCurrency(), price, Style.ROUND_BASIC)))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));
                    return;
                }

                if (discordInventory.getCapacity() >= 27) {
                    event.reply(Lang.get().message(Message.DISCORD_ALREADY_MAX_SLOTS))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));
                    return;
                }

                MoneyManager.getInstance().simpleWithdraw(player, CurrenciesManager.getInstance().getDefaultCurrency(), price);

                discordInventory.increaseCapacity();

                event.getInteraction().editComponents().setActionRow(Button.success("info", Lang.get().message(Message.DISCORD_SLOT_BOUGHT, "[NUM]", String.valueOf(discordInventory.getCapacity()))).asDisabled().withEmoji(Emoji.fromFormatted("U+2705"))).queue();

        }
    }


    private String getFormatedDate(LocalDateTime date) {

        String minute = String.valueOf(date.getMinute()).length() == 1 ? "0" + date.getMinute() : String.valueOf(date.getMinute());

        return date.getDayOfMonth() + "/" + date.getMonthValue() + "/" + date.getYear() + " " + date.getHour() + ":" + minute;
    }

    private void sendMessage(ButtonInteractionEvent event, String text, int delay) {
        event.reply(text)
                .setEphemeral(true)
                .queue(message -> message.deleteOriginal().queueAfter(delay, TimeUnit.SECONDS));
    }

}

