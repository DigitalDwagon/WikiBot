package dev.digitaldragon.commands;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.Uploader;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class DiscordReuploadListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("reupload"))
            return;
        event.deferReply().setEphemeral(true).queue();

        TextChannel channel = WikiBot.getLogsChannel();
        if (channel == null) {
            event.reply("Something went wrong.").setEphemeral(true).queue();
            return;
        }

        try {
            event.getHook().editOriginal("Starting...").queue();
            Uploader.reupload(event.getOption("jobid").getAsString(), event.getUser().getName(), event.getUser().getAsMention(), channel);
        } catch (IOException e) {
            event.getMessageChannel().sendMessage("Sorry, something went wrong.").queue();
        }
    }
}
