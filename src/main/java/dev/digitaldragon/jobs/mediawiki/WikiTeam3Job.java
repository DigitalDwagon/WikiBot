package dev.digitaldragon.jobs.mediawiki;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.generic.Command;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import dev.digitaldragon.util.Config;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.io.File;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Represents a WikiTeam3 job, which implements the Job interface.
 * This class provides functionality for running and managing WikiTeam3 jobs.
 */
@Getter
public class WikiTeam3Job extends Job {
    private String id;

    private JobStatus status = JobStatus.QUEUED;
    private String runningTask = null;
    @Nullable private Instant startTime = null;
    private File directory;

    private transient RunCommand downloadCommand = null;
    private transient RunCommand uploadCommand = null;

    @Setter private String archiveUrl = null;
    @Setter private String logsUrl = null;
    private int failedTaskCode;
    private boolean aborted;
    private WikiTeam3Args args;
    private JobMeta meta;

    public WikiTeam3Job(WikiTeam3Args args, JobMeta meta, String id) throws JobLaunchException {
        String targetUrl = Optional.ofNullable(args.getUrl())
                .or(() -> Optional.ofNullable(args.getApi()))
                .or(() -> Optional.ofNullable(args.getIndex()))
                .orElseThrow(() -> new JobLaunchException("You need to specify the URL, api.php URL, or index.php URL."));

        meta.setTargetUrl(targetUrl);

        // TODO: There should be a better system for setting these values
        /*if (args.getExplain() != null && meta.getExplain().isEmpty()) {
            meta.setExplain(args.getExplain());
        }
        if (args.getSilentMode() != null && meta.getSilentMode() == null) {
            meta.setSilentMode(JobMeta.SilentMode.valueOf(args.getSilentMode()));
        }
        if (args.getQueue() != null && meta.getQueue().isEmpty()) {
            meta.setQueue(args.getQueue());
        }*/

        this.args = args;
        this.meta = meta;
        this.id = id;

        this.directory = new File("jobs/" + id + "/");
        this.directory.mkdirs();
    }

    public WikiTeam3Job(String unparsedArgs, JobMeta meta, String id) throws JobLaunchException, ParseException {
        this(
                new WikiTeam3Args(Command.shellSplit(unparsedArgs).toArray(new String[0]), meta),
                meta,
                id
        );
    }

    private void failure(int code) {
        logsUrl = WikiBot.getLogFiles().uploadLogs(this);
        status = JobStatus.FAILED;
        failedTaskCode = code;
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
            Config.UploadConfig uploadConfig = WikiBot.getConfig().getUploadConfig();

            List<String> uploadParams = new ArrayList<>();
            uploadParams.add("wikiteam3uploader");
            uploadParams.add(dumpDir.getName());
            uploadParams.add("--zstd-level");
            uploadParams.add("22");
            uploadParams.add("--parallel");
            uploadParams.add("--bin-zstd");
            uploadParams.add(WikiBot.getConfig().getWikiTeam3Config().binZstd());
            uploadParams.add("--collection");
            uploadParams.add(uploadConfig.collection());
            if (uploadConfig.offloadEnabled()) {
                uploadParams.add("--offload");
                uploadParams.add(uploadConfig.offloadServer());
            }

            uploadCommand = new RunCommand(null, uploadParams.toArray(new String[0]), runDir, message -> {
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

        logsUrl = WikiBot.getLogFiles().uploadLogs(this);

        runningTask = "LinkExtract";
        CommonTasks.extractLinks(this);

        status = JobStatus.COMPLETED;
        runningTask = null;
        WikiBot.getBus().post(new JobSuccessEvent(this));
    }

    public boolean abort() {
        if (status == JobStatus.QUEUED) {
            aborted = true;
            status = JobStatus.ABORTED;
            return true;
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