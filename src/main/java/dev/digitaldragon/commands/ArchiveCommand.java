package dev.digitaldragon.commands;

import dev.digitaldragon.DokuWikiDumperBot;
import dev.digitaldragon.Main;
import dev.digitaldragon.archive.DokuWikiArchive;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ArchiveCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("dokuwikiarchive")) {
            return;
        }

        Guild testServer = Main.getInstance().getGuildById("349920496550281226");
        if (testServer == null) {
            event.reply("Something went wrong.").queue();
            return;
        }
        TextChannel channel = (TextChannel) testServer.getGuildChannelById("1112606638017368124");

        if (channel == null) {
            event.reply("Something went wrong.").queue();
            return;
        }

        String url = event.getOption("url").getAsString(); // Assuming the option name is "url"
        String note = event.getOption("note").getAsString(); // Assuming the option name is "note"

        String threadName;
        int maxLength = 100;
        if (url.length() <= maxLength) {
            threadName = url;
        } else {
            threadName = url.substring(0, maxLength - 3) + "...";
        }

        channel.createThreadChannel(threadName)
                .queue(thread -> {
                    DokuWikiArchive.ArchiveWiki(url, note, event.getUser(), thread);
                    event.reply(thread.getAsMention()).setEphemeral(true).queue();
                    thread.sendMessage(String.format("Running archivation job on <%s> (for %s)", url, event.getUser().getAsMention())).queue();
                });
    }
}
