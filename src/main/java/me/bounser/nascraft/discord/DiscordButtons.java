package me.bounser.nascraft.discord;

import github.scarsz.discordsrv.DiscordSRV;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.chart.price.ChartType;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.Database;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.discord.images.*;
import me.bounser.nascraft.discord.inventories.DiscordInventories;
import me.bounser.nascraft.discord.inventories.DiscordInventory;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.managers.MoneyManager;
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
                HashMap<Item, Float> alerts = DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId());

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
                            .replace("[PRICE]", Formatter.format(Math.abs(alerts.get(item)), Style.ROUND_BASIC)) + "\n";
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
                                    .replace("PRICE", Formatter.format(Math.abs(DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).get(item)), Style.ROUND_BASIC)));

                event.reply(Lang.get().message(Message.DISCORD_ALERT_REMOVE_SELECT))
                        .setEphemeral(true)
                        .addActionRow(builder.build())
                        .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
                return;

            case "advanced":

                List<ItemComponent> componentList = new ArrayList<>();

                componentList.add(Button.secondary("limit", "Limit orders").withEmoji(Emoji.fromFormatted("U+1F3AF")).asDisabled());
                componentList.add(Button.secondary("opensession", "Open session").withEmoji(Emoji.fromFormatted("U+1F5A5")).asDisabled());
                componentList.add(Button.secondary("alerts", Lang.get().message(Message.DISCORD_ALERTS_NAME)).withEmoji(Emoji.fromFormatted("U+1F514")));
                componentList.add(Button.secondary("ositions", "Open positions").withEmoji(Emoji.fromFormatted("U+231B")).asDisabled());

                event.reply(":bar_chart: Advanced Mode: Select an option.")
                        .addActionRow(componentList)
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

                            event.reply(":link: To be able to interact with the market you have to link your minecraft and discord's account." +
                                            "\nTo start the process use ``/discord link`` in-game!")
                                    .setEphemeral(true)
                                    .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                        } else {

                            event.reply(":link: You have your account linked with the user: ``" + DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getUser().getId()) + "``")
                                    .setEphemeral(true)
                                    .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                        }

                        return;

                    case NATIVE:

                        if (LinkManager.getInstance().getUUID(event.getUser().getId()) == null) {

                            event.reply(":link: To be able to interact with the market you have to link your minecraft and discord's account." +
                                            "\nLink your account using ``/link " + LinkManager.getInstance().startLinkingProcess(event.getUser().getId()) + "`` in-game!")
                                    .setEphemeral(true)
                                    .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                        } else {

                            event.reply(":link: You have your account linked with the user: ``" + database.getNickname(event.getUser().getId()) + "``")
                                    .setEphemeral(true)
                                    .addActionRow(Button.danger("unlink", "Unlink account"))
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
                    text = ":link: You have un-linked your account from ``" + nickname + "``";
                } else {
                    text = ":exclamation: You are already not linked!";
                }

                event.reply(text)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                return;

            case "search":

                TextInput material2 = TextInput.create("material", Lang.get().message(Message.MATERIAL), TextInputStyle.SHORT)
                        .setPlaceholder("Material of the item you want to operate with.")
                        .setRequiredRange(1, 40)
                        .build();

                Modal modal2 = Modal.create("basic", Lang.get().message(Message.DISCORD_BUTTON_1))
                        .addComponents(ActionRow.of(material2))
                        .build();

                event.replyModal(modal2).queue();

                return;

            case "data":

                String info = "## :information_source: Market information \n> Using this channel you will be able to **buy/sell items and much more without being in the game!** To start using this you have to link your discord account, to do so just press the :link: **Account linking** button and  follow instructions. \n## Click on a category to learn more:";

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
                    event.reply(":link: Use ``/link `` in-game to start the linking process!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;


                case NATIVE:
                    event.reply(":link: Link your account using ``/link " + LinkManager.getInstance().startLinkingProcess(event.getUser().getId()) + "`` in-game!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;

            }
        }

        UUID uuid = LinkManager.getInstance().getUUID(event.getUser().getId());

        switch (event.getComponentId()) {

            case "inventory":

                DiscordInventory discordInventory = DiscordInventories.getInstance().getInventory(uuid);

                if (discordInventory.getCapacity() < 40) {

                    List<Button> actionRow = new ArrayList<>();

                    actionRow.add(Button.success("i_buy", Lang.get().message(Message.DISCORD_BUY_SLOT) + discordInventory.getNextSlotPrice() + Lang.get().message(Message.CURRENCY)));
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

                if (!MarketManager.getInstance().getActive()) {
                    event.reply(":lock: Market is currently closed!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(25, TimeUnit.SECONDS));
                    return;
                }

                DiscordInventories.getInstance().getInventory(uuid).sellAll(
                        value -> event.reply("You just sold everything for: **" + Formatter.format(value, Style.ROUND_BASIC) + "**")
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(25, TimeUnit.SECONDS))
                );
                return;

            case "balance":
                OfflinePlayer player2 = Bukkit.getOfflinePlayer(uuid);

                double purse = Nascraft.getEconomy().getBalance(player2);
                float inventory = DiscordInventories.getInstance().getInventory(event.getUser().getId()).getInventoryValue();
                float brokerValue = 0;
                double total = purse + inventory + brokerValue;

                String text =
                        "\n> :green_circle: :dollar: **Purse** (Minecraft): ``" + Formatter.formatDouble(purse) +
                        "``\n> :yellow_circle: :school_satchel: **Discord Inventory**: ``" + Formatter.format(inventory, Style.ROUND_BASIC) +
                        "``\n> :red_circle: :man_office_worker: **Broker-Managed**: ``" + Formatter.format(brokerValue, Style.ROUND_BASIC) +
                        "``\n> \n" +
                        ">  :abacus: **Total**: ``" + Formatter.formatDouble(total) + "``\n";

                EmbedBuilder eb = new EmbedBuilder();

                eb.setImage("attachment://image.png");

                eb.setTitle(":coin:  **Balance**:");

                eb.setFooter("Purse: " + RoundUtils.roundToOne((float) (purse*100/total)) + "% Inventory: " + RoundUtils.roundToOne((float) (inventory*100/total)) + "% Broker: " + RoundUtils.roundToOne((float) (brokerValue*100/total)) + "%");

                eb.setDescription(text);


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

                List<Trade> trades = database.retrieveTrades(uuid, 15*offset);

                String history = ":scroll: **Trade history:** Page " +  (1 + offset)  + " (" + (trades.size() == 16 ? 15 : trades.size()) + "/15)\n";

                for (Trade trade : trades) {

                    if (trade.getTradable() != null) {
                        if (trade.isBuy())
                            history = history + "\n> ``" + getFormatedDate(trade.getDate()) + "`` :inbox_tray: **BUY " + trade.getAmount() + "** x **" + trade.getTradable().getName() + "** :arrow_right: **-" + Formatter.format(trade.getValue(), Style.ROUND_BASIC) + "**";
                        else
                            history = history + "\n> ``" + getFormatedDate(trade.getDate()) + "`` :outbox_tray: **SELL " + trade.getAmount() + "** x **" + trade.getTradable().getName() + "** :arrow_right: **+" + Formatter.format(trade.getValue(), Style.ROUND_BASIC) + "**";

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
                    .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(ItemTimeGraph.getImage(MarketManager.getInstance().getItem(event.getComponentId().substring(5)), chartType)), "image.png"))
                    .setEphemeral(true)
                    .queue();

            return;
        }

        if (!MarketManager.getInstance().getActive()) {
            event.reply(":lock: Market is currently closed!")
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
        
        float value;

        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        double balance = Nascraft.getEconomy().getBalance(player);

        DiscordInventory discordInventory = DiscordInventories.getInstance().getInventory(uuid);

        switch (String.valueOf(event.getComponentId().charAt(0))) {

            case "b":

                value = item.getPrice().getProjectedCost(-quantity, DiscordBot.getInstance().getDiscordBuyTax());

                if (balance < value) {

                    event.reply(lang.message(Message.DISCORD_INSUFFICIENT_BALANCE)
                                    .replace("[VALUE1]", Formatter.format((float) balance, Style.ROUND_BASIC))
                                    .replace("[VALUE2]", Formatter.format(value, Style.ROUND_BASIC)))
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

                MoneyManager.getInstance().withdraw(player, value);

                String buyText;
                if (quantity == 1) {
                    buyText = ":inbox_tray: You just bought **" + quantity + "** of **" + item.getName() + "** worth **" + Formatter.format(value, Style.ROUND_BASIC) + "**" +
                            "\n\n:coin: Your balance is now: **" + Formatter.format((float) Nascraft.getEconomy().getBalance(player), Style.ROUND_BASIC) + "**";
                } else {
                    buyText = ":inbox_tray: You just bought **" + quantity + "** of **" + item.getName() + "** worth **" + Formatter.format(value, Style.ROUND_BASIC) + "** (" + Formatter.format(value/quantity, Style.ROUND_BASIC) + " each)" +
                            "\n\n:coin: Your balance is now: **" + Formatter.format((float) Nascraft.getEconomy().getBalance(player), Style.ROUND_BASIC) + "**";
                }

                database.saveTrade(uuid, item, quantity, value, true, true);

                event.reply(buyText)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(6, TimeUnit.SECONDS));

                item.ghostBuyItem(quantity);
                discordInventory.addItem(item, quantity);

                break;

            case "s":

                if (!discordInventory.hasItem(item, quantity)) {
                    event.reply(":x: You don't have this item in your discord inventory!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));

                    return;
                }

                value = item.getPrice().getProjectedCost(quantity, DiscordBot.getInstance().getDiscordSellTax());

                MoneyManager.getInstance().deposit(player, value);

                String sellText;
                if(quantity == 1) {
                    sellText = ":outbox_tray: You just sold **" + quantity + "** of **" + item.getName() + "** worth **" + Formatter.format(value, Style.ROUND_BASIC) + "**" +
                            "\n\n:coin: Your balance is now: **" + Formatter.format((float) Nascraft.getEconomy().getBalance(player), Style.ROUND_BASIC) + "**";
                } else {
                    sellText = ":outbox_tray: You just sold **" + quantity + "** of **" + item.getName() + "** worth **" + Formatter.format(value, Style.ROUND_BASIC) + "** (" + Formatter.format(value/quantity, Style.ROUND_BASIC) + " each)" +
                            "\n\n:coin: Your balance is now: **" + Formatter.format((float) Nascraft.getEconomy().getBalance(player), Style.ROUND_BASIC) + "**";
                }

                database.saveTrade(uuid, item, quantity, value, false, true);

                event.reply(sellText)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(6, TimeUnit.SECONDS));

                item.ghostSellItem(quantity);
                discordInventory.removeItem(item, quantity);

                break;

            case "i":

                float price = discordInventory.getNextSlotPrice();

                if (balance < price) {

                    event.reply(":x: You don't have enough money to buy another slot! You need **" +  Formatter.format(price, Style.ROUND_BASIC) + "**")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));
                    return;
                }

                if (discordInventory.getCapacity() >= 40) {
                    event.reply(":x: You already have bought the maximum number of slots!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));
                    return;
                }

                Nascraft.getEconomy().withdrawPlayer(player, price);

                discordInventory.increaseCapacity();

                event.getInteraction().editComponents().setActionRow(Button.success("info", "Slot nº" + discordInventory.getCapacity() + " bought!").asDisabled().withEmoji(Emoji.fromFormatted("U+2705"))).queue();

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

