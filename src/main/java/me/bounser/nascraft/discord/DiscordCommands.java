package me.bounser.nascraft.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.images.BalanceImage;
import me.bounser.nascraft.discord.images.InventoryImage;
import me.bounser.nascraft.discord.inventories.DiscordInventories;
import me.bounser.nascraft.discord.inventories.DiscordInventory;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DiscordCommands extends ListenerAdapter {

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
                    alerts = alerts + "\n**" + item.getName() + "** at price: **" + Math.abs(DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).get(item)) + Lang.get().message(Message.CURRENCY) + "**";

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

                Item item = MarketManager.getInstance().getItem(Material.getMaterial(event.getOption("material").getAsString().replace(" ", "_").toUpperCase()));

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

                if (LinkManager.getInstance().getUUID(event.getUser().getId()) != null) {

                    event.reply(":link: Already linked! (With UUID " + LinkManager.getInstance().getUUID(event.getUser().getId()) + ")")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                } else {

                    event.reply(":link: Link your account using ``/link " + LinkManager.getInstance().startLinkingProcess(event.getUser().getId()) + "`` in-game!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                }
                break;

            case "balance":

                UUID uuid = LinkManager.getInstance().getUUID(event.getUser().getId());

                if (uuid != null) {

                    OfflinePlayer player2 = Bukkit.getOfflinePlayer(LinkManager.getInstance().getUUID(event.getUser().getId()));

                    double purse = Nascraft.getEconomy().getBalance(player2);
                    float inventory = DiscordInventories.getInstance().getInventory(event.getUser().getId()).getInventoryValue();
                    float brokerValue = 0;
                    double total = purse + inventory + brokerValue;

                    String text =
                            "\n> :green_circle: :dollar: **Purse** (Minecraft): ``" + Formatter.formatDouble(purse) +
                                    "``\n> :yellow_circle: :school_satchel: **Discord Inventory**: ``" + Formatter.format(inventory, Style.ROUND_BASIC) +
                                    "``\n> :red_circle: :man_office_worker: **Broker-Managed**: ``" + Formatter.format(brokerValue, Style.ROUND_BASIC) +
                                    "``\n> \n" +
                                    ">  :abacus: **Total**: ``" + Formatter.formatDouble(total) + "``\n";

                    File balanceFile = new File("image.png");
                    try {
                        ImageIO.write(BalanceImage.getImage(event.getUser()), "png", balanceFile);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    EmbedBuilder eb = new EmbedBuilder();

                    eb.setImage("attachment://image.png");

                    eb.setTitle(":coin:  **Balance**:");

                    eb.setFooter("Purse: " + RoundUtils.roundToOne((float) (purse*100/total)) + "% Inventory: " + RoundUtils.roundToOne((float) (inventory*100/total)) + "% Broker: " + RoundUtils.roundToOne((float) (brokerValue*100/total)) + "%");

                    eb.setDescription(text);


                    eb.setColor(DiscordBot.mixColors(new Color(100,250,100),
                            new Color(250,250,100),
                            new Color(250,100,100),
                            purse/total, inventory/total, brokerValue/total));

                    event.replyEmbeds(eb.build())
                            .addFiles(FileUpload.fromData(balanceFile , "image.png"))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS));

                } else
                    event.reply(":x: You don't have any account linked!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                break;

            case "inventory":

                if (LinkManager.getInstance().getUUID(event.getUser().getId()) == null) {
                    event.reply(":x: You don't have any account linked!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                DiscordInventory discordInventory = DiscordInventories.getInstance().getInventory(LinkManager.getInstance().getUUID(event.getUser().getId()));

                File outputfile = new File("image.png");
                try {
                    ImageIO.write(InventoryImage.getImage(discordInventory), "png", outputfile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

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
                break;

            case "search":

                Item itemSearched = null;

                for (Item itemCandidate : MarketManager.getInstance().getAllItems()) {
                    if (itemCandidate.getMaterial().toString().equalsIgnoreCase(event.getOption("material").getAsString()) ||
                            itemCandidate.getName().equalsIgnoreCase(event.getOption("material").getAsString())) {
                        itemSearched = itemCandidate; break;
                    }
                }

                if (itemSearched == null) {
                    event.reply("Item not recognized!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                } else {
                    DiscordBot.getInstance().sendBasicScreen(itemSearched, event.getUser(), null, null, event);
                }

                break;

        }
    }
}
