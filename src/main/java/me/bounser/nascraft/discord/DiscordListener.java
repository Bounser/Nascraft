package me.bounser.nascraft.discord;

import jdk.javadoc.internal.doclets.toolkit.taglets.snippet.Style;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class DiscordListener extends ListenerAdapter {

    private DiscordBot discordBot;

    private String messageID;

    private Guild guild;

    private TextChannel channel;

    public static DiscordListener instance;

    public DiscordListener(DiscordBot discordBot) {

        this.discordBot = discordBot;
        instance = this;
    }

    public static DiscordListener getInstance() { return instance; }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping":
                long time = System.currentTimeMillis();
                event.reply("Pong!").setEphemeral(true)
                        .flatMap(v ->
                                event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                        ).queue();
                break;
            case "inventory":


                break;
            case "alert":

                switch (DiscordAlerts.getInstance().setAlert(event.getUser(), event.getOption("material").getAsString(), (float) event.getOption("price").getAsDouble())) {

                    case "success":
                        event.reply("Success!").setEphemeral(true).queue();
                        break;

                    case "repeated":
                        event.reply("That item is already on you watchlist!").setEphemeral(true).queue();
                        break;

                    case "not_valid":
                        event.reply("Item not recognized!").setEphemeral(true).queue();
                        break;

                    default:
                        event.reply("error").setEphemeral(true).queue();
                        break;

                }
            case "link":

                if (!LinkManager.getInstance().getUUID(event.getUser()).equals("-1")) {
                    event.reply("Already linked! (With UUID " + LinkManager.getInstance().getUUID(event.getUser()) + ")").setEphemeral(true).queue(message -> {
                        message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS);
                    });
                    return;
                }

                if (LinkManager.getInstance().redeemCode(event.getOption("code").getAsInt(), event.getUser())) {
                    event.reply("Success! Account linked.").setEphemeral(true).queue(message -> {
                        message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS);
                    });
                } else {
                    event.reply("Error! Something went wrong.").setEphemeral(true).queue(message -> {
                        message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS);
                    });
                }

        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {

        if (event.getChannel().getId().equals(Config.getInstance().getChannel())) {

            Item item = MarketManager.getInstance().getItem(event.getValues().get(0));

            File outputfile = new File("image.png");
            try {
                ImageIO.write(ImageBuilder.getInstance().getImageOfItem(item), "png", outputfile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            List<ItemComponent> componentList = new ArrayList<>();

            if(LinkManager.getInstance().getUUID(event.getUser()).equals("-1")) {
                componentList.add(Button.success("b01" + item.getMaterial(), "BUY 1").asDisabled());
                componentList.add(Button.success("b32" + item.getMaterial(), "BUY 32").asDisabled());
                componentList.add(Button.danger("s32" + item.getMaterial(), "SELL 32").asDisabled());
                componentList.add(Button.danger("s01" + item.getMaterial(), "SELL 1").asDisabled());
                componentList.add(Button.secondary("info" + item.getMaterial(), "Not linked!"));
            } else {
                componentList.add(Button.success("b01" + item.getMaterial(), "BUY 1"));
                componentList.add(Button.success("b32" + item.getMaterial(), "BUY 32"));
                componentList.add(Button.danger("s32" + item.getMaterial(), "SELL 32"));
                componentList.add(Button.danger("s01" + item.getMaterial(), "SELL 1"));
            }

            event.replyFiles(FileUpload.fromData(outputfile , "image.png")).setEphemeral(true).addActionRow(componentList)
                    .queue(message -> {

                LocalTime timeNow = LocalTime.now();

                LocalTime nextMinute = timeNow.plusMinutes(1).withSecond(0);
                Duration timeRemaining = Duration.between(timeNow, nextMinute);

                EmbedBuilder embedBuilder = new EmbedBuilder();

                embedBuilder.setTitle(":warning: Caution: information outdated!");

                message.editOriginalEmbeds(embedBuilder.build()).queueAfter(timeRemaining.getSeconds(), TimeUnit.SECONDS);

            });
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        if (LinkManager.getInstance().getUUID(event.getUser()).equals("-1")) {
            event.reply("Your account is not linked! Run ``/link`` in-game to start the process of linking accounts.").setEphemeral(true).queue(message -> {
                message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
            });
            return;
        }

        Item item = MarketManager.getInstance().getItem(event.getComponentId().substring(3));

        LocalTime timeNow = LocalTime.now();

        LocalTime nextMinute = timeNow.plusMinutes(1).withSecond(0);
        Duration timeRemaining = Duration.between(timeNow, nextMinute);

        switch (String.valueOf(event.getComponentId().charAt(0))) {

            case "b":

                item.ghostBuyItem(Integer.parseInt(event.getComponentId().substring(1, 3)));
                event.reply("You just bought " + event.getComponentId().substring(1, 3) + " of " + item.getName() + " worth " + (item.getPrice().getBuyPrice()*Integer.valueOf(event.getComponentId().substring(1, 3))) + Config.getInstance().getCurrency()).setEphemeral(true)
                        .queue(message -> { message.deleteOriginal().queueAfter(timeRemaining.getSeconds(), TimeUnit.SECONDS); });
                break;

            case "s":

                item.ghostSellItem(Integer.parseInt(event.getComponentId().substring(1, 3)));
                event.reply("You just sold " + event.getComponentId().substring(1, 3) + " of " + item.getName() + " worth " + (item.getPrice().getSellPrice()*Integer.valueOf(event.getComponentId().substring(1, 3))) + Config.getInstance().getCurrency()).setEphemeral(true)
                        .queue(message -> { message.deleteOriginal().queueAfter(timeRemaining.getSeconds(), TimeUnit.SECONDS); });
                break;

        }
    }
}
