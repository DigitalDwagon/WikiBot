package dev.digitaldragon.commands;

import dev.digitaldragon.ArchiveBot;
import dev.digitaldragon.archive.DokuWikiArchive;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.*;
import java.util.*;

public class ArchiveCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        System.out.println(event.getName());
        System.out.println(event.getSubcommandName());
        if (!event.getName().equals("dokuwikiarchive")) {
            return;
        }
        String options = parseOptions(event);
        //validate server is okay
        Guild testServer = event.getJDA().getGuildById("349920496550281226");
        if (testServer == null) {
            event.reply("Something went wrong.").queue();
            return;
        }
        TextChannel channel = (TextChannel) testServer.getGuildChannelById("1112606638017368124");
        if (channel == null) {
            event.reply("Something went wrong.").queue();
            return;
        }

        // Single command execution
        if (Objects.equals(event.getSubcommandName(), "single")) {
            String url = Objects.requireNonNull(event.getOption("url")).getAsString(); // Assuming the option name is "url"
            String note = Objects.requireNonNull(event.getOption("explain")).getAsString(); // Assuming the option name is "explain"

            //ensure URL is good
            try {
                URL uri = new URL(url);
            } catch (MalformedURLException e) {
                event.reply("Invalid URL.").setEphemeral(true).queue();
                return;
            }

            event.reply("Launching job for <" + url + ">").queue();
            startJob(channel, url, note, event.getUser(), options);
        }

        if (Objects.equals(event.getSubcommandName(), "bulk")) {
            Message.Attachment bulk = Objects.requireNonNull(event.getOption("file")).getAsAttachment();
            if (!Objects.equals(bulk.getContentType(), "text/plain; charset=utf-8")) {
                event.reply("Your uploaded file is invalid.").setEphemeral(true).queue();
                return;
            }


            try {
                URL fileUrl = new URL(bulk.getUrl());
                HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;


                    Map<String, String> tasks = new HashMap<>();
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty())
                            continue;

                        String[] parts = line.split(" ", 2);
                        String url = parts[0];
                        try {
                            URL verifyValid = new URL(url);
                        } catch (MalformedURLException e) {
                            event.reply("Invalid URL: " + url).setEphemeral(true).queue();
                            return;
                        }
                        String note = parts.length > 1 ? parts[1] : "No note provided."; // check if second part exists
                        tasks.put(url, note);
                    }

                    for (Map.Entry<String, String> entry : tasks.entrySet()) {
                        String url = entry.getKey();
                        String note = entry.getValue();

                        startJob(channel, url, note, event.getUser(), options);
                    }
                    event.reply(String.format("Spawned %s jobs for %s", tasks.size(), bulk.getFileName())).queue();

                } else {
                    event.reply("Sorry, the server returned a bad response code: " + responseCode).queue();
                }
            } catch (IOException e) {
                event.reply("Sorry, there was an issue downloading your file.").queue();
            }
        }
    }

    public void startJob(TextChannel channel, String url, String note, User user, String options) {
        String threadName;
        int maxLength = 100;
        if (url.length() <= maxLength) {
            threadName = url;
        } else {
            threadName = url.substring(0, maxLength - 3) + "...";
        }

        channel.createThreadChannel(threadName).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                .queue(thread -> {
                    String jobId = UUID.randomUUID().toString();
                    DokuWikiArchive.ArchiveWiki(url, note, user, thread, options, jobId);
                    thread.sendMessage(String.format("Running archivation job on <%s> (for %s). `%s` ```%s``` \n Job ID: %s", url, user.getAsTag(), options, note, jobId)).queue(message -> message.pin().queue());
                });
    }

    public String parseOptions(SlashCommandInteractionEvent event) {
        StringBuilder options = new StringBuilder();
        processIntRangeOption(event, "delay", 1, 10, "--delay", options);
        processIntRangeOption(event, "retry", 1, 50, "--retry", options);
        processIntRangeOption(event, "hard_retry", 1, 50, "--hard-retry", options);
        processIntRangeOption(event, "threads", 1, 50, "--threads", options);

        processBooleanOption(event, "upload", "--upload", options, true);
        processBooleanOption(event, "auto", "--auto", options, true);

        processBooleanOption(event, "ignore_disposition", "--ignore-disposition-header-missing", options);
        processBooleanOption(event, "current_only", "--current-only", options);
        processBooleanOption(event, "no_resume", "--no-resume", options);
        processBooleanOption(event, "insecure", "--insecure", options);
        processBooleanOption(event, "ignore_errors", "--ignore-errors", options);
        processBooleanOption(event, "ignore_disabled_edit", "--ignore-action-disabled-edit", options);
        processBooleanOption(event, "content", "--content", options);
        processBooleanOption(event, "media", "--media", options);
        processBooleanOption(event, "html", "--html", options);
        processBooleanOption(event, "pdf", "--pdf", options);
        return options.toString();
    }

    private void processIntRangeOption(SlashCommandInteractionEvent event, String option, int min, int max, String command, StringBuilder options) {
        if (event.getOption(option) == null)
            return;

        Integer optionValue =  event.getOption(option).getAsInt();
        if (optionValue >= min && optionValue <= max) {
            options.append(command).append(" ").append(optionValue).append(" ");
        }
    }

    private void processBooleanOption(SlashCommandInteractionEvent event, String option, String command, StringBuilder options) {
        processBooleanOption(event, option, command, options, false);
    }


    private void processBooleanOption(SlashCommandInteractionEvent event, String option, String command, StringBuilder options, boolean defaultValue) {
        boolean optionValue = event.getOption(option) != null ? event.getOption(option).getAsBoolean() : defaultValue;
        if (optionValue) {
            options.append(command).append(" ");
        }
    }
}
