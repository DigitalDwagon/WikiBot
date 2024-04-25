package dev.digitaldragon.jobs;

import dev.digitaldragon.jobs.events.JobSuccessEvent;
import net.badbird5907.lightning.annotation.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CleanupListener {
    private static Logger logger = LoggerFactory.getLogger(CleanupListener.class);
    @EventHandler
    public void onJobSuccess(JobSuccessEvent event) {
        Job job = event.getJob();

        if (job.getDirectory() != null) {
            //move from /jobs to /jobs-done
            File doneDir = new File(job.getDirectory().getParentFile().getParentFile(), "jobs-done");
            if (!doneDir.exists()) {
                doneDir.mkdirs();
            }

            try {
                Files.move(job.getDirectory().toPath(), new File(doneDir, job.getId()).toPath());
            } catch (IOException e) {
                logger.error("Error moving job directory to jobs-done", e);
            }
        }
    }
}
