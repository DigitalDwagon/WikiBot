package dev.digitaldragon.jobs.dokuwiki;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import lombok.Getter;
import lombok.Setter;

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
public class DokuWikiDumperJob extends Job {
    private final String id;
    private final String name;
    private final String userName;
    private JobStatus status;
    private String runningTask = null;
    private Instant startTime = null;
    private final File directory;
    private RunCommand downloadCommand = null;
    private RunCommand uploadCommand = null;
    private String explanation;
    @Setter
    private String archiveUrl = null;
    @Setter
    private String logsUrl = null;
    private GenericLogsHandler handler;
    private int failedTaskCode;
    private DokuWikiDumperArgs args;
    private JobMeta meta;


    public DokuWikiDumperJob(String userName, String id, DokuWikiDumperArgs args) {
        name = args.getUrl();
        this.userName = userName;
        this.id = id;
        this.status = JobStatus.QUEUED;
        this.directory = new File("jobs/" + id + "/");
        this.directory.mkdirs();
        this.explanation = args.getExplain();
        this.handler = new GenericLogsHandler(this);
        this.args = args;
        this.meta = new JobMeta(userName);
        meta.setExplain(args.getExplain());
        meta.setTargetUrl(args.getUrl());
        if (args.getSilentMode() != null) meta.setSilentMode(JobMeta.SilentMode.valueOf(args.getSilentMode()));
        if (args.getQueue() != null) meta.setQueue(args.getQueue());
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


        WikiBot.getLogFiles().setLogFile(this, new File(directory, "log.txt"));
        startTime = Instant.now();
        status = JobStatus.RUNNING;
        log("wikibot v" + WikiBot.getVersion() + " job " + id);

        List<String> dumpArgs = args.get();
        File runDir = directory;
        if (args.getResume() != null) {
            File resumeDir = CommonTasks.findDumpDir(args.getResume());

            if (resumeDir == null) {
                log("Error (bot): Unknown job " + args.getResume());
                failure(1);
                return;
            }

            runDir = resumeDir.getParentFile();
            dumpArgs.add("--path");
            dumpArgs.add(resumeDir.getName());
        }



        runningTask = "Dump";
        log("Starting dump task");

        downloadCommand = new RunCommand(null, dumpArgs.toArray(new String[0]), runDir, message -> {
            log(message);
            CommonTasks.getArchiveUrl(message).ifPresent(s -> this.archiveUrl = s);

        });

        downloadCommand.run();
        int downloadExitCode = downloadCommand.waitFor();
        if (downloadExitCode != 0) {
            failure(downloadExitCode);
            return;
        }

        log("Finished dump task");

        runningTask = "Upload";
        log("Starting upload task");

        File dumpDir = CommonTasks.findDumpDir(runDir);
        if (dumpDir == null) {
            log("Failed to find the dump directory, aborting...");
            failure(999);
            return;
        }
        String[] uploadParams = new String[] {"dokuWikiUploader", dumpDir.getName(), "--collection", WikiBot.getConfig().getUploadConfig().collection()};
        uploadCommand = new RunCommand(null, uploadParams, runDir, message -> {
            log(message);
            CommonTasks.getArchiveUrl(message).ifPresent(s -> this.archiveUrl = s);

        });

        uploadCommand.run();
        if (uploadCommand.waitFor() != 0) {
            failure(uploadCommand.waitFor());
            return;
        }

        log("Finished task upload");

        logsUrl = CommonTasks.uploadLogs(this);

        status = JobStatus.COMPLETED;
        runningTask = null;
        handler.end();
        WikiBot.getBus().post(new JobSuccessEvent(this));
    }


    public boolean abort() {
        if (runningTask.equals("Dump")) {
            log("----- Bot: Aborting task " + runningTask + " -----");
            downloadCommand.getProcess().descendants().forEach(ProcessHandle::destroyForcibly);
            downloadCommand.getProcess().destroyForcibly();
            status = JobStatus.ABORTED;
            log("----- Bot: Aborted task " + runningTask + " -----");
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
        return List.of("Dump", "Upload");
    }
}
