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
        if (event.getOption("ignore_disposition") != null)
            if (event.getOption("ignore_disposition").getAsBoolean())
                options.append("--ignore-disposition-header-missing ");

        if (event.getOption("delay") != null)
            if (event.getOption("delay").getAsInt() <= 10)
                options.append("--delay ").append(event.getOption("delay").getAsInt()).append(" ");

        if (event.getOption("retry") != null)
            if (event.getOption("retry").getAsInt() <= 50 && event.getOption("retry").getAsInt() >= 1)
                options.append("--retry ").append(event.getOption("retry").getAsInt()).append(" ");

        if (event.getOption("hard_retry") != null)
            if (event.getOption("hard_retry").getAsInt() <= 50 && event.getOption("hard_retry").getAsInt() >= 1)
                options.append("--hard-retry ").append(event.getOption("hard_retry").getAsInt()).append(" ");

        if (event.getOption("current_only") != null)
            if (event.getOption("current_only").getAsBoolean())
                options.append("--current-only ");

        if (event.getOption("threads") != null)
            if (event.getOption("threads").getAsInt() <= 50 && event.getOption("threads").getAsInt() >= 1)
                options.append("--threads ").append(event.getOption("threads").getAsInt()).append(" ");

        if (event.getOption("auto") != null) {
            if (event.getOption("auto").getAsBoolean())
                options.append("--auto ");
        } else {
            options.append("--auto ");
        }

        if (event.getOption("no_resume") != null)
            if (event.getOption("no_resume").getAsBoolean())
                options.append("--no-resume ");

        if (event.getOption("insecure") != null)
            if (event.getOption("insecure").getAsBoolean())
                options.append("--insecure ");

        if (event.getOption("ignore_errors") != null)
            if (event.getOption("ignore_errors").getAsBoolean())
                options.append("--ignore-errors ");

        if (event.getOption("ignore_disabled_edit") != null)
            if (event.getOption("ignore_disabled_edit").getAsBoolean())
                options.append("--ignore-action-disabled-edit ");

        if (event.getOption("upload") != null) {
            if (event.getOption("upload").getAsBoolean())
                options.append("--upload ");
        } else {
            options.append("--upload ");
        }

        if (event.getOption("content") != null)
            if (event.getOption("content").getAsBoolean())
                options.append("--content ");

        if (event.getOption("media") != null)
            if (event.getOption("media").getAsBoolean())
                options.append("--media ");

        if (event.getOption("html") != null)
            if (event.getOption("html").getAsBoolean())
                options.append("--html ");

        if (event.getOption("pdf") != null)
            if (event.getOption("pdf").getAsBoolean())
                options.append("--pdf ");

        return options.toString();
    }
}
