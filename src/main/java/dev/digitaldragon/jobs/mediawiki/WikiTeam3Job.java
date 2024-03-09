package dev.digitaldragon.jobs.mediawiki;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobLogEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.ThreadChannel;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a WikiTeam3 job, which implements the Job interface.
 * This class provides functionality for running and managing WikiTeam3 jobs.
 */
@Getter
public class WikiTeam3Job extends Job {
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
        this.args = args;
    }

    private void failure(int code) {
        logsUrl = WikiBot.getLogFiles().uploadLogs(this);
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


        WikiBot.getLogFiles().setLogFile(this, new File(runDir, "log.txt"));
        startTime = Instant.now();
        status = JobStatus.RUNNING;
        log("wikibot v" + WikiBot.getVersion() + " job " + id + " starting now");

        runningTask = "DownloadMediaWiki";
        log("Starting Task DownloadMediaWiki");

        downloadCommand = new RunCommand(null, params, runDir, message -> {
            log(message);
            CommonTasks.getArchiveUrl(message).ifPresent(s -> this.archiveUrl = s);

        });

        downloadCommand.run();
        int downloadExitCode = downloadCommand.waitFor();
        if (downloadExitCode != 0) {
            failure(downloadExitCode);
            return;
        }

        log("Finished task DownloadMediaWiki");

        runningTask = "UploadMediaWiki";
        log("Starting Task UploadMediaWiki");

        File dumpDir = CommonTasks.findDumpDir(runDir);
        if (dumpDir == null) {
            log("Failed to find the dump directory, aborting...");
            failure(999);
            return;
        }
        String[] uploadParams = new String[] {"wikiteam3uploader", dumpDir.getName(), "--zstd-level", "22", "--parallel", "--bin-zstd", WikiBot.getConfig().getWikiTeam3Config().binZstd()};
        uploadCommand = new RunCommand(null, uploadParams, runDir, message -> {
            log(message);
            CommonTasks.getArchiveUrl(message).ifPresent(s -> this.archiveUrl = s);

        });

        uploadCommand.run();
        if (uploadCommand.waitFor() != 0) {
            failure(uploadCommand.waitFor());
            return;
        }

        log("Finished task UploadMediaWiki");


        if (args.isWarc()) {
            log("Starting Task Wget-AT");
            runningTask = "Wget-AT";
            File warcFile = new File(runDir, "output.warc");
            File urlsFile = new File(runDir, "pages.txt");
            MediaWikiWARCMachine warcMachine = new MediaWikiWARCMachine(args.getApi(), handler, directory, warcFile, urlsFile);
            warcMachine.run();
            log("Finished task Wget-AT");
        }

        logsUrl = WikiBot.getLogFiles().uploadLogs(this);

        runningTask = "LinkExtract";
        CommonTasks.extractLinks(this);

        status = JobStatus.COMPLETED;
        runningTask = null;
        handler.end();
        WikiBot.getBus().post(new JobSuccessEvent(this));
    }

    public boolean abort() {
        if (runningTask == null) {
            return false;
        }
        if (runningTask.equals("DownloadMediaWiki")) {
            log("----- Bot: Aborting task " + runningTask + " -----");
            downloadCommand.getProcess().descendants().forEach(ProcessHandle::destroyForcibly);
            downloadCommand.getProcess().destroyForcibly();
            status = JobStatus.ABORTED;
            log("----- Bot: Aborted task " + runningTask + " -----");
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