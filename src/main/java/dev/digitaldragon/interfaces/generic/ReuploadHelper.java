package dev.digitaldragon.interfaces.generic;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.Uploader;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.ReuploadJob;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.UUID;

public class ReuploadHelper {
    public static String beginJob(String jobId, String userName, boolean oldBackend) throws UserErrorException {
        TextChannel discordChannel = WikiBot.getLogsChannelSafely();

        if (oldBackend) {
            Uploader.reupload(jobId, userName, userName, discordChannel);
        } else {
            Job job = new ReuploadJob(userName, UUID.randomUUID().toString(), jobId);
            JobManager.submit(job);
        }

        return null;
    }
}
