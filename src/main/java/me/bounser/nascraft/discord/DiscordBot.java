package me.bounser.nascraft.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class DiscordBot {

    private JDA jda;

    static DiscordBot instance;

    public DiscordBot(String botToken) throws InterruptedException {

        instance = this;

        jda = JDABuilder.createLight(botToken, Collections.emptyList())
                .addEventListeners(new DiscordListener(this))
                .setActivity(Activity.watching("prices move."))
                .setStatus(OnlineStatus.ONLINE)
                .build();

        jda.updateCommands().addCommands(
                Commands.slash("ping", "Calculate ping of the bot")
        ).queue();

        update();
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

        eb.setTitle("Live item market");

        eb.setImage("attachment://image.png");

        eb.setFooter("Select an item to get more details and to operate (Buy/sell) with it.");

        eb.setColor(new Color(43,45,49));

        return eb.build();
    }

    public StringSelectMenu getOptionsList() {

        StringSelectMenu.Builder builder = StringSelectMenu.create("menu:id");

        for(Item item : MarketManager.getInstance().getAllItemsInAlphabeticalOrder()) {
            builder.addOption(item.getName(), item.getMaterial(), item.getPrice().getValue() + Config.getInstance().getCurrency());
        }
        return builder.build();
    }

}
