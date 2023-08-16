package me.bounser.nascraft.discord;

import jdk.vm.ci.code.site.Mark;
import me.bounser.nascraft.Nascraft;
import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.market.managers.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;


public class DiscordListener extends ListenerAdapter {

    private DiscordBot discordBot;

    private String messageID;

    private Guild guild;

    private TextChannel channel;

    public static DiscordListener instance;

    public DiscordListener(DiscordBot discordBot) {

        this.discordBot = discordBot;
        instance = this;
    }

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
                                event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time) // then edit original
                        ).queue();
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

            event.replyFiles(FileUpload.fromData(outputfile , "image.png")).setEphemeral(true).addActionRow(

                    Button.success("Buy1", "Buy x1"),
                    Button.success("Buy32", "Buy x32"),
                    Button.danger("Sell32", "Sell x32"),
                    Button.danger("Sell1", "Sell x1")

            ).queue();
        }
    }

}
