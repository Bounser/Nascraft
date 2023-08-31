package me.bounser.nascraft.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.market.RoundUtils;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class DiscordListener extends ListenerAdapter {


    public static DiscordListener instance;

    public DiscordListener() { instance = this; }

    public static DiscordListener getInstance() { return instance; }

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
                componentList.add(Button.success("b01" + item.getMaterial(), "BUY 1 x " + item.getPrice().getBuyPrice() + Config.getInstance().getCurrency()).asDisabled());
                componentList.add(Button.success("b32" + item.getMaterial(), "BUY 32 x " + RoundUtils.round(item.getPrice().getBuyPrice()*32) + Config.getInstance().getCurrency()).asDisabled());
                componentList.add(Button.danger("s32" + item.getMaterial(), "SELL 32 x " + RoundUtils.round(item.getPrice().getSellPrice()*32) + Config.getInstance().getCurrency()).asDisabled());
                componentList.add(Button.danger("s01" + item.getMaterial(), "SELL 1 x " + item.getPrice().getSellPrice() + Config.getInstance().getCurrency()).asDisabled());
                componentList.add(Button.secondary("info" + item.getMaterial(), "Not linked!").withEmoji(Emoji.fromFormatted("U+1F517")));
            } else {
                componentList.add(Button.success("b01" + item.getMaterial(), "BUY 1 x " + item.getPrice().getBuyPrice() + Config.getInstance().getCurrency()));
                componentList.add(Button.success("b32" + item.getMaterial(), "BUY 32 x " + RoundUtils.round(item.getPrice().getBuyPrice()*32) + Config.getInstance().getCurrency()));
                componentList.add(Button.danger("s32" + item.getMaterial(), "SELL 32 x " + RoundUtils.round(item.getPrice().getSellPrice()*32) + Config.getInstance().getCurrency()));
                componentList.add(Button.danger("s01" + item.getMaterial(), "SELL 1 x " + item.getPrice().getSellPrice() + Config.getInstance().getCurrency()));
            }

            event.replyFiles(FileUpload.fromData(outputfile , "image.png"))
                    .setEphemeral(true)
                    .addActionRow(componentList)
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

        if (!event.getChannel().getId().equals(Config.getInstance().getChannel())) { return; }

        if (LinkManager.getInstance().getUUID(event.getUser()) == null) {
            event.reply(":exclamation: Your account is not linked! Run ``/link`` in-game to start the process of linking accounts.")
                    .setEphemeral(true)
                    .queue(message -> {
                message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
            });
            return;
        }

        String initial = String.valueOf(event.getComponentId().charAt(0));

        Item item = null;
        int quantity = 0;
                
        if(initial.equals("b") || initial.equals("s")) {

            item = MarketManager.getInstance().getItem(event.getComponentId().substring(3));
            quantity = Integer.parseInt(event.getComponentId().substring(1, 3));
            
        }
        
        float value;

        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(LinkManager.getInstance().getUUID(event.getUser())));
        double balance = Nascraft.getEconomy().getBalance(player);

        switch (String.valueOf(event.getComponentId().charAt(0))) {

            case "b":

                value = item.getPrice().getBuyPrice()*quantity;

                if (balance < value) {

                    event.reply(":x: Not enough money! Your current balance is: **" + RoundUtils.round((float) balance) + Config.getInstance().getCurrency() + "** and you need at least **" + value + Config.getInstance().getCurrency() + "**")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));

                    return;
                }

                if (!DiscordInventories.getInstance().hasSpace(event.getUser().getId(), item, quantity)) {
                    event.reply(":x: Not enough space in your discord inventory!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));

                    return;
                }

                Nascraft.getEconomy().withdrawPlayer(player, value);

                event.reply(":inbox_tray: You just bought **" + quantity + "** of **" + item.getName() + "** worth **" + value + Config.getInstance().getCurrency() + "**" +
                                "\n\n:coin: Your balance is now: **" + RoundUtils.round((float) Nascraft.getEconomy().getBalance(player)) + Config.getInstance().getCurrency() + "**")
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(6, TimeUnit.SECONDS));

                item.ghostBuyItem(quantity);
                DiscordInventories.getInstance().addItem(event.getUser().getId(), item, quantity);

                break;

            case "s":

                if (!DiscordInventories.getInstance().hasItem(event.getUser().getId(), item, quantity)) {
                    event.reply(":x: You don't have this item in your discord inventory!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));

                    return;
                }

                value = item.getPrice().getSellPrice()*quantity;

                Nascraft.getEconomy().depositPlayer(player, value);

                item.ghostSellItem(Integer.parseInt(event.getComponentId().substring(1, 3)));
                event.reply(":outbox_tray: You just sold **" + event.getComponentId().substring(1, 3) + "** of **" + item.getName() + "** worth **" + (item.getPrice().getSellPrice()*Integer.valueOf(event.getComponentId().substring(1, 3))) + Config.getInstance().getCurrency() + "** " +
                                "\n\n:coin: Your balance is now: **" + RoundUtils.round((float) Nascraft.getEconomy().getBalance(player)) + Config.getInstance().getCurrency() + "**")
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(6, TimeUnit.SECONDS));

                item.ghostSellItem(quantity);
                DiscordInventories.getInstance().removeItem(event.getUser().getId(), item, quantity);

                break;

            case "i":

                float price = DiscordInventories.getInstance().getNextSlotPrice(event.getUser());

                if (balance < price) {

                    event.reply(":x: You don't have enough money to buy another slot! You need **" +  price + Config.getInstance().getCurrency() + "**")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));
                    return;
                }

                if (DiscordInventories.getInstance().getCapacity(event.getUser().getId()) >= 40) {
                    event.reply(":x: You already have bought the maximum number of slots!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(4, TimeUnit.SECONDS));
                    return;
                }

                Nascraft.getEconomy().withdrawPlayer(player, price);

                DiscordInventories.getInstance().increaseCapacity(event.getUser().getId());

                event.getInteraction().editComponents().setActionRow(Button.success("info", "Slot nº" + DiscordInventories.getInstance().getCapacity(event.getUser().getId()) + " bought!").asDisabled().withEmoji(Emoji.fromFormatted("U+2705"))).queue();

        }
    }
}
