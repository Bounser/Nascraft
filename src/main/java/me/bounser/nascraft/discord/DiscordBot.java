package me.bounser.nascraft.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.managers.ImagesManager;
import me.bounser.nascraft.discord.images.ItemBasicImage;
import me.bounser.nascraft.discord.images.MainImage;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.exceptions.ContextException;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.FileUpload;

import java.awt.*;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DiscordBot {

    private float discordBuyTax;

    private float discordSellTax;

    private final JDA jda;

    static DiscordBot instance;

    public DiscordBot() {

        instance = this;

        jda = JDABuilder.createLight(Config.getInstance().getToken(), Collections.emptyList())
                .addEventListeners(new DiscordButtons())
                .addEventListeners(new DiscordCommands())
                .addEventListeners(new DiscordSelection())
                .addEventListeners(new DiscordModal())
                .setActivity(Activity.watching(Lang.get().message(Message.DISCORD_STATUS)))
                .setStatus(OnlineStatus.ONLINE)
                .build();

        List<CommandData> commandList = new ArrayList<>();

        if (Config.getInstance().getOptionAlertEnabled()) {
            commandList.add(Commands.slash("alert", "Set up an alert based on prices changes!")
                    .addOption(OptionType.STRING, "material", "Material name of the item.")
                    .addOption(OptionType.NUMBER, "price", "Price at which you will receive a mention."));

            commandList.add(Commands.slash("alerts", "List all your current alerts."));
            commandList.add(Commands.slash("remove-alert", "Remove an alert."));
        }

        commandList.add(Commands.slash("link", "Link a minecraft account to use all the functionalities"));
        commandList.add(Commands.slash("balance", "Checks available balance."));
        commandList.add(Commands.slash("inventory", "Check your inventory."));
        commandList.add(Commands.slash("search", "Search an item by name to operate with it.")
                .addOption(OptionType.STRING, "material", "Name (material) of the item."));
        commandList.add(Commands.slash("stop", "While the market is stopped no one will be able to buy or sell."));
        commandList.add(Commands.slash("resume", "Resume the market functionalities."));
        commandList.add(Commands.slash("seeinv", "See discord inventories of players by their discord ID.")
                .addOption(OptionType.STRING, "userid", "ID of the discord user"));
        commandList.add(Commands.slash("seebal", "See discord balances of players by their discord ID.")
                .addOption(OptionType.STRING, "userid", "ID of the discord user"));

        jda.updateCommands().addCommands(commandList).queue();

        removeAllMessages();

        discordBuyTax = Config.getInstance().getDiscordBuyTax();
        discordSellTax = Config.getInstance().getDiscordSellTax();
    }

    public JDA getJDA() { return jda; }

    public void update() {

        if (!Config.getInstance().getDiscordMenuEnabled()) return;

        jda.getGuilds().forEach(guild -> {

            TextChannel textChannel = guild.getTextChannelById(Config.getInstance().getChannel());

            if (textChannel == null) {
                Nascraft.getInstance().getLogger().info("textChannel is null. Check that the ID of the discord menu channel is correct. In case it is correct, check whether the bot is in more than one discord server (It should be in just one)");
                return;
            }

            textChannel.getHistoryFromBeginning(10).queue(
                    result -> {

                        if (result.size() > 2) {
                            removeAllMessages();
                        }
                    }
            );

            List<ItemComponent> componentList1 = new ArrayList<>();
            List<ItemComponent> componentList2 = new ArrayList<>();

            if (Config.getInstance().getOptionWikiEnabled()) componentList1.add(Button.primary("data", Emoji.fromFormatted("U+2754")));
            componentList1.add(Button.secondary("search", Lang.get().message(Message.DISCORD_BUTTON_1)).withEmoji(Emoji.fromFormatted("U+1F50D")));
            if (Config.getInstance().getOptionPersonalLogEnabled()) componentList1.add(Button.secondary("history", Lang.get().message(Message.DISCORD_BUTTON_2)).withEmoji(Emoji.fromFormatted("U+1F4DC")));
            if (Config.getInstance().getOptionCPIEnabled() || Config.getInstance().getOptionAlertEnabled()) componentList1.add(Button.secondary("advanced", Lang.get().message(Message.DISCORD_BUTTON_3)).withEmoji(Emoji.fromFormatted("U+1F4CA")));

            componentList2.add(Button.secondary("link", Lang.get().message(Message.DISCORD_BUTTON_4)).withEmoji(Emoji.fromFormatted("U+1F517")));
            componentList2.add(Button.secondary("inventory", Lang.get().message(Message.DISCORD_BUTTON_5)).withEmoji(Emoji.fromFormatted("U+1F392")));
            componentList2.add(Button.secondary("balance", Lang.get().message(Message.DISCORD_BUTTON_6)).withEmoji(Emoji.fromFormatted("U+1FA99")));

            if (Config.getInstance().getOptionSelectionEnabled()) {
                textChannel.sendMessageEmbeds(getEmbedded())
                        .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(MainImage.getImage()), "image.png"))
                        .addActionRow(getOptionsList())
                        .addActionRow(componentList1)
                        .addActionRow(componentList2)
                        .queue(message -> {
                            try {
                                message.delete().queueAfter(Config.getInstance().getUpdateTime(), TimeUnit.SECONDS);
                            } catch (ErrorResponseException ignored) { }
                        });
            } else {
                textChannel.sendMessageEmbeds(getEmbedded())
                        .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(MainImage.getImage()), "image.png"))
                        .addActionRow(componentList1)
                        .addActionRow(componentList2)
                        .queue(message -> {
                            try {
                                message.delete().queueAfter(Config.getInstance().getUpdateTime(), TimeUnit.SECONDS);
                            } catch (ErrorResponseException ignored) { }
                        });
            }
        });
    }

    public static DiscordBot getInstance() { return instance; }

    public MessageEmbed getEmbedded() {

        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(Lang.get().message(Message.DISCORD_MAIN_TITLE));

        eb.setImage("attachment://image.png");

        eb.setFooter(Lang.get().message(Message.DISCORD_MAIN_FOOTER));

        eb.setColor(getColorByValue(MarketManager.getInstance().getChange1h()));

        return eb.build();
    }

    public Color getColorByValue(float value) {
        value = Math.min(3, Math.max(-3, value));

        Color redColor = new Color(220, 70, 70);
        Color whiteColor = new Color(155, 125, 255);
        Color greenColor = new Color(70, 220, 70);

        int r = calculateIntermediateValue(redColor.getRed(), whiteColor.getRed(), greenColor.getRed(), value);
        int g = calculateIntermediateValue(redColor.getGreen(), whiteColor.getGreen(), greenColor.getGreen(), value);
        int b = calculateIntermediateValue(redColor.getBlue(), whiteColor.getBlue(), greenColor.getBlue(), value);

        return new Color(r, g, b);
    }

    private int calculateIntermediateValue(int start, int middle, int end, float value) {
        if (value < 0) {
            return (int) (start + (middle - start) * (1.0 + value / 3.0));
        } else {
            return (int) (middle + (end - middle) * (value / 3.0));
        }
    }

    public StringSelectMenu getOptionsList() {

        StringSelectMenu.Builder builder = StringSelectMenu.create("menu:id");

        List<Item> items = new ArrayList<>();

        if (MarketManager.getInstance().getAllItems().size() > 25) {
            for (Item item : MarketManager.getInstance().getTopGainers(8)) if(!items.contains(item)) items.add(item);
            for (Item item : MarketManager.getInstance().getMostTraded(8)) if(!items.contains(item)) items.add(item);
            for (Item item : MarketManager.getInstance().getTopDippers(8)) if(!items.contains(item)) items.add(item);
        } else {
            items = MarketManager.getInstance().getAllParentItemsInAlphabeticalOrder();
        }

        for (Item item : items)
            builder.addOption(item.getName(), item.getIdentifier(), Formatter.plainFormat(item.getCurrency(), item.getPrice().getValue(), Style.ROUND_BASIC) + " - " + Lang.get().message(Message.DISCORD_BUY) + ": " + Formatter.plainFormat(item.getCurrency(), item.getPrice().getBuyPrice(), Style.ROUND_BASIC)+ " " + Lang.get().message(Message.DISCORD_SELL) + ": " + Formatter.plainFormat(item.getCurrency(), item.getPrice().getSellPrice(), Style.ROUND_BASIC));

        return builder.build();
    }

    public void removeAllMessages() {

        if (!Config.getInstance().getDiscordMenuEnabled()) return;

        try {
            jda.awaitReady().getGuilds().forEach(guild -> {
                TextChannel textChannel = guild.getTextChannelById(Config.getInstance().getChannel());
                if (textChannel == null) {
                    Nascraft.getInstance().getLogger().info("textChannel is null #1");
                    return;
                }

                textChannel.getHistory().retrievePast(10).queue(messages -> {
                    if (!messages.isEmpty()) {
                        textChannel.purgeMessages(messages);
                        removeAllMessages();
                    }
                });
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendClosedMessage() {

        jda.getGuilds().forEach(guild -> {
            TextChannel textChannel = guild.getTextChannelById(Config.getInstance().getChannel());
            if (textChannel == null) {
                Nascraft.getInstance().getLogger().info("textChannel is null #2"); return;
            }
            textChannel.sendMessage(Lang.get().message(Message.DISCORD_MARKET_PAUSED)).queue();
        });
    }

    public void sendBasicScreen(Item item, User user, ModalInteractionEvent mEvent, StringSelectInteractionEvent sEvent, SlashCommandInteractionEvent cEvent) {

        Lang lang = Lang.get();

        List<ItemComponent> timeComponents = new ArrayList<>();

        if (Config.getInstance().getOptionGraphsEnabled()) {
            timeComponents.add(Button.secondary("time" + item.getIdentifier(), Lang.get().message(Message.DISCORD_DETAILED_GRAPH_OF)).asDisabled());
            timeComponents.add(Button.secondary("time1" + item.getIdentifier(), Lang.get().message(Message.DISCORD_1_DAY)));
            timeComponents.add(Button.secondary("time2" + item.getIdentifier(), Lang.get().message(Message.DISCORD_1_MONTH)));
            timeComponents.add(Button.secondary("time3" + item.getIdentifier(), Lang.get().message(Message.DISCORD_1_YEAR)));
            timeComponents.add(Button.secondary("time4" + item.getIdentifier(), Lang.get().message(Message.DISCORD_ALL)));
        }

        List<ItemComponent> componentList = new ArrayList<>();

        if (LinkManager.getInstance().getUUID(user.getId()) == null) {
            componentList.add(Button.success("b01" + item.getIdentifier(), lang.message(Message.DISCORD_BUY) + " 1 x " + Formatter.plainFormat(item.getCurrency(), item.getPrice().getBuyPrice(), Style.REDUCED_LENGTH)).asDisabled());
            componentList.add(Button.success("b32" + item.getIdentifier(), lang.message(Message.DISCORD_BUY) + " 32 x " + Formatter.plainFormat(item.getCurrency(), item.getPrice().getProjectedCost(-32, discordBuyTax), Style.REDUCED_LENGTH)).asDisabled());
            componentList.add(Button.danger("s32" + item.getIdentifier(), lang.message(Message.DISCORD_SELL) + " 32 x " + Formatter.plainFormat(item.getCurrency(), item.getPrice().getProjectedCost(32, discordSellTax), Style.REDUCED_LENGTH)).asDisabled());
            componentList.add(Button.danger("s01" + item.getIdentifier(), lang.message(Message.DISCORD_SELL) + " 1 x " + Formatter.plainFormat(item.getCurrency(), item.getPrice().getSellPrice(), Style.REDUCED_LENGTH)).asDisabled());
            componentList.add(Button.secondary("info" + item.getIdentifier(), Lang.get().message(Message.DISCORD_NOT_LINKED_SHORT)).withEmoji(Emoji.fromFormatted("U+1F517")));
        } else {
            componentList.add(Button.success("b01" + item.getIdentifier(), lang.message(Message.DISCORD_BUY) + " 1 x " + Formatter.plainFormat(item.getCurrency(), item.getPrice().getBuyPrice(), Style.REDUCED_LENGTH)));
            componentList.add(Button.success("b32" + item.getIdentifier(), lang.message(Message.DISCORD_BUY) + " 32 x " + Formatter.plainFormat(item.getCurrency(), item.getPrice().getProjectedCost(-32, discordBuyTax), Style.REDUCED_LENGTH)));
            componentList.add(Button.danger("s32" + item.getIdentifier(), lang.message(Message.DISCORD_SELL) + " 32 x " + Formatter.plainFormat(item.getCurrency(), item.getPrice().getProjectedCost(32, discordSellTax), Style.REDUCED_LENGTH)));
            componentList.add(Button.danger("s01" + item.getIdentifier(), lang.message(Message.DISCORD_SELL) + " 1 x " + Formatter.plainFormat(item.getCurrency(), item.getPrice().getSellPrice(), Style.REDUCED_LENGTH)));
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setColor(new Color(100, 50, 150));

        embedBuilder.setImage("attachment://image.png");

        if (mEvent != null)
            mEvent.replyEmbeds(embedBuilder.build())
                    .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(ItemBasicImage.getImage(item)), "image.png"))
                    .setEphemeral(true)
                    .addActionRow(timeComponents)
                    .addActionRow(componentList)
                    .queue(message -> message.editOriginalEmbeds(getBasicEditedEmbedded()).queueAfter(getSecondsRemainingToUpdate(), TimeUnit.SECONDS));

        if (sEvent != null)
            sEvent.replyEmbeds(embedBuilder.build())
                    .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(ItemBasicImage.getImage(item)), "image.png"))
                    .setEphemeral(true)
                    .addActionRow(timeComponents)
                    .addActionRow(componentList)
                    .queue(message -> message.editOriginalEmbeds(getBasicEditedEmbedded()).queueAfter(getSecondsRemainingToUpdate(), TimeUnit.SECONDS));

        if (cEvent != null)
            cEvent.replyEmbeds(embedBuilder.build())
                    .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(ItemBasicImage.getImage(item)), "image.png"))
                    .setEphemeral(true)
                    .addActionRow(timeComponents)
                    .addActionRow(componentList)
                    .queue(message -> message.editOriginalEmbeds(getBasicEditedEmbedded()).queueAfter(getSecondsRemainingToUpdate(), TimeUnit.SECONDS));
    }

    public void sendLinkLog(String id, UUID uuid, String nick, boolean linked) {

        try {
            jda.awaitReady().getGuilds().forEach(guild -> {
                TextChannel textChannel = guild.getTextChannelById(Config.getInstance().getLogChannel());

                if (textChannel == null) {
                    Nascraft.getInstance().getLogger().info("Log channel could not be found. (link log)");
                    return;
                }

                jda.retrieveUserById(id).queue(user -> {

                    String message = linked ? Lang.get().message(Message.DISCORD_LOG_LINKED) : Lang.get().message(Message.DISCORD_LOG_UNLINKED);

                    message = message
                            .replace("[UUID]", uuid.toString())
                            .replace("[NICK]", nick)
                            .replace("[ID]", id)
                            .replace("[DISCORD-NAME]", user.getName());

                    textChannel.sendMessage(message).queue();

                });

            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private MessageEmbed getBasicEditedEmbedded() {

        EmbedBuilder newEmbedBuilder = new EmbedBuilder();

        newEmbedBuilder.setTitle(Lang.get().message(Message.DISCORD_OUTDATED));

        newEmbedBuilder.setImage("attachment://image.png");

        newEmbedBuilder.setColor(new Color(200, 50, 50));

        return newEmbedBuilder.build();
    }

    private int getSecondsRemainingToUpdate() {
        LocalTime timeNow = LocalTime.now();

        LocalTime nextMinute = timeNow.plusMinutes(1).withSecond(0);
        Duration timeRemaining = Duration.between(timeNow, nextMinute);

        return (int) timeRemaining.getSeconds();
    }


    public static Color mixColors(Color color1, Color color2, Color color3, double weight1, double weight2, double weight3) {

        double totalWeight = weight1 + weight2 + weight3;

        double red = (color1.getRed() * weight1 + color2.getRed() * weight2 + color3.getRed() * weight3) / totalWeight;
        double green = (color1.getGreen() * weight1 + color2.getGreen() * weight2 + color3.getGreen() * weight3) / totalWeight;
        double blue = (color1.getBlue() * weight1 + color2.getBlue() * weight2 + color3.getBlue() * weight3) / totalWeight;

        return new Color((int) Math.round(red), (int) Math.round(green), (int) Math.round(blue));
    }

    public void setDiscordBuyTax(float newTax) { discordBuyTax = newTax; }
    public void setDiscordSellTax(float newTax) { discordSellTax = newTax; }

    public float getDiscordBuyTax() { return discordBuyTax; }
    public float getDiscordSellTax() { return discordSellTax; }

}
