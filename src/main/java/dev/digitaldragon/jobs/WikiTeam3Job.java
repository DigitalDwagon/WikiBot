package dev.digitaldragon.jobs;

import dev.digitaldragon.backfeed.LinkExtract;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.dv8tion.jda.api.entities.ThreadChannel;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Getter
public class WikiTeam3Job implements Job {
    private String id = null;
    private String name = "undefined";
    private String userName = "undefined";
    private JobStatus status = null;
    private String runningTask = null;
    private Instant startTime = null;
    private String params = null;
    private File directory = null;
    private RunCommand downloadCommand = null;
    private RunCommand uploadCommand = null;
    private String explanation = null;
    @Setter
    private String archiveUrl = null;
    @Setter
    private String logsUrl = null;
    @Setter
    private ThreadChannel threadChannel = null;
    private GenericLogsHandler handler;

    public WikiTeam3Job(String userName, String id, String name, String params, String explanation) {
        System.out.println(name);
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.userName = userName;
        this.id = id;
        this.name = name;
        this.params = params;
        this.status = JobStatus.QUEUED;
        this.directory = new File("jobs/" + id + "/");
        this.directory.mkdirs();
        this.explanation = explanation;
        this.handler = new GenericLogsHandler(this);
        this.downloadCommand = new RunCommand("wikiteam3dumpgenerator " + params, directory, handler);
    }

    private void failure() {
        logsUrl = CommonTasks.uploadLogs(this);
        status = JobStatus.FAILED;
        if (runningTask.equals("AbortTask")) {
            status = JobStatus.ABORTED;
            JobEvents.onJobAbort(this);
        }
        JobEvents.onJobFailure(this);
    }

    public void run() {
        startTime = Instant.now();
        status = JobStatus.RUNNING;

        if (!runDownload()) {
            failure();
            return;
        }
        if (!runUpload()) {
            failure();
            return;
        }

        logsUrl = CommonTasks.uploadLogs(this);

        runningTask = "LinkExtract";
        CommonTasks.extractLinks(this);

        status = JobStatus.COMPLETED;
        runningTask = null;
        handler.end();
        JobEvents.onJobSuccess(this);
    }

    private boolean runDownload() {
        runningTask = "DownloadMediaWiki";
        handler.onMessage("----- Bot: Task " + runningTask + " started -----");
        return CommonTasks.runAndVerify(downloadCommand, handler, runningTask);
    }

    private boolean runUpload() {
        runningTask = "UploadMediaWiki";
        handler.onMessage("----- Bot: Task " + runningTask + " started -----");
        if (directory.listFiles() == null) {
            return false;
        }

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                uploadCommand = new RunCommand("wikiteam3uploader " + file.getName() + " --zstd-level 22 --parallel", directory, handler::onMessage);
                break;
            }
        }
        if (uploadCommand == null) {
            return false;
        }
        return CommonTasks.runAndVerify(uploadCommand, handler, runningTask);
    }



    public boolean abort() {
        if (runningTask.equals("DownloadMediaWiki")) {
            handler.onMessage("----- Bot: Aborting task " + runningTask + " -----");
            downloadCommand.getProcess().descendants().forEach(ProcessHandle::destroyForcibly);
            downloadCommand.getProcess().destroyForcibly();
            status = JobStatus.ABORTED;
            handler.onMessage("----- Bot: Aborted task " + runningTask + " -----");
            runningTask = "AbortTask";
            return true;
        }
        return false;
    }

    @Override
    public boolean isRunning() {
        return status == JobStatus.RUNNING;
    }


    public JobType getType() {
        return JobType.WIKITEAM3;
    }

    public List<String> getAllTasks() {
        return List.of("DownloadMediaWiki", "UploadMediaWiki", "LinkExtract");
    }
}
