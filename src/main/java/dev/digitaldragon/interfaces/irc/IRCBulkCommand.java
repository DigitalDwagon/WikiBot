package dev.digitaldragon.interfaces.irc;

import dev.digitaldragon.interfaces.generic.PukiWikiDumperHelper;
import dev.digitaldragon.jobs.JobLaunchException;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperJob;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Job;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.UUID;

public class IRCBulkCommand {
    @Handler
    public void onBulkCommand(ChannelMessageEvent event) {
        String message = event.getMessage();
        String nick = event.getActor().getNick();
        if (!message.startsWith("!bulk")) return;
        if (!IrcCommandListener.isVoiced(event.getChannel(), event.getActor())) {
            event.getChannel().sendMessage(String.format("%s: You don't have permission to do that! Please ask someone else to run this for you.", nick));
            return;
        }


        String url = message.substring(6).trim();
        if (url.isEmpty()) {
            event.getChannel().sendMessage("Usage: !bulk <url>");
            return;
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (Exception e) {
            event.getChannel().sendMessage(String.format("%s: Invalid URL", nick));
            return;
        }

        try {
            //http request
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        String contentType = response.headers()
                                .firstValue("Content-Type")
                                .orElse("");
                        if (contentType.contains("text/plain")) {
                            return response.body();
                        } else {
                            event.getChannel().sendMessage(nick + ": Server returned invalid Content-Type " + contentType + ", expected text/plain");
                            return "";
                        }
                    })
                    .thenAccept(body -> {
                        if (body.equals("")) return;
                        //split lines
                        String[] lines = body.split("\n");
                        int i = 0;
                        int jobs = 0;
                        for (String line : lines) {
                            i++;
                            String command = line.substring(0, 3);
                            line = line.substring(4).trim();
                            if (!line.contains("--silent-mode")) line += " --silent-mode " + JobMeta.SilentMode.FAIL.name(); // references the enum directly to cause a compile error in case END is removed from the enum
                            String result = null;
                            JobMeta meta = new JobMeta(nick);
                            meta.setPlatform(JobMeta.JobPlatform.IRC);
                            switch (command) {
                                case "!mw" ->  {
                                    try {
                                        JobManager.submit(new WikiTeam3Job(line, meta, UUID.randomUUID().toString()));
                                    } catch (JobLaunchException e) {
                                        result = e.getMessage();
                                    } catch (ParseException e) {
                                        result = "Invalid parameters or options! Hint: make sure that your --explain is in quotes if it has more than one word. (-e \"no coverage\")";
                                    }
                                    jobs++;
                                }
                                case "!dw" -> {
                                    try {
                                        JobManager.submit(new DokuWikiDumperJob(line, meta, UUID.randomUUID().toString()));
                                    } catch (JobLaunchException e) {
                                        result = e.getMessage();
                                    } catch (ParseException e) {
                                        result = "Invalid parameters or options! Hint: make sure that your --explain is in quotes if it has more than one word. (-e \"no coverage\")";
                                    }
                                    jobs++;
                                }
                                case "!pw" -> {
                                    PukiWikiDumperHelper.beginJob(line, nick);
                                    jobs++;
                                }
                                default -> {
                                    if (line.startsWith("!")) event.getChannel().sendMessage(String.format("%s: Invalid command on line %s", nick, i));
                                    continue;
                                }
                            }
                            if (result != null) {
                                event.getChannel().sendMessage(String.format("%s: %s (line %s)", nick, result, i));
                            }

                        }
                        event.getChannel().sendMessage(String.format("%s: Launched %s jobs (for %s)", nick, jobs, uri));
                    });
        } catch (Exception e) {
            event.getChannel().sendMessage(String.format("%s: An error occurred while processing the URL", nick));
        }

    }
}
