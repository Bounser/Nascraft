package me.bounser.nascraft.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class DiscordBot {

    private final JDA jda;

    static DiscordBot instance;

    public DiscordBot() {

        instance = this;

        jda = JDABuilder.createLight(Config.getInstance().getToken(), Collections.emptyList())
                .addEventListeners(new DiscordListener())
                .addEventListeners(new DiscordCommands())
                .setActivity(Activity.watching("prices move."))
                .setStatus(OnlineStatus.ONLINE)
                .build();

        jda.updateCommands().addCommands(
                Commands.slash("ping", "Calculate ping of the bot."),
                Commands.slash("alert", "Setup an alert based on prices changes!")
                        .addOption(OptionType.STRING, "material", "Material name of the item.")
                        .addOption(OptionType.NUMBER, "price", "Price at which you will receive a mention."),
                Commands.slash("alerts", "List all your current alerts."),
                Commands.slash("remove-alert", "Remove an alert.")
                        .addOption(OptionType.STRING, "material", "Material name of the item to remove."),
                Commands.slash("link", "Link a minecraft account using the code received in-game with /link")
                        .addOption(OptionType.INTEGER, "code", "Code received using /link in minecraft."),
                Commands.slash("balance", "Checks available balance."),
                Commands.slash("inventory", "Check your inventory.")
        ).queue();

        removeLastMessage();
    }

    public JDA getJDA() { return jda; }

    public void update() {

        jda.getGuilds().forEach(guild -> {

            TextChannel textChannel = guild.getTextChannelById(Config.getInstance().getChannel());

            if (textChannel == null) {
                Nascraft.getInstance().getLogger().info("textChannel is null");
                return;
            }

            File outputfile = new File("image.png");
            try {
                ImageIO.write(ImageBuilder.getInstance().getMainImage(), "png", outputfile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            textChannel.sendMessageEmbeds(getEmbedded()).addFiles(FileUpload.fromData(outputfile, "image.png")).addActionRow(getOptionsList()).queue(message -> {

                message.delete().queueAfter(60, TimeUnit.SECONDS);

            });

        });
    }

    public static DiscordBot getInstance() { return instance; }

    public MessageEmbed getEmbedded() {

        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle(":green_circle:  Live item market");

        eb.setImage("attachment://image.png");

        eb.setFooter("Select an item to get more details and to operate (Buy/sell) with it. \nPrices in operations may vary.");

        eb.setColor(new Color(120, 176, 88));

        return eb.build();
    }

    public StringSelectMenu getOptionsList() {

        StringSelectMenu.Builder builder = StringSelectMenu.create("menu:id");

        List<Item> items = new ArrayList<>();

        for (Item item : MarketManager.getInstance().getTopGainers(8)) if(!items.contains(item)) items.add(item);
        for (Item item : MarketManager.getInstance().getMostTraded(8)) if(!items.contains(item)) items.add(item);
        for (Item item : MarketManager.getInstance().getTopDippers(8)) if(!items.contains(item)) items.add(item);

        for (Item item : items)
            builder.addOption(item.getName(), item.getMaterial(), item.getPrice().getValue() + Config.getInstance().getCurrency());

        return builder.build();
    }

    public void removeLastMessage() {

        try {

            jda.awaitReady().getGuilds().forEach(guild -> {
                TextChannel textChannel = guild.getTextChannelById(Config.getInstance().getChannel());

                if (textChannel == null) {
                    Nascraft.getInstance().getLogger().info("textChannel is null");
                    return;
                }

                textChannel.getHistory().size();

                String lastMessageID = textChannel.getLatestMessageId();

                textChannel.getHistory().retrievePast(1).queue(messages -> {
                    if (!messages.isEmpty())
                        textChannel.retrieveMessageById(lastMessageID).queue(message ->{ message.delete().queue(); });
                });

            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
