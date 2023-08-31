package me.bounser.nascraft.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.market.RoundUtils;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

                File outputfile = new File("image.png");
                try {
                    ImageIO.write(ImageBuilder.getInstance().getInventory(event.getUser()), "png", outputfile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (DiscordInventories.getInstance().getCapacity(event.getUser().getId()) < 40) {

                    event.replyFiles(FileUpload.fromData(outputfile , "image.png"))
                            .setEphemeral(true)
                            .addActionRow(Button.success("i_buy", "Buy slot for " + DiscordInventories.getInstance().getNextSlotPrice(event.getUser()) + Config.getInstance().getCurrency()))
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

        }
    }

}
