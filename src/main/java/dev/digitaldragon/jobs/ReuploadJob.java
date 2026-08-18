package dev.digitaldragon.jobs;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.util.Config;
import lombok.Getter;

import java.io.File;
import java.util.List;

/**
 * Job to re-attempt an upload of a dump to archive.org.
 */
@Getter
public class ReuploadJob extends Job {
    private final String id;

    private String runningTask = null;
    private File directory = null;
    private transient RunCommand uploadCommand = null;
    private final String targetId;
    private boolean aborted = false;
    private JobMeta meta;

    public ReuploadJob(String userName, String id, String targetId) {
        this.id = id;
        meta = new JobMeta(userName, JobMeta.JobPlatform.IRC);
        meta.setExplain("Reupload job " + targetId);
        // non-input params
        this.directory = new File("jobs/" + id + "/");
        this.directory.mkdirs();
        this.targetId = targetId;
    }

    protected JobResult execute() {
        runningTask = "StartUpload";

        log("looking for dump to reupload for job ID " + targetId);


        File dumpDir = CommonTasks.findDumpDir(targetId);
        if (dumpDir == null || dumpDir.listFiles() == null) {
            this.log("Fatal: Couldn't find the dump to upload! It probably doesn't exist, or you entered an incorrect Job ID.");
            return new JobResult(false, 999);
        }

        JobType jobType = null;

        for (File child : dumpDir.listFiles()) {
            if (child.getName().equals("siteinfo.json")) {
                jobType = JobType.WIKITEAM3;
                break;
            }

            if (child.getName().equals("meta")) {
                jobType = JobType.DOKUWIKIDUMPER;
                break;
            }

            if (child.getName().equals("wiki")) {
                jobType = JobType.PUKIWIKIDUMPER;
                break;
            }
        }

        if (jobType == null) {
            log("Could not determine job type. The dump may be incomplete or have already been uploaded. Maybe try --resume instead?");
            return new JobResult(false, 999);
        }

        String[] uploadParams = null;

        switch (jobType) {
            case JobType.WIKITEAM3 -> {
                Config.UploadConfig uploadConfig = WikiBot.getConfig().getUploadConfig();
                uploadParams = new String[]{"wikiteam3uploader", dumpDir.getName(),
                        "--zstd-level", "22",
                        "--parallel",
                        "--bin-zstd", WikiBot.getConfig().getWikiTeam3Config().binZstd(),
                        "--collection", uploadConfig.collection()};

                if (uploadConfig.offloadEnabled()) {
                    String[] newUploadParams = new String[uploadParams.length + 2];
                    newUploadParams[newUploadParams.length - 2] = "--offload";
                    newUploadParams[newUploadParams.length - 1] = uploadConfig.offloadServer();
                    System.arraycopy(uploadParams, 0, newUploadParams, 0, uploadParams.length);
                    uploadParams = newUploadParams;
                }
            }
            case JobType.DOKUWIKIDUMPER -> uploadParams = new String[]{"dokuWikiUploader", dumpDir.getName(), "--collection", WikiBot.getConfig().getUploadConfig().collection()};
            case JobType.PUKIWIKIDUMPER -> uploadParams = new String[]{"pukiWikiUploader", dumpDir.getName(), "--collection", WikiBot.getConfig().getUploadConfig().collection()};
        }

        if (uploadParams == null) {
            // This should be unreachable
            log("Found job of type " + jobType.name() + " but failed to determine the upload parameters! This looks like a bug, please report this to a bot admin!");
            return new JobResult(false, 999);
        }

        uploadCommand = new RunCommand(uploadParams, dumpDir.getParentFile(), message -> {
            this.log(message);
            CommonTasks.getArchiveUrl(message).ifPresent(this::setArchiveUrl);
        });


        runningTask = "Upload";
        uploadCommand.run();
        int exitCode = uploadCommand.waitFor();
        if (exitCode != 0) {
            return new JobResult(false, exitCode);
        }

        runningTask = null;
        return new JobResult(true, 0);
    }

    public boolean abort() {
        if (isRunning())
            return false;
        this.setStatus(JobStatus.ABORTED);
        WikiBot.getBus().post(new JobAbortEvent(this));
        return true;
    }

    public JobType getType() {
        return JobType.REUPLOAD;
    }

    public List<String> getAllTasks() {
        return List.of("StartUpload", "Upload");
    }
}
