package me.bounser.nascraft.discord;

import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.config.lang.Lang;
import me.bounser.nascraft.config.lang.Message;
import me.bounser.nascraft.database.DatabaseManager;
import me.bounser.nascraft.database.commands.resources.Trade;
import me.bounser.nascraft.discord.linking.LinkManager;
import me.bounser.nascraft.formatter.Formatter;
import me.bounser.nascraft.formatter.Style;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class DiscordLog {

    private final JDA jda;

    private static DiscordLog instance;

    public static DiscordLog getInstance() { return instance == null ? instance = new DiscordLog() : instance; }

    private DiscordLog() {
        jda = DiscordBot.getInstance().getJDA();
    }

    private String buffer = "";

    public void sendTradeLog(Trade trade) {

        try {
            buffer += "\n" + prepareMessage(trade);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (buffer.length() > 1000)
            flushBuffer();
    }

    private String prepareMessage(Trade trade) throws InterruptedException, ExecutionException {
        Player player = Bukkit.getPlayer(trade.getUuid());

        String action = trade.isBuy() ? Lang.get().message(Message.DISCORD_LOG_BUY) : Lang.get().message(Message.DISCORD_LOG_SELL);

        String userId = LinkManager.getInstance().getUserDiscordID(trade.getUuid());

        String nickname;
        if (player == null) nickname = DatabaseManager.get().getDatabase().getNickname(userId);
        else nickname = player.getName();

        String message = userId == null ?
                Lang.get().message(Message.DISCORD_LOG_TRADE_NOT_LINKED) :
                Lang.get().message(Message.DISCORD_LOG_TRADE_LINKED);

        message = message
                .replace("[UUID]", trade.getUuid().toString())
                .replace("[NICK]", nickname)
                .replace("[ACTION]", action)
                .replace("[QUANTITY]", String.valueOf(trade.getAmount()))
                .replace("[ALIAS]", trade.getItem().getName())
                .replace("[WORTH]", Formatter.format(trade.getValue(), Style.ROUND_BASIC));

        if (userId != null) {
            CompletableFuture<String> futureMessage = new CompletableFuture<>();
            String finalMessage = message;
            jda.retrieveUserById(userId).queue(user -> {
                if (user != null) {
                    String completedMessage = finalMessage
                            .replace("[USER]", user.getName())
                            .replace("[ID]", userId);
                    futureMessage.complete(completedMessage);
                } else {
                    futureMessage.complete(finalMessage);
                }
            });
            return futureMessage.get();
        } else {
            return message;
        }
    }

    public void flushBuffer() {

        if (buffer.isEmpty()) return;

        try {
            jda.awaitReady().getGuilds().forEach(guild -> {

                TextChannel textChannel = guild.getTextChannelById(Config.getInstance().getLogChannel());
                if (textChannel == null) {
                    Nascraft.getInstance().getLogger().info("Log channel could not be found.");
                    return;
                }

                textChannel.sendMessage(buffer).queue();

            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        buffer = "";
    }

}
