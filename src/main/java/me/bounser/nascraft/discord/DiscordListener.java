package me.bounser.nascraft.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.inventories.DiscordInventories;
import me.bounser.nascraft.discord.inventories.DiscordInventory;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
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

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class DiscordListener extends ListenerAdapter {


    public static DiscordListener instance;

    private Lang lang = Lang.get();

    public DiscordListener() { instance = this; }

    public static DiscordListener getInstance() { return instance; }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {

        if (event.getChannel().getId().equals(Config.getInstance().getChannel())) {

            if (event.getValues().get(0).contains("alert-")) {

                DiscordAlerts.getInstance().removeAlert(event.getUser().getId(), MarketManager.getInstance().getItem(event.getValues().get(0).substring(6)));

                event.reply(":no_bell: Alert of item ``" + event.getValues().get(0).substring(6) + "`` removed!")
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));

                return;
            }

            Item item = MarketManager.getInstance().getItem(event.getValues().get(0));

            File outputfile = new File("image.png");
            try {
                ImageIO.write(ImageBuilder.getInstance().getImageOfItem(item), "png", outputfile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            List<ItemComponent> componentList = new ArrayList<>();

            if (LinkManager.getInstance().getUUID(event.getUser()) == null) {
                componentList.add(Button.success("b01" + item.getMaterial(), lang.message(Message.DISCORD_BUY) + " 1 x " + Formatter.format(item.getPrice().getBuyPrice(), Style.REDUCED_LENGTH)).asDisabled());
                componentList.add(Button.success("b32" + item.getMaterial(), lang.message(Message.DISCORD_BUY) + " 32 x " + Formatter.format(item.getPrice().getBuyPrice()*32, Style.REDUCED_LENGTH)).asDisabled());
                componentList.add(Button.danger("s32" + item.getMaterial(), lang.message(Message.DISCORD_SELL) + " 32 x " + Formatter.format(item.getPrice().getSellPrice()*32, Style.REDUCED_LENGTH)).asDisabled());
                componentList.add(Button.danger("s01" + item.getMaterial(), lang.message(Message.DISCORD_SELL) + " 1 x " + Formatter.format(item.getPrice().getSellPrice(), Style.REDUCED_LENGTH)).asDisabled());
                componentList.add(Button.secondary("info" + item.getMaterial(), "Not linked!").withEmoji(Emoji.fromFormatted("U+1F517")));
            } else {
                componentList.add(Button.success("b01" + item.getMaterial(), lang.message(Message.DISCORD_BUY) + " 1 x " + Formatter.format(item.getPrice().getBuyPrice(), Style.REDUCED_LENGTH)));
                componentList.add(Button.success("b32" + item.getMaterial(), lang.message(Message.DISCORD_BUY) + " 32 x " + Formatter.format(item.getPrice().getBuyPrice()*32, Style.REDUCED_LENGTH)));
                componentList.add(Button.danger("s32" + item.getMaterial(), lang.message(Message.DISCORD_SELL) + " 32 x " + Formatter.format(item.getPrice().getSellPrice()*32, Style.REDUCED_LENGTH)));
                componentList.add(Button.danger("s01" + item.getMaterial(), lang.message(Message.DISCORD_SELL) + " 1 x " + Formatter.format(item.getPrice().getSellPrice(), Style.REDUCED_LENGTH)));
            }

            event.replyFiles(FileUpload.fromData(outputfile , "image.png"))
                    .setEphemeral(true)
                    .addActionRow(componentList)
                    .queue(message -> {

                LocalTime timeNow = LocalTime.now();

                LocalTime nextMinute = timeNow.plusMinutes(1).withSecond(0);
                Duration timeRemaining = Duration.between(timeNow, nextMinute);

                EmbedBuilder embedBuilder = new EmbedBuilder();

                embedBuilder.setTitle(lang.message(Message.DISCORD_OUTDATED));

                embedBuilder.setColor(new Color(200, 50, 50));

                message.editOriginalEmbeds(embedBuilder.build()).queueAfter(timeRemaining.getSeconds(), TimeUnit.SECONDS);

            });
        }
    }

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
                            .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));

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

            case "1info":

                event.reply("In this market you will be able to buy and sell items. Even if you are not connected!")
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS));


                return;
        }

        if (LinkManager.getInstance().getUUID(event.getUser()) == null) {
            event.reply(":exclamation: Your account is not linked! Run ``/link`` in-game to start the process of linking accounts.")
                    .setEphemeral(true)
                    .queue(message -> {
                message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
            });
            return;
        }

        if (event.getComponentId().equals("inventory")) {

            File outputfile = new File("image.png");
            try {
                ImageIO.write(ImageBuilder.getInstance().getInventory(event.getUser()), "png", outputfile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            DiscordInventory discordInventory = DiscordInventories.getInstance().getInventory(event.getUser().getId());

            if (discordInventory.getCapacity() < 40) {

                event.replyFiles(FileUpload.fromData(outputfile , "image.png"))
                        .setEphemeral(true)
                        .addActionRow(Button.success("i_buy", "Buy slot for " + discordInventory.getNextSlotPrice() + Lang.get().message(Message.CURRENCY)))
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
        }

        if (event.getComponentId().equals("balance")) {

            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(LinkManager.getInstance().getUUID(event.getUser())));

            event.reply(":coin: You have: **" + RoundUtils.round((float) Nascraft.getEconomy().getBalance(player)) + Lang.get().message(Message.CURRENCY) + "**")
                    .setEphemeral(true)
                    .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

        }

        String initial = String.valueOf(event.getComponentId().charAt(0));

        Item item = null;
        int quantity = 0;
                
        if (initial.equals("b") || initial.equals("s")) {

            item = MarketManager.getInstance().getItem(event.getComponentId().substring(3));
            quantity = Integer.parseInt(event.getComponentId().substring(1, 3));
            
        }
        
        float value;

        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(LinkManager.getInstance().getUUID(event.getUser())));
        double balance = Nascraft.getEconomy().getBalance(player);

        DiscordInventory discordInventory = DiscordInventories.getInstance().getInventory(event.getUser().getId());

        switch (String.valueOf(event.getComponentId().charAt(0))) {

            case "b":

                value = item.getPrice().getBuyPrice()*quantity;

                if (balance < value) {

                    event.reply(lang.message(Message.DISCORD_INSUFFICIENT_BALANCE)
                                    .replace("[VALUE1]", String.valueOf(RoundUtils.round((float) balance)))
                                    .replace("[VALUE2]", String.valueOf(value))
                                    .replace("[CURRENCY]", Lang.get().message(Message.CURRENCY)))
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

                event.reply(":inbox_tray: You just bought **" + quantity + "** of **" + item.getName() + "** worth **" + value + Lang.get().message(Message.CURRENCY) + "**" +
                                "\n\n:coin: Your balance is now: **" + RoundUtils.round((float) Nascraft.getEconomy().getBalance(player)) + Lang.get().message(Message.CURRENCY) + "**")
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

                item.ghostSellItem(Integer.parseInt(event.getComponentId().substring(1, 3)));
                event.reply(":outbox_tray: You just sold **" + event.getComponentId().substring(1, 3) + "** of **" + item.getName() + "** worth **" + (item.getPrice().getSellPrice()*Integer.valueOf(event.getComponentId().substring(1, 3))) + Lang.get().message(Message.CURRENCY) + "** " +
                                "\n\n:coin: Your balance is now: **" + RoundUtils.round((float) Nascraft.getEconomy().getBalance(player)) + Lang.get().message(Message.CURRENCY) + "**")
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(6, TimeUnit.SECONDS));

                item.ghostSellItem(quantity);
                discordInventory.removeItem(item, quantity);

                break;

            case "i":

                float price = discordInventory.getNextSlotPrice();

                if (balance < price) {

                    event.reply(":x: You don't have enough money to buy another slot! You need **" +  price + Lang.get().message(Message.CURRENCY) + "**")
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

    @Override
    public void onModalInteraction(@Nonnull ModalInteractionEvent event) {
        if (event.getModalId().equals("createalert")) {

            Item item = null;

            for (Item item1 : MarketManager.getInstance().getAllItems()) {
                if (item1.getMaterial().equalsIgnoreCase(event.getValue("material").getAsString()) ||
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

            switch (DiscordAlerts.getInstance().setAlert(event.getUser().getId(), item.getMaterial(), price)) {

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
        }
    }
}
