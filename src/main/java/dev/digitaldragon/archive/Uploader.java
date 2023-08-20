package dev.digitaldragon.archive;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.util.AfterTask;
import dev.digitaldragon.util.CommandTask;
import dev.digitaldragon.util.EnvConfig;
import dev.digitaldragon.util.IRCClient;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Uploader {
    public static void reupload(String uploadJobId, String userName, String userMention, TextChannel channel) throws IOException {
        File directory = RunJob.createWorkingDirectory(uploadJobId);
        for (File file : directory.listFiles()) {
            if (!file.isDirectory()) {
                continue;
            }

            if (Arrays.stream(file.listFiles()).anyMatch(f -> f.getName().equals("siteinfo.json"))) {
                WikiTeam3(uploadJobId, userName, userMention, channel);
                continue;
            }

            if (Arrays.stream(file.listFiles()).anyMatch(f -> f.getName().equals("meta"))) {
                DokuWikiDumper(uploadJobId, userName, userMention, channel);
                continue;
            }


        }
    }
    public static void WikiTeam3(String jobId, String userName, String userMention, TextChannel channel) {
        channel.createThreadChannel("Reupload " + jobId).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                .queue(thread -> {
                    IRCClient.sendMessage(userName, "Reuploading " + jobId + ". (WikiTeam3)");
                    WikiBot.ircClient.sendMessage(EnvConfig.getConfigs().get("ircchannel").trim(), userName + ": Launched job " + jobId + "! (WikiTeam3 Reupload)");
                    thread.sendMessage(String.format("Running reupload with WikiTeam3 (for %s). Job ID: %s", userName, jobId)).queue(message -> message.pin().queue());
                    RunJob.startArchive(jobId, "Reupload", userMention, userName, thread, jobId, AfterTask.MEDIAWIKI);
                });
    }

    public static void DokuWikiDumper(String jobId, String userName, String userMention, TextChannel channel) {
        channel.createThreadChannel("Reupload " + jobId).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                .queue(thread -> {
                    IRCClient.sendMessage(userName, "Reuploading " + jobId + ". (DokuWikiDumper)");
                    WikiBot.ircClient.sendMessage(EnvConfig.getConfigs().get("ircchannel").trim(), userName + ": Launched job " + jobId + "! (DokuWikiDumper Reupload)");
                    thread.sendMessage(String.format("Running reupload with DokuWikiUploader (for %s). Job ID: %s", userName, jobId)).queue(message -> message.pin().queue());
                    RunJob.startArchive(jobId, "Reupload", userMention, userName, thread, jobId, AfterTask.DOKUWIKI);
                });
    }
}
