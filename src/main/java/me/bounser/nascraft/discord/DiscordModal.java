package me.bounser.nascraft.discord;

import me.bounser.nascraft.discord.images.ItemAdvancedImage;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DiscordModal extends ListenerAdapter {

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {

        if (event.getModalId().equals("createalert")) {

            Item item = null;

            for (Item item1 : MarketManager.getInstance().getAllItems()) {
                if (item1.getMaterial().toString().equalsIgnoreCase(event.getValue("material").getAsString()) ||
                        item1.getName().equalsIgnoreCase(event.getValue("material").getAsString())) {
                    item = item1; break;
                }
            }

            if (item == null) {
                event.reply("That material is not valid")
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            float price;

            try {
                price = Float.parseFloat(event.getValue("price").getAsString());
            } catch (NumberFormatException e) {
                event.reply("That price is not valid!")
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));
                return;
            }

            switch (DiscordAlerts.getInstance().setAlert(event.getUser().getId(), item.getMaterial().toString(), price)) {

                case "success":
                    event.reply("Success! You will receive a DM when the price reaches the price.")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    break;

                case "repeated":
                    event.reply("That item is already on you watchlist!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    break;

                case "not_valid":
                    event.reply("Item not recognized!")
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
                if (itemCandidate.getMaterial().toString().equalsIgnoreCase(event.getValue("material").getAsString()) ||
                        itemCandidate.getName().equalsIgnoreCase(event.getValue("material").getAsString())) {
                    item = itemCandidate; break;
                }
            }

            if (item == null) {
                event.reply("Item not recognized!")
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
            } else {
                sendAdvancedMessage(item, event);
            }
            return;
        }

        if (event.getModalId().equals("basic")) {

            Item item = null;

            for (Item itemCandidate : MarketManager.getInstance().getAllItems()) {
                if (itemCandidate.getMaterial().toString().equalsIgnoreCase(event.getValue("material").getAsString()) ||
                        itemCandidate.getName().equalsIgnoreCase(event.getValue("material").getAsString())) {
                    item = itemCandidate; break;
                }
            }

            if (item == null) {
                event.reply("Item not recognized!")
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
            } else {
                DiscordBot.getInstance().sendBasicScreen(item, event.getUser(), event, null, null);
            }
        }
    }

    public void sendAdvancedMessage(Item item, ModalInteractionEvent modalEvent) {

        List<ItemComponent> componentList = new ArrayList<>();

        componentList.add(Button.secondary("long-" + item.getMaterial(), "Long price").withEmoji(Emoji.fromFormatted("U+1F4C8")));
        componentList.add(Button.secondary("short-" + item.getMaterial(), "Short price").withEmoji(Emoji.fromFormatted("U+1F4C9")));

        componentList.add(Button.secondary("future-" + item.getMaterial(), "Futures").withEmoji(Emoji.fromFormatted("U+1F4D1")));
        componentList.add(Button.secondary("recurring-" + item.getMaterial(), "Programmed actions").withEmoji(Emoji.fromFormatted("U+1F4C5")));

        File outputfile = new File("image.png");
        try {
            ImageIO.write(ItemAdvancedImage.getImage(item), "png", outputfile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        modalEvent.replyFiles(FileUpload.fromData(outputfile , "image.png"))
                .addActionRow(componentList)
                .setEphemeral(true)
                .queue();
    }
}
