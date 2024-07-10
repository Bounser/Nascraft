package me.bounser.nascraft.discord;

import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.discord.images.ImagesManager;
import me.bounser.nascraft.discord.images.ItemAdvancedImage;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DiscordModal extends ListenerAdapter {

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {

        if (event.getModalId().equals("createalert")) {

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
        }

        if (event.getModalId().equals("advanced")) {

            Item item = null;

            for (Item itemCandidate : MarketManager.getInstance().getAllItems()) {
                if (itemCandidate.getIdentifier().equalsIgnoreCase(event.getValue("material").getAsString()) ||
                        itemCandidate.getName().equalsIgnoreCase(event.getValue("material").getAsString())) {
                    item = itemCandidate; break;
                }
            }

            if (item == null) {
                event.reply(Lang.get().message(Message.DISCORD_ALERT_INVALID_MATERIAL))
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
            } else {
                sendAdvancedMessage(item, event);
            }
            return;
        }

        if (event.getModalId().equals("basic")) {

            Item item = null;

            for (Item itemCandidate : MarketManager.getInstance().getAllParentItems()) {
                if (itemCandidate.getIdentifier().equalsIgnoreCase(event.getValue("material").getAsString()) ||
                        itemCandidate.getName().equalsIgnoreCase(event.getValue("material").getAsString())) {
                    item = itemCandidate; break;
                }
            }

            if (item == null) {
                event.reply(Lang.get().message(Message.DISCORD_ALERT_INVALID_MATERIAL))
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
            } else {
                DiscordBot.getInstance().sendBasicScreen(item, event.getUser(), event, null, null);
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
