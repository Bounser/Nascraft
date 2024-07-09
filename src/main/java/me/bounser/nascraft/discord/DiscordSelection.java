package me.bounser.nascraft.discord;

import me.bounser.nascraft.config.Config;
import me.bounser.nascraft.discord.alerts.DiscordAlerts;
import me.bounser.nascraft.market.MarketManager;
import me.bounser.nascraft.market.unit.Item;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

public class DiscordSelection extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {

        if (event.getChannel().getId().equals(Config.getInstance().getChannel())) {

            if (event.getValues().get(0).contains("alert-")) {

                if (DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()) == null || !DiscordAlerts.getInstance().getAlerts().get(event.getUser().getId()).containsKey(MarketManager.getInstance().getItem(event.getValues().get(0).substring(6)))) {
                    event.reply(":exclamation: There is no alert currently setup for that item!")
                            .setEphemeral(true)
                            .queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
                }

                DiscordAlerts.getInstance().removeAlert(event.getUser().getId(), MarketManager.getInstance().getItem(event.getValues().get(0).substring(6)));

                event.reply(":no_bell: Alert of item ``" + MarketManager.getInstance().getItem(event.getValues().get(0).substring(6)).getName() + "`` removed!")
                        .setEphemeral(true)
                        .queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));

                return;
            }

            Item item = MarketManager.getInstance().getItem(event.getValues().get(0));

            DiscordBot.getInstance().sendBasicScreen(item, event.getUser(), null, event, null);
        }
    }

}
