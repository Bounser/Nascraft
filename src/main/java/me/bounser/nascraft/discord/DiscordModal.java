package me.bounser.nascraft.discord;

import me.bounser.nascraft.chart.cpi.ItemAndCPIChart;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.managers.ImagesManager;
import me.bounser.nascraft.discord.images.ItemAdvancedImage;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DiscordModal extends ListenerAdapter {

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {

        switch (event.getModalId()) {

            case "createalert":

                Item item = null;

                for (Item item1 : MarketManager.getInstance().getAllItems()) {
                    if (item1.getIdentifier().equalsIgnoreCase(event.getValue("material").getAsString()) ||
                            item1.getName().equalsIgnoreCase(event.getValue("material").getAsString())) {
                        item = item1; break;
                    }
                }

                if (item == null) {
                    event.reply(Lang.get().message(Message.DISCORD_ALERT_INVALID_MATERIAL))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }

                float price;

                try {
                    price = Float.parseFloat(event.getValue("price").getAsString());
                } catch (NumberFormatException e) {
                    event.reply(Lang.get().message(Message.DISCORD_ALERT_INVALID_PRICE))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
                    return;
                }

                switch (DiscordAlerts.getInstance().setAlert(event.getUser().getId(), item.getIdentifier(), price)) {

                    case SUCCESS:
                        event.reply(Lang.get().message(Message.DISCORD_ALERT_SUCCESS))
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                        break;

                    case LIMIT_REACHED:
                        event.reply(Lang.get().message(Message.DISCORD_ALERT_LIMIT_REACHED))
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                        break;

                    case REPEATED:
                        event.reply(Lang.get().message(Message.DISCORD_ALERT_ALREADY_LISTED))
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                        break;

                    case NOT_VALID:
                        event.reply(Lang.get().message(Message.DISCORD_ALERT_INVALID_MATERIAL))
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                        break;

                    default:
                        event.reply("Error")
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                        break;
                }

                return;

            case "advanced":

                Item advancedItem = null;

                for (Item itemCandidate : MarketManager.getInstance().getAllItems()) {
                    if (itemCandidate.getIdentifier().equalsIgnoreCase(event.getValue("material").getAsString()) ||
                            itemCandidate.getName().equalsIgnoreCase(event.getValue("material").getAsString())) {
                        advancedItem = itemCandidate; break;
                    }
                }

                if (advancedItem == null) {
                    event.reply(Lang.get().message(Message.DISCORD_ALERT_INVALID_MATERIAL))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                } else {
                    sendAdvancedMessage(advancedItem, event);
                }

                return;

            case "basic":

                Item itemBasic = null;

                for (Item itemCandidate : MarketManager.getInstance().getAllParentItems()) {
                    if (itemCandidate.getIdentifier().equalsIgnoreCase(event.getValue("material").getAsString()) ||
                            itemCandidate.getName().equalsIgnoreCase(event.getValue("material").getAsString())) {
                        itemBasic = itemCandidate; break;
                    }
                }

                if (itemBasic == null) {
                    event.reply(Lang.get().message(Message.DISCORD_ALERT_INVALID_MATERIAL))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                } else {
                    DiscordBot.getInstance().sendBasicScreen(itemBasic, event.getUser(), event, null, null);
                }

                return;

            case "compare-item-cpi":

                Item itemToCompare = null;

                for (Item itemCandidate : MarketManager.getInstance().getAllParentItems()) {
                    if (itemCandidate.getIdentifier().equalsIgnoreCase(event.getValue("material").getAsString()) ||
                            itemCandidate.getName().equalsIgnoreCase(event.getValue("material").getAsString())) {
                        itemToCompare = itemCandidate; break;
                    }
                }

                if (itemToCompare == null) {
                    event.reply(Lang.get().message(Message.DISCORD_ALERT_INVALID_MATERIAL))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                } else {
                    EmbedBuilder embedBuilderFlow = new EmbedBuilder();

                    embedBuilderFlow.setColor(new Color(100, 50, 150));

                    embedBuilderFlow.setImage("attachment://image.png");

                    event.replyEmbeds(embedBuilderFlow.build())
                            .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(ItemAndCPIChart.getImage(500, 300, itemToCompare)), "image.png"))
                            .setEphemeral(true)
                            .queue();
                }
        }
    }

    public void sendAdvancedMessage(Item item, ModalInteractionEvent modalEvent) {

        List<ItemComponent> componentList = new ArrayList<>();

        componentList.add(Button.secondary("long-" + item.getIdentifier(), "Long price").withEmoji(Emoji.fromFormatted("U+1F4C8")));
        componentList.add(Button.secondary("short-" + item.getIdentifier(), "Short price").withEmoji(Emoji.fromFormatted("U+1F4C9")));

        componentList.add(Button.secondary("future-" + item.getIdentifier(), "Futures").withEmoji(Emoji.fromFormatted("U+1F4D1")));
        componentList.add(Button.secondary("recurring-" + item.getIdentifier(), "Programmed actions").withEmoji(Emoji.fromFormatted("U+1F4C5")));

        modalEvent.replyFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(ItemAdvancedImage.getImage(item)), "image.png"))
                .addActionRow(componentList)
                .setEphemeral(true)
                .queue();
    }
}
