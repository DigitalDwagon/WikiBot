package dev.digitaldragon.jobs.mediawiki;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.ThreadChannel;

import java.io.File;
import java.time.Instant;
import java.util.List;

/**
 * Represents a WikiTeam3 job, which implements the Job interface.
 * This class provides functionality for running and managing WikiTeam3 jobs.
 */
@Getter
public class WikiTeam3Job implements Job {
    private String id = null;
    private String name = "undefined";
    private String userName = "undefined";
    private JobStatus status = null;
    private String runningTask = null;
    private Instant startTime = null;
    private String[] params = null;
    private File directory = null;
    private File runDir = null;
    private RunCommand downloadCommand = null;
    private RunCommand uploadCommand = null;
    private String explanation = null;
    @Setter
    private String archiveUrl = null;
    @Setter
    private String logsUrl = null;
    private GenericLogsHandler handler;
    private int failedTaskCode;
    private boolean aborted;
    private WikiTeam3Args args;


    public WikiTeam3Job(String userName, String id, String name, String[] params, WikiTeam3Args args, String explanation) {
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
        this.runDir =  args.getResumeDir() != null ? args.getResumeDir().getParentFile() : directory;
        //handler.onMessage(runDir.getAbsolutePath());
        this.downloadCommand = new RunCommand(null, params, runDir, handler);
        this.args = args;
    }

    private void failure(int code) {
        logsUrl = CommonTasks.uploadLogs(this);
        status = JobStatus.FAILED;
        failedTaskCode = code;
        handler.end();
        if (aborted) {
            status = JobStatus.ABORTED;
            WikiBot.getBus().post(new JobAbortEvent(this));
        } else {
            WikiBot.getBus().post(new JobFailureEvent(this));
        }
    }

    public void run() {
        if (aborted)
            return;
        startTime = Instant.now();
        status = JobStatus.RUNNING;

        int runDownload = runDownload();
        if (runDownload != 0) {
            failure(runDownload);
            return;
        }
        int runUpload = runUpload();
        if (runUpload != 0) {
            failure(runUpload);
            return;
        }
        if (args.isWarc()) {
            runningTask = "Wget-AT";
            File warcFile = new File(runDir, "output.warc");
            File urlsFile = new File(runDir, "pages.txt");
            MediaWikiWARCMachine warcMachine = new MediaWikiWARCMachine(args.getApi(), handler, directory, warcFile, urlsFile);
            warcMachine.run();
        }

        logsUrl = CommonTasks.uploadLogs(this);

        runningTask = "LinkExtract";
        CommonTasks.extractLinks(this);

        status = JobStatus.COMPLETED;
        runningTask = null;
        handler.end();
        WikiBot.getBus().post(new JobSuccessEvent(this));
    }

    private int runDownload() {
        runningTask = "DownloadMediaWiki";
        handler.onMessage("----- Bot: Task " + runningTask + " started -----");
        return CommonTasks.runAndVerify(downloadCommand, handler, runningTask);
    }

    private int runUpload() {
        runningTask = "UploadMediaWiki";
        handler.onMessage("----- Bot: Task " + runningTask + " started -----");
        if (runDir.listFiles() == null) {
            return 999;
        }

        for (File file : runDir.listFiles()) {
            if (file.isDirectory()) {
                uploadCommand = new RunCommand("wikiteam3uploader " + file.getName() + " --zstd-level 22 --parallel", null, runDir, handler::onMessage);
                break;
            }
        }
        if (uploadCommand == null) {
            return 999;
        }
        return CommonTasks.runAndVerify(uploadCommand, handler, runningTask);
    }



    public boolean abort() {
        if (runningTask == null) {
            return false;
        }
        if (runningTask.equals("DownloadMediaWiki")) {
            handler.onMessage("----- Bot: Aborting task " + runningTask + " -----");
            downloadCommand.getProcess().descendants().forEach(ProcessHandle::destroyForcibly);
            downloadCommand.getProcess().destroyForcibly();
            status = JobStatus.ABORTED;
            handler.onMessage("----- Bot: Aborted task " + runningTask + " -----");
            aborted = true;
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
        return List.of("DownloadMediaWiki", "UploadMediaWiki", "Wget-AT", "LinkExtract");
    }
}