package dev.digitaldragon.jobs;

import lombok.Getter;

import java.io.File;
import java.time.Instant;
import java.util.List;

@Getter
public class WikiTeam3Job implements Job {
    private String id = null;
    @Getter
    private String name = null;
    @Getter
    private String userName = null;
    @Getter
    private JobStatus status = null;
    @Getter
    private String runningTask = null;
    @Getter
    private Instant startTime = null;
    @Getter
    private String params = null;
    @Getter
    private File directory = null;
    @Getter
    private RunCommand downloadCommand = null;
    @Getter
    private RunCommand uploadCommand = null;
    public void run() {
        startTime = Instant.now();
        status = JobStatus.RUNNING;
        if (!runDownload()) {
            status = JobStatus.FAILED;
            return;
        }
        if (!runUpload()) {
            status = JobStatus.FAILED;
            return;
        }

        status = JobStatus.COMPLETED;
    }

    public boolean abort() {
        if (runningTask.equals("DownloadMediaWiki")) {
            downloadCommand.getProcess().destroy();
            return true;
        }
        return false;
    }

    @Override
    public boolean isRunning() {
        return runningTask == null;
    }


    public JobType getType() {
        return JobType.WIKITEAM3;
    }

    public List<String> getAllTasks() {
        return List.of("DownloadMediaWiki", "UploadMediaWiki", "LinkExtract");
    }

    public WikiTeam3Job(String userName, String id, String name, String params) {
        this.userName = userName;
        this.id = id;
        this.name = name;
        this.params = params;
        this.status = JobStatus.QUEUED;
        directory = new File("jobs/" + id + "/");
        directory.mkdirs();
        this.downloadCommand = new RunCommand("wikiteam3dumpgenerator " + params, directory, LogsHandler::onMessage);
    }

    private static class LogsHandler {
        public static void onMessage(String message) {
            System.out.println(message);
        }
    }

    private boolean runDownload() {
        runningTask = "DownloadMediaWiki";
        LogsHandler.onMessage("----- Bot: Task " + runningTask + " started -----");
        return runAndVerify(downloadCommand);
    }

    private boolean runUpload() {
        runningTask = "UploadMediaWiki";
        LogsHandler.onMessage("----- Bot: Task " + runningTask + " started -----");
        if (directory.listFiles() == null) {
            return false;
        }

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                uploadCommand = new RunCommand("wikiteam3upload " + file.getName() + " --zstd-level 22 --parallel", directory, LogsHandler::onMessage);
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
            LogsHandler.onMessage("----- Bot: Task " + runningTask + " finished -----");
            LogsHandler.onMessage("----- Bot: Exit code: " + exitCode + " -----");

            if (exitCode == 0) {
                return true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

}
