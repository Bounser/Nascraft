package me.bounser.nascraft.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.discord.images.BalanceImage;
import me.bounser.nascraft.discord.images.ImagesManager;
import me.bounser.nascraft.discord.images.InventoryImage;
import me.bounser.nascraft.discord.inventories.DiscordInventories;
import me.bounser.nascraft.discord.inventories.DiscordInventory;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.RoundUtils;
import me.bounser.nascraft.formatter.Style;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ContextException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DiscordCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        switch (event.getName()) {

            case "alert":

                switch (DiscordAlerts.getInstance().setAlert(event.getUser().getId(), event.getOption("material").getAsString(), (float) event.getOption("price").getAsDouble())) {

                    case "success":
                        event.reply(Lang.get().message(Message.DISCORD_ALERT_SUCCESS))
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                        break;

                    case "repeated":
                        event.reply(Lang.get().message(Message.DISCORD_ALERT_ALREADY_LISTED))
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                        break;

                    case "not_valid":
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

            case "alerts":

                if (DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()) == null ||
                        DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).size() < 1) {
                    event.reply(Lang.get().message(Message.DISCORD_NO_ALERTS_SETUP))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                String alerts = Lang.get().message(Message.DISCORD_ALERT_HEADER);

                for (Item item : DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).keySet())
                    alerts = alerts + "\n" + Lang.get().message(Message.DISCORD_ALERT_SEGMENT)
                            .replace("[MATERIAL]", item.getName())
                            .replace("[PRICE1]", Formatter.format(Math.abs(DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).get(item)), Style.ROUND_BASIC))
                            .replace("[PRICE2]", String.valueOf(item.getPrice().getValue()));

                event.reply(alerts)
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));

                break;

            case "remove-alert":

                if (DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()) == null ||
                        DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).size() < 1) {
                    event.reply(Lang.get().message(Message.DISCORD_NO_ALERTS_SETUP))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                Item item = MarketManager.getInstance().getItem((event.getOption("material").getAsString()));

                if (item == null) {
                    event.reply(Lang.get().message(Message.DISCORD_ALERT_INVALID_MATERIAL))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                switch (DiscordAlerts.getInstance().removeAlert(event.getUser().getId(), item)) {

                    case "not_found":
                        event.reply(Lang.get().message(Message.DISCORD_ALERT_NOT_IN_WATCHLIST))
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                        break;

                    case "success":
                        event.reply(Lang.get().message(Message.DISCORD_ALERT_REMOVED).replace("[MATERIAL]", MarketManager.getInstance().getItem(event.getOption("material").getAsString()).getName()))
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
                            .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(BalanceImage.getImage(event.getUser())), "image.png"))
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

                if (discordInventory.getCapacity() < 40) {

                    event.replyFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(InventoryImage.getImage(discordInventory)), "image.png"))
                            .setEphemeral(true)
                            .addActionRow(Button.success("i_buy", Lang.get().message(Message.DISCORD_BUY_SLOT) + discordInventory.getNextSlotPrice() + Lang.get().message(Message.CURRENCY)))
                            .queue(message -> message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS));

                } else {

                    event.replyFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(InventoryImage.getImage(discordInventory)) , "image.png"))
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS));
                }
                break;

            case "search":

                Item itemSearched = null;

                for (Item itemCandidate : MarketManager.getInstance().getAllItems()) {
                    if (itemCandidate.getIdentifier().equalsIgnoreCase(event.getOption("material").getAsString()) ||
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

            case "stop":
            case "resume":
                if (!event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(Config.getInstance().getAdminRoleID()))) {
                    event.reply(":exclamation: You are not allowed to use this command!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                if (event.getName().equalsIgnoreCase("stop"))
                    if (MarketManager.getInstance().getActive()) {
                        MarketManager.getInstance().stop();
                        event.reply(":lock: Market has been stopped successfully!")
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    } else {
                        event.reply(":lock: Market is already closed!")
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    }

                if (event.getName().equalsIgnoreCase("resume"))
                    if (!MarketManager.getInstance().getActive()) {
                        MarketManager.getInstance().resume();
                        event.reply(":unlock: Market has been resumed successfully!")
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    } else {
                        event.reply(":unlock: Market is already resumed!")
                                .setEphemeral(true)
                                .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    }
                break;

            case "seeinv":
            case "seebal":

                if (!event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(Config.getInstance().getAdminRoleID()))) {
                    event.reply(":exclamation: You are not allowed to use this command!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                if (event.getOption("userid") == null ) {
                    event.reply(":exclamation: Invalid argument! You have to provide the ID of the discord user!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                try {
                    DiscordBot.getInstance().getJDA().retrieveUserById(event.getOption("userid").getAsString()).queue(

                            user -> {

                                if (user == null) {
                                    event.reply(":exclamation: There is no user with ID ``" + event.getOption("userid").getAsString() + "`` !")
                                            .setEphemeral(true)
                                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                                    return;
                                }

                                if (LinkManager.getInstance().getUUID(event.getOption("userid").getAsString()) == null) {
                                    event.reply(":exclamation: User ``" + user.getName() + "`` is not linked!")
                                            .setEphemeral(true)
                                            .queue(message -> message.deleteOriginal().queueAfter(10, TimeUnit.SECONDS));
                                    return;
                                }

                                switch (event.getName()) {

                                    case "seeinv":
                                        DiscordInventory discordInventory1 = DiscordInventories.getInstance().getInventory(event.getOption("userid").getAsString());

                                        event.reply("## Displaying inventory of user: ``" + user.getName() + "``")
                                                .setFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(InventoryImage.getImage(discordInventory1)), "image.png"))
                                                .setEphemeral(true)
                                                .queue(message -> message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS));

                                        return;

                                    case "seebal":

                                        OfflinePlayer player2 = Bukkit.getOfflinePlayer(LinkManager.getInstance().getUUID(user.getId()));

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


                                        EmbedBuilder eb = new EmbedBuilder();

                                        eb.setImage("attachment://image.png");

                                        eb.setTitle(":coin:  **Balance**:");

                                        eb.setFooter("Purse: " + RoundUtils.roundToOne((float) (purse*100/total)) + "% Inventory: " + RoundUtils.roundToOne((float) (inventory*100/total)) + "% Broker: " + RoundUtils.roundToOne((float) (brokerValue*100/total)) + "%");

                                        eb.setDescription(text);

                                        eb.setColor(DiscordBot.mixColors(new Color(100,250,100),
                                                new Color(250,250,100),
                                                new Color(250,100,100),
                                                purse/total, inventory/total, brokerValue/total));

                                        event.reply("## Displaying balance of user: ``" + user.getName() + "``")
                                                .addEmbeds(eb.build())
                                                .addFiles(FileUpload.fromData(ImagesManager.getBytesOfImage(BalanceImage.getImage(event.getUser())), "image.png"))
                                                .setEphemeral(true)
                                                .queue(message -> message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS));

                                }
                            }
                    );

                } catch (Exception e) {

                    event.reply("Invalid user id! Make sure its in a valid format. Example of a valid ID (yours): ``" + event.getUser().getId() + "``")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(15, TimeUnit.SECONDS));
                }

        }
    }
}

