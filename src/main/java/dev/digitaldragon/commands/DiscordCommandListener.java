package dev.digitaldragon.commands;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.DokuWikiDumperPlugin;
import dev.digitaldragon.archive.WikiTeam3Plugin;
import dev.digitaldragon.util.BulkArchiveParser;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

public class DiscordCommandListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        TextChannel channel = WikiBot.getLogsChannel();
        if (channel == null) {
            event.reply("Something went wrong.").setEphemeral(true).queue();
            return;
        }

        if (event.getName().equals("dokuwikiarchive")) {
            if (Objects.equals(event.getSubcommandName(), "single")) {
                String url = getUrlOption(event, "url");
                if (url == null)
                    return;
                String explain = Objects.requireNonNull(event.getOption("explain")).getAsString();
                event.reply("Launching job for <" + url + ">").queue();
                DokuWikiDumperPlugin.startJob(channel, url, explain, event.getUser().getAsMention(), event.getUser().getName(), DokuWikiDumperPlugin.parseDiscordOptions(event));
            }

            if (Objects.equals(event.getSubcommandName(), "bulk")) {
                Message.Attachment bulk = Objects.requireNonNull(event.getOption("file")).getAsAttachment();
                Map<String, String> tasks;
                try {
                     tasks = BulkArchiveParser.parse(bulk.getUrl());
                } catch (Exception e) {
                    event.reply(e.getMessage()).setEphemeral(true).queue();
                    return;
                }
                for (Map.Entry<String, String> entry : tasks.entrySet()) {
                    String url = entry.getKey();
                    String note = entry.getValue();

                    DokuWikiDumperPlugin.startJob(channel, url, note, event.getUser().getAsMention(), event.getUser().getName(), DokuWikiDumperPlugin.parseDiscordOptions(event));
                }
                event.reply("Launched " + tasks.size() + " jobs!").queue();
            }
        }

        if (event.getName().equals("mediawikiarchive")) {
            if (Objects.equals(event.getSubcommandName(), "single")) {
                String url = getUrlOption(event, "url");
                String explain = Objects.requireNonNull(event.getOption("explain")).getAsString();
                event.reply("Launching job!").queue();
                WikiTeam3Plugin.startJob(channel, url, explain, event.getUser().getAsMention(), event.getUser().getName(), WikiTeam3Plugin.parseDiscordOptions(event));
            }

            if (Objects.equals(event.getSubcommandName(), "bulk")) {
                Message.Attachment bulk = Objects.requireNonNull(event.getOption("file")).getAsAttachment();
                Map<String, String> tasks;
                try {
                    tasks = BulkArchiveParser.parse(bulk.getUrl());
                } catch (Exception e) {
                    event.reply(e.getMessage()).setEphemeral(true).queue();
                    return;
                }
                for (Map.Entry<String, String> entry : tasks.entrySet()) {
                    String url = entry.getKey();
                    String note = entry.getValue();

                    DokuWikiDumperPlugin.startJob(channel, url, note, event.getUser().getAsMention(), event.getUser().getName(), WikiTeam3Plugin.parseDiscordOptions(event));
                }
                event.reply("Launched " + tasks.size() + " jobs!").queue();
            }
        }
    }

    private String getUrlOption(SlashCommandInteractionEvent event, String option) {
        String url = Objects.requireNonNull(event.getOption(option)).getAsString();
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            event.reply("Invalid URL.").setEphemeral(true).queue();
            return null;
        }
        return url;
    }
}
