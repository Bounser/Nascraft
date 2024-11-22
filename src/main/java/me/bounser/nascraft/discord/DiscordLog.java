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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class DiscordLog {

    private final JDA jda;
    private static DiscordLog instance;

    public static DiscordLog getInstance() {
        if (instance == null) {
            synchronized (DiscordLog.class) {
                if (instance == null) {
                    instance = new DiscordLog();
                }
            }
        }
        return instance;
    }

    private final AtomicReference<StringBuffer> buffer = new AtomicReference<>(new StringBuffer());
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private DiscordLog() {
        jda = DiscordBot.getInstance().getJDA();
    }

    public void sendTradeLog(Trade trade) {
        prepareMessageAsync(trade).thenAcceptAsync(message -> {
            StringBuffer currentBuffer = buffer.get();
            synchronized (currentBuffer) {
                currentBuffer.append("\n").append(message);
                if (currentBuffer.length() > 1000) {
                    flushBuffer();
                }
            }
        }, executorService).exceptionally(throwable -> {
            Nascraft.getInstance().getLogger().warning("Error in sendTradeLog: " + throwable.getMessage());
            return null;
        });
    }

    private CompletableFuture<String> prepareMessageAsync(Trade trade) {
        return CompletableFuture.supplyAsync(() -> {
            Player player = Bukkit.getPlayer(trade.getUuid());
            String action = trade.isBuy() ? Lang.get().message(Message.DISCORD_LOG_BUY) : Lang.get().message(Message.DISCORD_LOG_SELL);

            String userId = LinkManager.getInstance().getUserDiscordID(trade.getUuid());
            String nickname = (player != null) ? player.getName() : DatabaseManager.get().getDatabase().getNickname(userId);

            String message = userId == null ?
                    Lang.get().message(Message.DISCORD_LOG_TRADE_NOT_LINKED) :
                    Lang.get().message(Message.DISCORD_LOG_TRADE_LINKED);

            return message
                    .replace("[UUID]", trade.getUuid().toString())
                    .replace("[NICK]", nickname)
                    .replace("[ACTION]", action)
                    .replace("[QUANTITY]", String.valueOf(trade.getAmount()))
                    .replace("[ALIAS]", trade.getItem().getName())
                    .replace("[WORTH]", Formatter.plainFormat(trade.getItem().getCurrency(), trade.getValue(), Style.ROUND_BASIC));
        }, executorService).thenCompose(message ->
                trade.getUuid() != null && LinkManager.getInstance().getUserDiscordID(trade.getUuid()) != null ?
                        retrieveDiscordUserMessage(LinkManager.getInstance().getUserDiscordID(trade.getUuid()), message) :
                        CompletableFuture.completedFuture(message));
    }

    private CompletableFuture<String> retrieveDiscordUserMessage(String userId, String message) {
        CompletableFuture<String> futureMessage = new CompletableFuture<>();
        jda.retrieveUserById(userId).queue(user -> {
            if (user != null) {
                futureMessage.complete(message.replace("[USER]", user.getName()).replace("[ID]", userId));
            } else {
                futureMessage.complete(message);
            }
        }, error -> {
            Nascraft.getInstance().getLogger().warning("Error retrieving Discord user: " + error.getMessage());
            futureMessage.complete(message);
        });
        return futureMessage;
    }

    public void flushBuffer() {
        StringBuffer currentBuffer = buffer.getAndSet(new StringBuffer());

        if (currentBuffer.isEmpty()) return;

        executorService.submit(() -> {
            try {
                if (!jda.getStatus().isInit()) {
                    Nascraft.getInstance().getLogger().warning("JDA not ready. Log skipped.");
                    return;
                }

                jda.getGuilds().forEach(guild -> {
                    TextChannel textChannel = guild.getTextChannelById(Config.getInstance().getLogChannel());
                    if (textChannel != null) {
                        textChannel.sendMessage(currentBuffer.toString()).queue();
                    } else {
                        Nascraft.getInstance().getLogger().warning("Log channel not found for guild: " + guild.getName());
                    }
                });
            } catch (Exception e) {
                Nascraft.getInstance().getLogger().severe("Error flushing buffer: " + e.getMessage());
            }
        });
    }
}
