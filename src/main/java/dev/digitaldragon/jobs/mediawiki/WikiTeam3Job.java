package dev.digitaldragon.jobs.mediawiki;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private boolean aborted;
    private WikiTeam3Args args;
    private JobMeta meta;


    public WikiTeam3Job(String userName, String id, WikiTeam3Args args) {
        String targetUrl = args.getUrl();
        if (targetUrl == null)
            targetUrl = args.getApi();
        if (targetUrl == null)
            targetUrl = args.getIndex();

        this.userName = userName;
        this.id = id;
        this.name = targetUrl;
        this.status = JobStatus.QUEUED;
        this.directory = new File("jobs/" + id + "/");
        this.directory.mkdirs();
        this.explanation = args.getExplain();
        this.handler = new GenericLogsHandler(this);
        this.args = args;
        this.meta = new JobMeta(userName);
        meta.setExplain(args.getExplain());
        meta.setTargetUrl(targetUrl);
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

        List<String> parsedArgs = new ArrayList<>(Arrays.stream(args.get()).toList());

        File runDir = directory;
        if (args.getResume() != null) {
            runDir = new File("jobs/" + args.getResume() + "/");
            File dumpDir = CommonTasks.findDumpDir(runDir);
            if (!runDir.exists() || dumpDir == null) {
                log("Failed to find the resume directory, aborting...");
                failure(999);
                return;
            }
            parsedArgs.add("--resume");
            parsedArgs.add("--path");
            parsedArgs.add(dumpDir.getName());

        }

        if (!args.isWarcOnly()) {
            WikiBot.getLogFiles().setLogFile(this, new File(directory, "log.txt"));
            startTime = Instant.now();
            status = JobStatus.RUNNING;
            log("wikibot v" + WikiBot.getVersion() + " job " + id);

            runningTask = "DownloadMediaWiki";
            log("Starting Task DownloadMediaWiki");

            downloadCommand = new RunCommand(null, parsedArgs.toArray(new String[0]), runDir, message -> {
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
        }

        if (args.isWarc()) {
            log("Starting Task Wget-AT");
            runningTask = "Wget-AT";
            File warcFile = new File(runDir, "output.warc");
            File urlsFile = new File(runDir, "pages.txt");
            MediaWikiWARCMachine warcMachine = new MediaWikiWARCMachine(this, args.getApi(), handler, directory, warcFile, urlsFile);
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