package dev.digitaldragon.jobs;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.time.Instant;
import java.util.List;

@Getter
public class WikiTeam3Job implements Job {
    private String id = null;
    private String name = null;
    private String userName = null;
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
    private final GenericLogsHandler handler = new GenericLogsHandler(this);

    public WikiTeam3Job(String userName, String id, String name, String params, String explanation) {
        this.userName = userName;
        this.id = id;
        this.name = name;
        this.params = params;
        this.status = JobStatus.QUEUED;
        this.directory = new File("jobs/" + id + "/");
        this.directory.mkdirs();
        this.downloadCommand = new RunCommand("wikiteam3dumpgenerator " + params, directory, handler::onMessage);
        this.explanation = explanation;
    }

    private void failure() {
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

        status = JobStatus.COMPLETED;
        runningTask = null;
        JobEvents.onJobSuccess(this);
    }

    private boolean runDownload() {
        runningTask = "DownloadMediaWiki";
        handler.onMessage("----- Bot: Task " + runningTask + " started -----");
        return runAndVerify(downloadCommand);
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
        return runAndVerify(uploadCommand);
    }

    private boolean runAndVerify(RunCommand uploadCommand) {
        uploadCommand.run();

        try {
            int exitCode = uploadCommand.getProcess().waitFor();
            handler.onMessage("----- Bot: Task " + runningTask + " finished -----");
            handler.onMessage("----- Bot: Exit code: " + exitCode + " -----");

            if (exitCode == 0) {
                return true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
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
