package dev.digitaldragon.jobs.mediawiki;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.generic.Command;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.util.Config;
import lombok.Getter;

import java.io.File;
import java.text.ParseException;
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
    private String runningTask = null;
    private File directory;

    private transient RunCommand downloadCommand = null;
    private transient RunCommand uploadCommand = null;
    private transient RunCommand itemCommand = null;

    private WikiTeam3Args args;
    private JobMeta meta;

    public WikiTeam3Job(WikiTeam3Args args, JobMeta meta) throws JobLaunchException {
        String targetUrl = Optional.ofNullable(args.getUrl())
                .or(() -> Optional.ofNullable(args.getApi()))
                .or(() -> Optional.ofNullable(args.getIndex()))
                .orElseThrow(() -> new JobLaunchException("You need to specify the URL, api.php URL, or index.php URL."));

        meta.setTargetUrl(targetUrl);

        this.args = args;
        this.meta = meta;

        this.directory = new File("jobs/" + id + "/");
        this.directory.mkdirs();
    }

    public WikiTeam3Job(String unparsedArgs, JobMeta meta) throws JobLaunchException, ParseException {
        this(
                new WikiTeam3Args(Command.shellSplit(unparsedArgs).toArray(new String[0]), meta),
                meta
        );
    }

    protected JobResult execute() {
        List<String> parsedArgs = new ArrayList<>(Arrays.stream(args.get()).toList());

        File runDir = directory;
        if (args.getResume() != null) {
            runDir = new File("jobs/" + args.getResume() + "/");
            File dumpDir = CommonTasks.findDumpDir(runDir);
            if (!runDir.exists() || dumpDir == null) {
                log("Failed to find the resume directory, aborting...");
                return new JobResult(false, 999);
            }
            parsedArgs.add("--resume");
            parsedArgs.add("--path");
            parsedArgs.add(dumpDir.getName());

        }

        WikiBot.getLogFiles().setLogFile(this, new File(directory, "log.txt"));

        runningTask = "DownloadMediaWiki";
        log("Starting Task DownloadMediaWiki");

        downloadCommand = new RunCommand(parsedArgs.toArray(new String[0]), runDir, message -> {
            log(message);
            CommonTasks.getArchiveUrl(message).ifPresent(this::setArchiveUrl);

        });

        downloadCommand.run();
        int downloadExitCode = downloadCommand.waitFor();
        if (downloadExitCode != 0) {
            return new JobResult(false, downloadExitCode);
        }

        log("Finished task DownloadMediaWiki");

        runningTask = "UploadMediaWiki";
        log("Starting Task UploadMediaWiki");

        File dumpDir = CommonTasks.findDumpDir(runDir);
        if (dumpDir == null) {
            log("Failed to find the dump directory, aborting...");
            return new JobResult(false, 999);
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

        uploadCommand = new RunCommand(uploadParams.toArray(new String[0]), runDir, message -> {
            log(message);
            CommonTasks.getArchiveUrl(message).ifPresent(this::setArchiveUrl);
        });

        uploadCommand.run();
        int uploadCommandExitCode = uploadCommand.waitFor();

        log("Finished task UploadMediaWiki");

        if (uploadCommandExitCode != 0) {
            log("---");
            log("This job failed to upload, marking it failed...");
            return new JobResult(false, uploadCommandExitCode);
        }

        log("");
        log("---");
        log("Job done!");
        log("archive.org Item URL: " + this.getArchiveUrl());

        runningTask = "LinkExtract";
        CommonTasks.extractLinks(this);
        runningTask = null;
        return new JobResult(true, 0);
    }

    public boolean abort() {
        if (this.getStatus() == JobStatus.QUEUED) {
            this.setStatus(JobStatus.ABORTED);
            WikiBot.getBus().post(new JobAbortEvent(this));
            return true;
        }
        if (!runningTask.equals("UploadMediaWiki")) {
            this.setStatus(JobStatus.ABORTED);
            log("----- Bot: Aborting task " + runningTask + " -----");
            downloadCommand.getProcess().descendants().forEach(ProcessHandle::destroyForcibly);
            downloadCommand.getProcess().destroyForcibly();
            if (itemCommand != null) {
                itemCommand.getProcess().descendants().forEach(ProcessHandle::destroyForcibly);
                itemCommand.getProcess().destroyForcibly();
            }
            log("----- Bot: Aborted task " + runningTask + " -----");
            return true;
        }
        return false;
    }

    public JobType getType() {
        return JobType.WIKITEAM3;
    }

    public List<String> getAllTasks() {
        return List.of("DownloadMediaWiki", "UploadMediaWiki", "Wget-AT", "LinkExtract");
    }


}
