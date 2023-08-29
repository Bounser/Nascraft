package me.bounser.nascraft.discord;

import jdk.javadoc.internal.doclets.toolkit.taglets.snippet.Style;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.market.RoundUtils;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class DiscordListener extends ListenerAdapter {


    public static DiscordListener instance;

    public DiscordListener() { instance = this; }

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
                                event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time)
                        ).queue();
                break;

            case "alert":

                switch (DiscordAlerts.getInstance().setAlert(event.getUser().getId(), event.getOption("material").getAsString(), (float) event.getOption("price").getAsDouble())) {

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

            case "alerts":

                if (DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()) == null ||
                        DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).size() < 1) {
                    event.reply(":x: You don't have any alert setup!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                String alerts = ":bell: Active alerts:\n ";

                for (Item item : DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).keySet())
                    alerts = alerts + "\n**" + item.getName() + "** at price: **" + Math.abs(DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).get(item)) + Config.getInstance().getCurrency() + "**";

                event.reply(alerts)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                break;

            case "remove-alert":

                if (DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()) == null ||
                        DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).size() < 1) {
                    event.reply(":x: You don't have any alert setup already!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                Item item = MarketManager.getInstance().getItem(event.getOption("material").getAsString().replace(" ", "_"));

                if (item == null) {
                    event.reply(":x: Item not recognized.")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                switch (DiscordAlerts.getInstance().removeAlert(event.getUser().getId(), item)) {

                    case "not_found":
                        event.reply(":x: Item is not in your watchlist.")
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                        break;

                    case "success":
                        event.reply(":no_bell: Alert for **" + event.getOption("material").getAsString().replace(" ", "_") + "** removed.")
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                        break;
                }

            case "link":

                if (LinkManager.getInstance().getUUID(event.getUser()) != null) {

                    event.reply(":link: Already linked! (With UUID " + LinkManager.getInstance().getUUID(event.getUser()) + ")")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                if (LinkManager.getInstance().redeemCode(event.getOption("code").getAsInt(), event.getUser())) {

                    event.reply(":white_check_mark: Success! Account linked.")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                } else {

                    event.reply(":negative_squared_cross_mark: Error! Something went wrong while retrieving your code.")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                }
                break;

            case "balance":

                if (LinkManager.getInstance().getUUID(event.getUser()) != null) {

                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(LinkManager.getInstance().getUUID(event.getUser())));

                    event.reply(":coin: You have: **" + RoundUtils.round((float) Nascraft.getEconomy().getBalance(player)) + Config.getInstance().getCurrency() + "**")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                } else {
                    event.reply(":x: You don't have any account linked!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                }
                break;

            case "inventory":

                if (LinkManager.getInstance().getUUID(event.getUser()) == null) {
                    event.reply(":x: You don't have any account linked!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                if (DiscordInventories.getInstance().getInventory(event.getUser().getId()) == null ||
                        DiscordInventories.getInstance().getInventory(event.getUser().getId()).size() == 0) {
                    event.reply(":jar: Your " + DiscordInventories.getInstance().getCapacity(event.getUser().getId()) + " slots are all empty!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                String inventory = ":package: Your inventory:\n ";

                for (Item inventoryItem : DiscordInventories.getInstance().getInventory(event.getUser().getId()).keySet())
                    if (inventoryItem != null)
                        inventory = inventory + "\n**" + inventoryItem.getName() + "** x **" + DiscordInventories.getInstance().getInventory(event.getUser().getId()).get(inventoryItem) + "** ";

                event.reply(inventory)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                break;

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

            if(LinkManager.getInstance().getUUID(event.getUser()) == null) {
                componentList.add(Button.success("b01" + item.getMaterial(), "BUY 1").asDisabled());
                componentList.add(Button.success("b32" + item.getMaterial(), "BUY 32").asDisabled());
                componentList.add(Button.danger("s32" + item.getMaterial(), "SELL 32").asDisabled());
                componentList.add(Button.danger("s01" + item.getMaterial(), "SELL 1").asDisabled());
                componentList.add(Button.secondary("info" + item.getMaterial(), "Not linked!").withEmoji(Emoji.fromFormatted("U+1F517")));
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

                embedBuilder.setTitle(":warning: Caution: information outdated! :warning:");

                embedBuilder.setColor(new Color(200, 50, 50));

                message.editOriginalEmbeds(embedBuilder.build()).queueAfter(timeRemaining.getSeconds(), TimeUnit.SECONDS);

            });
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        if (LinkManager.getInstance().getUUID(event.getUser()) == null) {
            event.reply(":exclamation: Your account is not linked! Run ``/link`` in-game to start the process of linking accounts.").setEphemeral(true).queue(message -> {
                message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
            });
            return;
        }

        Item item = MarketManager.getInstance().getItem(event.getComponentId().substring(3));
        int quantity = Integer.parseInt(event.getComponentId().substring(1, 3));
        float value;

        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(LinkManager.getInstance().getUUID(event.getUser())));
        double balance = Nascraft.getEconomy().getBalance(player);

        switch (String.valueOf(event.getComponentId().charAt(0))) {

            case "b":

                value = item.getPrice().getBuyPrice()*quantity;

                if (balance < value) {

                    event.reply(":x: Not enough money! Your current balance is: **" + RoundUtils.round((float) balance) + Config.getInstance().getCurrency() + "** and you need at least **" + value + Config.getInstance().getCurrency() + "**")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));

                    return;
                }

                if (!DiscordInventories.getInstance().hasSpace(event.getUser().getId(), item)) {
                    event.reply(":x: Not enough space in your discord inventory!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));

                    return;
                }

                Nascraft.getEconomy().withdrawPlayer(player, value);

                event.reply(":inbox_tray: You just bought **" + quantity + "** of **" + item.getName() + "** worth **" + value + Config.getInstance().getCurrency() + "**" +
                                "\n\n:coin: Your balance is now: **" + RoundUtils.round((float) Nascraft.getEconomy().getBalance(player)) + Config.getInstance().getCurrency() + "**")
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                item.ghostBuyItem(quantity);
                DiscordInventories.getInstance().addItem(event.getUser().getId(), item, quantity);

                break;

            case "s":

                if (!DiscordInventories.getInstance().hasItem(event.getUser().getId(), item, quantity)) {
                    event.reply(":x: You don't have this item in your discord inventory!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS));

                    return;
                }

                value = item.getPrice().getSellPrice()*quantity;

                Nascraft.getEconomy().depositPlayer(player, value);

                item.ghostSellItem(Integer.parseInt(event.getComponentId().substring(1, 3)));
                event.reply(":outbox_tray: You just sold **" + event.getComponentId().substring(1, 3) + "** of **" + item.getName() + "** worth **" + (item.getPrice().getSellPrice()*Integer.valueOf(event.getComponentId().substring(1, 3))) + Config.getInstance().getCurrency() + "** " +
                                "\n\n:coin: Your balance is now: **" + RoundUtils.round((float) Nascraft.getEconomy().getBalance(player)) + Config.getInstance().getCurrency() + "**").setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                item.ghostSellItem(quantity);
                DiscordInventories.getInstance().removeItem(event.getUser().getId(), item, quantity);

                break;

        }
    }
}
