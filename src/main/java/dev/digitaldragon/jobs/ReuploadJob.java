package dev.digitaldragon.jobs;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.ThreadChannel;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Getter
public class ReuploadJob implements Job {
    private final String id;
    private final String name;
    private final String userName;
    private JobStatus status = null;
    private String runningTask = null;
    private Instant startTime = null;
    private File directory = null;
    private RunCommand uploadCommand = null;
    private final String explanation;
    @Setter
    private String archiveUrl = null;
    @Setter
    private String logsUrl = null;
    @Setter
    private ThreadChannel threadChannel = null;
    private final GenericLogsHandler handler;
    private int failedTaskCode;
    private final String uploadingFor;
    private boolean aborted = false;

    public ReuploadJob(String userName, String id, String targetId) {
        this.userName = userName;
        this.id = id;
        // non-input params
        this.directory = new File("jobs/" + id + "/");
        this.directory.mkdirs();
        this.uploadingFor = targetId;
        this.explanation = "Reupload of " + targetId;
        this.status = JobStatus.QUEUED;
        this.name =  "Reupload " + targetId;
        this.handler = new GenericLogsHandler(this);
    }

    private void failure(int code) {
        logsUrl = CommonTasks.uploadLogs(this);
        status = JobStatus.FAILED;
        failedTaskCode = code;
        handler.end();
        if (runningTask.equals("AbortTask")) {
            status = JobStatus.ABORTED;
            JobEvents.onJobAbort(this);
        } else {
            JobEvents.onJobFailure(this);
        }
    }

    public void run() {
        if (aborted)
            return;


        startTime = Instant.now();
        status = JobStatus.RUNNING;

        runningTask = "DetectJobType";
        File directory;
        try {
            directory = new File("jobs/" + uploadingFor + "/");
            directory.mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
            failure(999);
            return;
        }

        if (directory.listFiles() == null) {
            failure(999);
            return;
        }

        boolean hasRun = false;

        for (File file : directory.listFiles()) {
            if (!file.isDirectory()) {
                continue;
            }

            if (Arrays.stream(file.listFiles()).anyMatch(f -> f.getName().equals("siteinfo.json"))) {
                runningTask = "UploadMediaWiki";
                int runUpload = CommonTasks.runUpload(this, new File("jobs/" + uploadingFor + "/"), handler, uploadCommand, JobType.WIKITEAM3);
                if (runUpload != 0) {
                    failure(runUpload);
                    return;
                }
                hasRun = true;

                continue;
            }

            if (Arrays.stream(file.listFiles()).anyMatch(f -> f.getName().equals("meta"))) {
                runningTask = "UploadDokuWiki";
                int runUpload = CommonTasks.runUpload(this, new File("jobs/" + uploadingFor + "/"), handler, uploadCommand, JobType.DOKUWIKIDUMPER);
                if (runUpload != 0) {
                    failure(runUpload);
                    return;
                }
                hasRun = true;
                continue;
            }
        }

        if (!hasRun) {
            handler.onMessage("Fatal: Couldn't find the dump to upload! It probably doesn't exist, or you entered an incorrect Job ID.");
            failure(999);
            return;
        }


        logsUrl = CommonTasks.uploadLogs(this);

        status = JobStatus.COMPLETED;
        runningTask = null;
        handler.end();
        JobEvents.onJobSuccess(this);
    }

    public boolean abort() {
        if (isRunning())
            return false;
        aborted = true;
        return true;
    }

    public boolean isRunning() {
        return status == JobStatus.RUNNING;
    }

    public JobType getType() {
        return JobType.REUPLOAD;
    }

    public List<String> getAllTasks() {
        return List.of("DetectJobType", "UploadMediaWiki", "UploadDokuWiki");
    }
}
