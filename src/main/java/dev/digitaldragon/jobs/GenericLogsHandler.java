package dev.digitaldragon.jobs;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.api.LogWebsocket;
import dev.digitaldragon.jobs.events.JobLogEvent;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A StringLogHandler that writes logs to log.txt and a Discord channel.
 */
public class GenericLogsHandler implements StringLogHandler {
    private final Job job;
    private ThreadChannel threadChannel;
    private String logs = "";
    private final int maxMessageLength = 2000;
    public GenericLogsHandler(Job job) {
        this.job = job;
        String message = "Running job of type " + job.getType().name() +
                " (for " + job.getUserName() +
                "). `" + job.getId() + "` ```" +
                job.getExplanation() + "```";
        String name = job.getName();

        TextChannel channel = WikiBot.getLogsChannel();
        if (channel == null) {
            return;
        }

        String threadName;
        int maxLength = 100;
        if (name.length() <= maxLength) {
            threadName = name;
        } else {
            threadName = name.substring(0, maxLength - 3) + "...";
        }


        channel.createThreadChannel(threadName).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                .queue(thread -> {
                    thread.sendMessage(message).queue(sentMessage -> sentMessage.pin().queue());
                    this.threadChannel = thread;
                    job.setThreadChannel(thread);
                });

        onMessage("----- Bot: Logs manager init -----");
    }

    public synchronized void onMessage(String message) {
        WikiBot.getBus().post(new JobLogEvent(job, message));
        try {
            LogWebsocket.sendLogMessageToClients(job.getId(), message);

            System.out.println(message);
            writeLineToFile(new File(job.getDirectory(), "log.txt"), message);


            if (logs.length() + message.length() > maxMessageLength - 6) {
                try {
                    threadChannel.sendMessage("```" + logs + "```").queue();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                logs = "";
            }

            logs += message + "\n";


            if (message.contains("https://archive.org/details/") && message.contains(" ")) {
                String[] split = message.split(" ");
                for (String s : split) {
                    if (s.contains("https://archive.org/details/")) {
                        job.setArchiveUrl(s);
                        break;
                    }
                }
            } else if (message.contains("https://archive.org/details/")) {
                job.setArchiveUrl(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeLineToFile(File file, String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.append(line);
            writer.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void end() {
        try {
            threadChannel.sendMessage("```" + logs + "```").queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        logs = "";
    }
}
