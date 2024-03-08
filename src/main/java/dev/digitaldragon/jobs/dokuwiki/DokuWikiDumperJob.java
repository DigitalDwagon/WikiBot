package dev.digitaldragon.jobs.dokuwiki;

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
 * The DokuWikiDumperJob class represents a job for dumping a DokuWiki instance.
 *
 * This class implements the Job interface.
 *
 * The DokuWikiDumperJob class provides methods for creating, running, and
 * aborting a DokuWiki dump job.
 */
@Getter
public class DokuWikiDumperJob implements Job {
    private String id = null;
    private String name = "undefined";
    private String userName = "undefined";
    private JobStatus status = null;
    private String runningTask = null;
    private Instant startTime = null;
    private String[] params = null;
    private File directory = null;
    private RunCommand downloadCommand = null;
    private RunCommand uploadCommand = null;
    private String explanation = null;
    @Setter
    private String archiveUrl = null;
    @Setter
    private String logsUrl = null;
    private GenericLogsHandler handler;
    private int failedTaskCode;

    public DokuWikiDumperJob(String userName, String id, String name, String[] params, String explanation) {
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
        this.downloadCommand = new RunCommand(null, params, directory, handler);
    }

    private void failure(int code) {
        logsUrl = CommonTasks.uploadLogs(this);
        status = JobStatus.FAILED;
        failedTaskCode = code;
        if (runningTask.equals("AbortTask")) {
            status = JobStatus.ABORTED;
            WikiBot.getBus().post(new JobAbortEvent(this));
        } else {
            WikiBot.getBus().post(new JobFailureEvent(this));
        }
    }

    public void run() {
        startTime = Instant.now();
        status = JobStatus.RUNNING;

        int runDownload = runDownload();
        if (runDownload != 0) {
            failure(runDownload);
            return;
        }

        logsUrl = CommonTasks.uploadLogs(this);

        status = JobStatus.COMPLETED;
        runningTask = null;
        handler.end();
        WikiBot.getBus().post(new JobSuccessEvent(this));
    }

    private int runDownload() {
        runningTask = "DokuWikiDumper";
        handler.onMessage("----- Bot: Task " + runningTask + " started -----");
        return CommonTasks.runAndVerify(downloadCommand, handler, runningTask);
    }


    public boolean abort() {
        if (runningTask.equals("DokuWikiDumper")) {
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
        return JobType.DOKUWIKIDUMPER;
    }

    public List<String> getAllTasks() {
        return List.of("DokuWikiDumper");
    }
}
