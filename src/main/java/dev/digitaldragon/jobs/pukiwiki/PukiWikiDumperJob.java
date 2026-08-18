package dev.digitaldragon.jobs.pukiwiki;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.generic.Command;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import lombok.Getter;

import java.io.File;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;

/**
 * The DokuWikiDumperJob class represents a job for dumping a DokuWiki instance.
 *
 * This class implements the Job interface.
 *
 * The DokuWikiDumperJob class provides methods for creating, running, and
 * aborting a DokuWiki dump job.
 */
@Getter
public class PukiWikiDumperJob extends Job {
    private String runningTask = null;
    private final File directory;
    private transient RunCommand downloadCommand = null;
    private transient RunCommand uploadCommand = null;
    private PukiWikiDumperArgs args;
    private JobMeta meta;

    public PukiWikiDumperJob(PukiWikiDumperArgs args, JobMeta meta) throws JobLaunchException {
        meta.setTargetUrl(Optional.ofNullable(args.getUrl()).orElseThrow(() -> new JobLaunchException("You need to specify the wiki URL.")));

        this.args = args;
        this.meta = meta;
        this.id = id;

        this.directory = new File("jobs/" + id + "/");
        this.directory.mkdirs();
    }

    public PukiWikiDumperJob(String unparsedArgs, JobMeta meta) throws JobLaunchException, ParseException {
        this(
                new PukiWikiDumperArgs(Command.shellSplit(unparsedArgs).toArray(new String[0]), meta),
                meta
        );
    }

    protected JobResult execute() {
        WikiBot.getLogFiles().setLogFile(this, new File(directory, "log.txt"));

        List<String> dumpArgs = args.get();
        File runDir = directory;
        if (args.getResume() != null) {
            File resumeDir = CommonTasks.findDumpDir(args.getResume());

            if (resumeDir == null) {
                log("Error (bot): Unknown job " + args.getResume());
                return new JobResult(false, 1);
            }

            runDir = resumeDir.getParentFile();
            dumpArgs.add("--path");
            dumpArgs.add(resumeDir.getName());
        }



        runningTask = "Dump";
        log("Starting dump task");

        downloadCommand = new RunCommand(dumpArgs.toArray(new String[0]), runDir, message -> {
            log(message);
            CommonTasks.getArchiveUrl(message).ifPresent(this::setArchiveUrl);
        });

        downloadCommand.run();
        int downloadExitCode = downloadCommand.waitFor();
        if (downloadExitCode != 0) {
            return new JobResult(false, downloadExitCode);
        }

        log("Finished dump task");

        runningTask = "Upload";
        log("Starting upload task");

        File dumpDir = CommonTasks.findDumpDir(runDir);
        if (dumpDir == null) {
            log("Failed to find the dump directory, aborting...");
            return new JobResult(false, 999);
        }
        String[] uploadParams = new String[] {"pukiWikiUploader", dumpDir.getName(), "--collection", WikiBot.getConfig().getUploadConfig().collection()};
        uploadCommand = new RunCommand(uploadParams, runDir, message -> {
            log(message);
            CommonTasks.getArchiveUrl(message).ifPresent(this::setArchiveUrl);
        });

        uploadCommand.run();
        if (uploadCommand.waitFor() != 0) {
            return new JobResult(false, uploadCommand.waitFor());
        }

        log("Finished task upload");

        runningTask = null;
        return new JobResult(true, 0);
    }


    public boolean abort() {
        if (this.getStatus() == JobStatus.QUEUED) {
            this.setStatus(JobStatus.ABORTED);
            WikiBot.getBus().post(new JobAbortEvent(this));
            return true;
        }
        if (runningTask.equals("Dump")) {
            this.setStatus(JobStatus.ABORTED);
            log("----- Bot: Aborting task " + runningTask + " -----");
            downloadCommand.getProcess().descendants().forEach(ProcessHandle::destroyForcibly);
            downloadCommand.getProcess().destroyForcibly();
            log("----- Bot: Aborted task " + runningTask + " -----");
            runningTask = "AbortTask";
            return true;
        }
        return false;
    }

    public JobType getType() {
        return JobType.PUKIWIKIDUMPER;
    }

    public List<String> getAllTasks() {
        return List.of("Dump", "Upload");
    }
}
