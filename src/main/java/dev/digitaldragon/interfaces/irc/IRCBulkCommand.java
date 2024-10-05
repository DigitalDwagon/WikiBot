package dev.digitaldragon.interfaces.irc;

import dev.digitaldragon.interfaces.generic.DokuWikiDumperHelper;
import dev.digitaldragon.interfaces.generic.PukiWikiDumperHelper;
import dev.digitaldragon.interfaces.generic.WikiTeam3Helper;
import dev.digitaldragon.jobs.JobMeta;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
                    .thenApply(HttpResponse::body)
                    .thenAccept(body -> {
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
                            switch (command) {
                                case "!mw" ->  {
                                    WikiTeam3Helper.beginJob(line, nick);
                                    jobs++;
                                }
                                case "!dw" -> {
                                    DokuWikiDumperHelper.beginJob(line, nick);
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
