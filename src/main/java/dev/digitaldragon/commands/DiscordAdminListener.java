package dev.digitaldragon.commands;

import dev.digitaldragon.util.IRCClient;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DiscordAdminListener extends ListenerAdapter {
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("poke"))
            return;

        IRCClient.reconnect();
        event.reply("Poke received!").setEphemeral(true).queue();
    }
}
