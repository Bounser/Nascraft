package me.bounser.nascraft.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.SQLite;
import me.bounser.nascraft.database.Trade;
import me.bounser.nascraft.discord.images.InventoryImage;
import me.bounser.nascraft.discord.inventories.DiscordInventories;
import me.bounser.nascraft.discord.inventories.DiscordInventory;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
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
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class DiscordButtons extends ListenerAdapter {


    public static DiscordButtons instance;

    private Lang lang = Lang.get();

    public DiscordButtons() { instance = this; }

    public static DiscordButtons getInstance() { return instance; }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        if (!event.getChannel().getId().equals(Config.getInstance().getChannel())) { return; }

        switch (event.getComponentId()) {

            case "alerts":
                HashMap<Item, Float> alerts = DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId().toString());

                if (alerts == null || alerts.size() == 0) {
                    event.reply("No alerts setup! You can setup an alert to receive a DM when the price of an item reaches a limit")
                            .setEphemeral(true)
                            .addActionRow(Arrays.asList(Button.success("addalert", "Add Alert").withEmoji(Emoji.fromFormatted("U+1F514")), Button.danger("removealert", "Remove Alert").withEmoji(Emoji.fromFormatted("U+1F515"))))
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                    return;
                }

                String alertsMessage = "Alerts :bell:\n\n";

                for (Item item : DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId().toString()).keySet()) {
                    alertsMessage = alertsMessage + "> " + item.getName() + " at price: " + Formatter.format(Math.abs(alerts.get(item)), Style.ROUND_TO_TWO) + "\n";
                }

                event.reply(alertsMessage)
                        .setEphemeral(true)
                        .addActionRow(Arrays.asList(Button.success("addalert", "Add Alert").withEmoji(Emoji.fromFormatted("U+1F514")), Button.danger("removealert", "Remove Alert").withEmoji(Emoji.fromFormatted("U+1F515"))))
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                return;

            case "addalert":

                TextInput material = TextInput.create("material", "Item material", TextInputStyle.SHORT)
                        .setPlaceholder("Material of the item to track.")
                        .setRequiredRange(1, 40)
                        .build();

                TextInput price = TextInput.create("price", "Price", TextInputStyle.SHORT)
                        .setPlaceholder("Price at which you want to be notified when reached.")
                        .setMinLength(1)
                        .setMaxLength(10)
                        .build();

                Modal modal = Modal.create("createalert", "Create alert")
                        .addComponents(ActionRow.of(material), ActionRow.of(price))
                        .build();

                event.replyModal(modal).queue();
                return;

            case "removealert":

                if (DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()) == null ||
                        DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).size() == 0) {
                    event.reply("You don't have any alerts currently setup.")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }

                StringSelectMenu.Builder builder = StringSelectMenu.create("menu:id");

                for (Item item : DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).keySet())
                    builder.addOption(item.getName(), "alert-" + item.getMaterial(), "At price: " + Formatter.format(Math.abs(DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).get(item)), Style.ROUND_TO_TWO));

                event.reply("Select the alert that you want to remove.")
                        .setEphemeral(true)
                        .addActionRow(builder.build())
                        .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
                return;

            case "advanced":

                List<ItemComponent> componentList = new ArrayList<>();

                componentList.add(Button.secondary("limit", "Limit orders").withEmoji(Emoji.fromFormatted("U+1F3AF")));
                componentList.add(Button.secondary("opensession", "Open session").withEmoji(Emoji.fromFormatted("U+1F5A5")));
                componentList.add(Button.secondary("alerts", "Alerts").withEmoji(Emoji.fromFormatted("U+1F514")));
                componentList.add(Button.secondary("ositions", "Open positions").withEmoji(Emoji.fromFormatted("U+231B")));

                event.reply(":bar_chart: Advanced Mode: Select an option. \n\n:warning: Caution! In this section you may find complex operations that can involve a total loss of invested money! ")
                        .addActionRow(componentList)
                        .setEphemeral(true)
                        .queue();

                return;

            case "manager":

                List<ItemComponent> brokers = new ArrayList<>();

                brokers.add(Button.primary("data-broker", Emoji.fromFormatted("U+2754")));
                brokers.add(Button.primary("brokeraggresive", "Aggresive Broker").withEmoji(Emoji.fromFormatted("U+1F4BC")));
                brokers.add(Button.primary("brokerregular", "Regular Broker").withEmoji(Emoji.fromFormatted("U+1F4BC")));
                brokers.add(Button.primary("brokerlazy", "Lazy Broker").withEmoji(Emoji.fromFormatted("U+1F4BC")));

                String brokerText = "## Brokers:\n> :man_office_worker: **Aggresive broker**: Assing a quantity of money to him and he will automatically try to earn more, doing even complex operations with derivatives. He charges a comission of **10% a day**. No profits are guaranteed and you can loss all money invested.\n" +
                        "\n> " +
                        ":man_teacher:  **Regular broker**: Assing a quantity of money to him and he will automatically try to earn more in a conservating way. He charges a comission of **4% a day**. No profits are guaranteed and you can loss all money invested.\n" +
                        "\n> " +
                        " :man_technologist:**Lazy broker**: Assing a quantity of money to him will guarantee the market returns (Average) minus a **1% comission**.";

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

                if (LinkManager.getInstance().getUUID(event.getUser().getId()) == null) {

                    event.reply(":link: To be able to interact with the market you have to link your minecraft and discord's account." +
                            "\nLink your account using ``/link " + LinkManager.getInstance().startLinkingProcess(event.getUser().getId()) + "`` in-game!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                } else {

                    event.reply(":link: You have your account linked with the user: ``" + SQLite.getInstance().getNickname(event.getUser().getId()) + "``")
                            .setEphemeral(true)
                            .addActionRow(Button.danger("unlink", "Unlink account"))
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                }

                return;

            case "unlink":

                event.editButton(Button.danger("unlinkc", "Confirm")).queue(); return;

            case "unlinkc":

                String nickname = SQLite.getInstance().getNickname(event.getUser().getId());
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

                TextInput material2 = TextInput.create("material", "Item material", TextInputStyle.SHORT)
                        .setPlaceholder("Material of the item you want to operate with.")
                        .setRequiredRange(1, 40)
                        .build();

                Modal modal2 = Modal.create("basic", "Search item...")
                        .addComponents(ActionRow.of(material2))
                        .build();

                event.replyModal(modal2).queue();

                return;

            case "data":

                String info = "## :information_source: Market information \n> Using this channel you will be able to **buy/sell items and much more without being inside the server!** To start using this you have to link your discord's account, to do so just press the :link: **Account linking** button and  follow instructions. \n## Click on a category to learn more:";

                List<ItemComponent> componentListData1 = new ArrayList<>();
                List<ItemComponent> componentListData2 = new ArrayList<>();

                componentListData1.add(Button.primary("data-inventory", "Inventory").withEmoji(Emoji.fromFormatted("U+1F392")));
                componentListData1.add(Button.primary("data-balance", "Balance").withEmoji(Emoji.fromFormatted("U+1FA99")));
                componentListData1.add(Button.primary("data-alerts", "Alerts").withEmoji(Emoji.fromFormatted("U+1F514")));
                componentListData2.add(Button.primary("data-dynamic", "Dynamic Orders").withEmoji(Emoji.fromFormatted("U+1F3AF")));
                componentListData2.add(Button.primary("data-session", "Sessions").withEmoji(Emoji.fromFormatted("U+1F5A5")));
                componentListData2.add(Button.primary("data-broker", "Brokers").withEmoji(Emoji.fromFormatted("U+1F4BC")));

                event.reply(info)
                        .addActionRow(componentListData1)
                        .addActionRow(componentListData2)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(30, TimeUnit.SECONDS));

                return;

            case "data-inventory":
                String inventory = "## :school_satchel: Inventory\n> Once you link your account, **you're in-game money will be shared with your discord account** and vice versa, and **you will receive a virtual inventory of three slots**, in which you will be able to store the items you buy through discord. Each slot can store **up to 999 of a unique type of item**, and two slots can't store the same type of material. You can buy more slots and check the contents and value pressing the button :school_satchel:\uFEFF **Inventory**.";
                sendMessage(event, inventory, 25); return;

            case "data-balance":
                String balance = "## :coin: Balance\n> If you are linked, the :coin: **Balance** button will show you some statistics on how is your financial state. It will display the **purse shared between your discord and minecraft accounts**, and other information as the current **value of your discord inventory** and the money **under brokers management**.";
                sendMessage(event, balance, 25); return;

            case "data-alerts":
                String alertsData = "## :bell: Alerts\n> If you are interested in a certain item you can set up an **alert** to get a **DM when the price of an item reaches a threshold** that you previously register. To set up your own alerts use :bell:**Alerts**";
                sendMessage(event, alertsData, 25); return;

            case "data-dynamic":
                String dynamic = "## :dart: Dynamic orders\n> Set orders that can react to the price of the asset. You can place Limit orders, to buy/sell an asset when it reaches a certain price, or take profit/stoploss, in which you will sell your position once a price gets reached. To access this functionality, press :bar_chart: **Advanced Options** and then :dart: **Dynamic Orders**.";
                sendMessage(event, dynamic, 25); return;

            case "data-session":
                String session = "## :desktop:  **Sessions**: \n" +
                        "> Selecting an item opens a session, in which you will have access to more operations:" +
                        "\n\n> :chart_with_upwards_trend: **Long price**: Bet in favor of the price increasing. You will take a loan against your Inventory, up to 50% of its value (During the operation all assets will be locked, so you won't be able to sell), and you will multiply the movements of the price by a factor of 3. That means that a 33% gain will translate into a 100% gain, and a -33% into a total loss (-100%)." +
                        "\n\n> :chart_with_downwards_trend: **Short price**: Bet against the price increasing. Is the inverse of the long. Same principle, with a factor of 2. You will earn a 10% if the price falls 5%, and loss everything if the prices goes up 50%." +
                        "\n\n> :bookmark_tabs: **Futures**: They are a contracts in which you can define a future operation with todays prices, paying a comission. If you don't have money right now and want to buy or sell, for example diamonds, if you think that todays price is good, you can sign a future contract, paying 10% in advance, and you will have the right of buying the item in a week (10% comission) or in a month (20% comission) with todays price." +
                        "\n\n> :date: **Programmed actions**: With this option you will be able to sell/buy items regularly in a fixed time interval. For example, you can buy 64 diamonds each week or sell 200 iron ingots a day.";
                sendMessage(event, session, 25); return;

            case "data-broker":
                String broker = "## :man_office_worker: Brokers:\n" +
                        "You can hire a broker to automate the investment process, albeit at a cost in the form of a fee. The available brokers are: " +
                        "\n\n> :man_office_worker: **Aggresive broker**: Assing a quantity of money to him and he will automatically try to earn more by any means, complex derivatives included. He charges a fee of **8% a day**. No profits are guaranteed and you can loss all money invested." +
                        "\n\n> :man_teacher:  **Regular broker**: Assing a quantity of money to him and he will automatically try to earn more conservatively. He charges a fee of **4% a day**. No profits are guaranteed and you can loss all money invested." +
                        "\n\n> :man_technologist:**Lazy broker**: Assing a quantity of money to him will guarantee the market returns (Average) minus a daily **1% fee**.";
                sendMessage(event, broker, 25); return;

        }

        if (LinkManager.getInstance().getUUID(event.getUser().getId()) == null) {

            event.reply(":link: Link your account using ``/link " + LinkManager.getInstance().startLinkingProcess(event.getUser().getId()) + "`` in-game!")
                    .setEphemeral(true)
                    .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
            return;
        }

        UUID uuid = LinkManager.getInstance().getUUID(event.getUser().getId());

        switch (event.getComponentId()) {

            case "inventory":

                DiscordInventory discordInventory = DiscordInventories.getInstance().getInventory(uuid);

                File outputfile = new File("image.png");
                try {
                    ImageIO.write(InventoryImage.getImage(discordInventory), "png", outputfile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (discordInventory.getCapacity() < 40) {

                    List actionRow = new ArrayList();

                    actionRow.add(Button.success("i_buy", "Buy slot for " + discordInventory.getNextSlotPrice() + Lang.get().message(Message.CURRENCY)));
                    actionRow.add(Button.danger("all", "Sell all"));

                    event.replyFiles(FileUpload.fromData(outputfile , "image.png"))
                            .setEphemeral(true)
                            .addActionRow(actionRow)
                            .queue(message -> {
                                message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS);
                            });

                } else {

                    event.replyFiles(FileUpload.fromData(outputfile , "image.png"))
                            .setEphemeral(true)
                            .queue(message -> {
                                message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS);
                            });
                }
                return;

            case "all":
                event.editButton(Button.danger("allconfirmed", "Confirm")).queue(); return;
            case "allconfirmed":

                DiscordInventory discordInventoryToSell = DiscordInventories.getInstance().getInventory(uuid);

                OfflinePlayer player1 = Bukkit.getOfflinePlayer(uuid);

                float totalValue = 0;

                for (Item item : discordInventoryToSell.getContent().keySet()) {
                    totalValue += item.sellItem(discordInventoryToSell.getContent().get(item), uuid, false);
                    discordInventoryToSell.removeItem(item, discordInventoryToSell.getContent().get(item));
                }

                String value = Formatter.format(totalValue, Style.ROUND_TO_TWO);

                Nascraft.getEconomy().depositPlayer(player1, totalValue);

                event.reply("You just sold everything for: " + value)
                        .setEphemeral(true)
                        .queue(message -> {
                            message.deleteOriginal().queueAfter(25, TimeUnit.SECONDS);
                        });

            case "balance":
                OfflinePlayer player2 = Bukkit.getOfflinePlayer(uuid);

                double purse = Nascraft.getEconomy().getBalance(player2);
                float inventory = DiscordInventories.getInstance().getInventory(event.getUser().getId()).getInventoryValue();
                float broker = 0;

                String text = ":coin:  **Balance**:\n" +
                        "\n> :dollar: **Purse** (Minecraft): ``" + Formatter.formatDouble(purse, Style.ROUND_TO_TWO) +
                        "``\n> :school_satchel: **Discord Inventory**: ``" + Formatter.format(inventory, Style.ROUND_TO_TWO) +
                        "``\n> :man_office_worker: **Broker-Managed**: ``" + Formatter.format(broker, Style.ROUND_TO_TWO) +
                        "``\n> \n" +
                        ">  :abacus: **Total**: ``" + Formatter.formatDouble(purse + inventory + broker, Style.ROUND_TO_TWO) + "``";

                event.reply(text)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
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

                List<Trade> trades = SQLite.getInstance().retrieveTrades(uuid, 15*offset);


                String history = ":scroll: **Trade history:** Page " +  (1 + offset)  + " (" + (trades.size() == 16 ? 15 : trades.size()) + " trades. Max 15)\n";

                for (int i = 0; i < trades.size()-1; i++) {

                    Trade trade = trades.get(i);
                    if (trade.getItem() != null) {
                        if (trade.isBuy())
                            history = history + "\n> ``" + getFormatedDate(trade.getDate()) + "`` :inbox_tray: **BUY " + trade.getAmount() + "** x **" + trade.getItem().getName() + "** for **" + Formatter.format(trade.getValue(), Style.ROUND_TO_TWO) + "**";
                        else
                            history = history + "\n> ``" + getFormatedDate(trade.getDate()) + "`` :outbox_tray: **SELL " + trade.getAmount() + "** x **" + trade.getItem().getName() + "** for **-" + Formatter.format(trade.getValue(), Style.ROUND_TO_TWO) + "**";

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

        String initial = String.valueOf(event.getComponentId().charAt(0));

        Item item = null;
        int quantity = 0;
                
        if (initial.equals("b") || initial.equals("s")) {

            item = MarketManager.getInstance().getItem(Material.getMaterial(event.getComponentId().substring(3)));
            quantity = Integer.parseInt(event.getComponentId().substring(1, 3));
            
        } else if (!initial.equals("i")) { return; }
        
        float value;

        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        double balance = Nascraft.getEconomy().getBalance(player);

        DiscordInventory discordInventory = DiscordInventories.getInstance().getInventory(uuid);

        switch (String.valueOf(event.getComponentId().charAt(0))) {

            case "b":

                value = item.getPrice().getBuyPrice()*quantity;

                if (balance < value) {

                    event.reply(lang.message(Message.DISCORD_INSUFFICIENT_BALANCE)
                                    .replace("[VALUE1]", Formatter.format((float) balance, Style.ROUND_TO_TWO))
                                    .replace("[VALUE2]", Formatter.format(value, Style.ROUND_TO_TWO)))
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

                Nascraft.getEconomy().withdrawPlayer(player, value);

                String buyText;
                if (quantity == 1) {
                    buyText = ":inbox_tray: You just bought **" + quantity + "** of **" + item.getName() + "** worth **" + Formatter.format(value, Style.ROUND_TO_TWO) + "**" +
                            "\n\n:coin: Your balance is now: **" + Formatter.format((float) Nascraft.getEconomy().getBalance(player), Style.ROUND_TO_TWO) + "**";
                } else {
                    buyText = ":inbox_tray: You just bought **" + quantity + "** of **" + item.getName() + "** worth **" + Formatter.format(value, Style.ROUND_TO_TWO) + "** (" + Formatter.format(value/quantity, Style.ROUND_TO_TWO) + " each)" +
                            "\n\n:coin: Your balance is now: **" + Formatter.format((float) Nascraft.getEconomy().getBalance(player), Style.ROUND_TO_TWO) + "**";
                }

                SQLite.getInstance().saveTrade(uuid, item, quantity, value, true, true);

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

                value = item.getPrice().getSellPrice()*quantity;

                Nascraft.getEconomy().depositPlayer(player, value);

                item.ghostSellItem(quantity);

                String sellText;
                if(quantity == 1) {
                    sellText = ":outbox_tray: You just sold **" + quantity + "** of **" + item.getName() + "** worth **" + Formatter.format(value, Style.ROUND_TO_TWO) + "**" +
                            "\n\n:coin: Your balance is now: **" + Formatter.format((float) Nascraft.getEconomy().getBalance(player), Style.ROUND_TO_TWO) + "**";
                } else {
                    sellText = ":outbox_tray: You just sold **" + quantity + "** of **" + item.getName() + "** worth **" + Formatter.format(value, Style.ROUND_TO_TWO) + "** (" + Formatter.format(value/quantity, Style.ROUND_TO_TWO) + " each)" +
                            "\n\n:coin: Your balance is now: **" + Formatter.format((float) Nascraft.getEconomy().getBalance(player), Style.ROUND_TO_TWO) + "**";
                }

                SQLite.getInstance().saveTrade(uuid, item, quantity, value, false, true);

                event.reply(sellText)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(6, TimeUnit.SECONDS));

                item.ghostSellItem(quantity);
                discordInventory.removeItem(item, quantity);

                break;

            case "i":

                float price = discordInventory.getNextSlotPrice();

                if (balance < price) {

                    event.reply(":x: You don't have enough money to buy another slot! You need **" +  Formatter.format(price, Style.ROUND_TO_TWO) + "**")
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