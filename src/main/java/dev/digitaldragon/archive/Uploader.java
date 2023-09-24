package dev.digitaldragon.archive;

import dev.digitaldragon.util.AfterTask;
import dev.digitaldragon.util.IRCClient;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Uploader {
    /**
     * Reuploads a dump based on its job ID.
     * Deprecated: use {@link dev.digitaldragon.jobs.ReuploadJob} instead.
     *
     * @param uploadJobId The ID of the upload job.
     * @param userName The name of the user.
     * @param userMention The mention of the user.
     * @param channel The text channel to send the reuploaded file to.
     */
    @Deprecated
    public static void reupload(String uploadJobId, String userName, String userMention, TextChannel channel) {
        try {
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
        } catch (IOException exception) {
            System.out.println("Error: " + exception.getMessage());
        }
    }

    public static void WikiTeam3(String jobId, String userName, String userMention, TextChannel channel) {
        channel.createThreadChannel("Reupload " + jobId).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                .queue(thread -> {
                    IRCClient.sendMessage(userName, "Launched job " + jobId + "! (WikiTeam3 Reupload)");
                    thread.sendMessage(String.format("Running reupload with WikiTeam3 (for %s). Job ID: %s", userName, jobId)).queue(message -> message.pin().queue());
                    RunJob.startArchive(jobId, "Reupload", userMention, userName, thread, jobId, AfterTask.MEDIAWIKI);
                });
    }

    public static void DokuWikiDumper(String jobId, String userName, String userMention, TextChannel channel) {
        channel.createThreadChannel("Reupload " + jobId).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                .queue(thread -> {
                    IRCClient.sendMessage(userName, "Launched job " + jobId + "! (DokuWikiDumper Reupload)");
                    thread.sendMessage(String.format("Running reupload with DokuWikiUploader (for %s). Job ID: %s", userName, jobId)).queue(message -> message.pin().queue());
                    RunJob.startArchive(jobId, "Reupload", userMention, userName, thread, jobId, AfterTask.DOKUWIKI);
                });
    }
}
