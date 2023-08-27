package dev.digitaldragon.util;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.RunJob;
import dev.digitaldragon.jobs.StringLogHandler;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;

public class DiscordClient {
    /*public static void createThread(String name, String message, StringLogHandler handler) {
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
                    handler.setThreadChannel(thread);
                });

        return;
    }*/
}
