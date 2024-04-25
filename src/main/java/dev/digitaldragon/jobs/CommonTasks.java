package dev.digitaldragon.jobs;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.backfeed.LinkExtract;
import dev.digitaldragon.util.UploadObject;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The CommonTasks class provides common tasks and utilities for various operations.
 */
public class CommonTasks {
    public static int runAndVerify(RunCommand command, StringLogHandler handler, String taskName) {
        command.run();

        try {
            int exitCode = command.getProcess().waitFor();
            handler.onMessage("----- Bot: Task " + taskName + " finished -----");
            handler.onMessage("----- Bot: Exit code: " + exitCode + " -----");

            return exitCode;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return 999;
    }

    public static String uploadLogs(Job job) {
        String jobId = job.getId();
        try {
            UploadObject.uploadObject("digitaldragons", "cdn.digitaldragon.dev", "wikibot/jobs/" + jobId + "/log.txt", String.format("jobs/%s/log.txt", jobId), "text/plain; charset=utf-8", "inline");
        } catch (IOException e) {
            e.printStackTrace();
            return "Error uploading logs. Sorry :(";
        }
        return String.format("https://cdn.digitaldragon.dev/wikibot/jobs/%s/log.txt", jobId);
    }

    @Deprecated
    public static int runUpload(Job job, File directory, StringLogHandler handler, RunCommand uploadCommand, JobType type) {
        handler.onMessage("----- Bot: Task " + job.getRunningTask() + " started -----");
        if (directory.listFiles() == null) {
            return 999;
        }

        Consumer<String> jobWatcher = (message -> {
            job.log(message);
            CommonTasks.getArchiveUrl(message).ifPresent(job::setArchiveUrl);
        });

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                if (type == JobType.WIKITEAM3) {
                    uploadCommand = new RunCommand("wikiteam3uploader " + file.getName() + " --zstd-level 22 --parallel --bin-zstd " + WikiBot.getConfig().getWikiTeam3Config().binZstd(), null, directory, jobWatcher);
                }
                if (type == JobType.DOKUWIKIDUMPER) {
                    uploadCommand = new RunCommand("dokuWikiUploader " + file.getName(), null, directory, jobWatcher);
                }
                if (type == JobType.PUKIWIKIDUMPER) {
                    uploadCommand = new RunCommand("pukiWikiUploader " + file.getName(), null, directory, jobWatcher);
                }
                break;
            }
        }
        if (uploadCommand == null) {
            return 999;
        }
        return CommonTasks.runAndVerify(uploadCommand, handler, job.getRunningTask());
    }

    @Nullable
    public static File findDumpDir(String jobId) {
        File directory = new File("jobs/" + jobId);
        return findDumpDir(directory);
    }

    @Nullable
    public static File findDumpDir(File directory) {
        if (!directory.exists()) {
            return null;
        }
        if (directory.listFiles() == null) {
            return null;
        }
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                return file;
            }
        }
        return null;
    }

    public static Optional<String> getArchiveUrl(String message) {
        if (message.contains("https://archive.org/details/")) {
            String[] split = message.split(" ");
            for (String s : split) {
                if (s.contains("https://archive.org/details/")) {
                    return Optional.of(s);
                }
            }
        }
        return Optional.empty();
    }

    public static void extractLinks(Job job) {
        for (File file : job.getDirectory().listFiles()) {
            if (file.isDirectory()) {
                extractLinksFromDumpsDir(job, file);
            }
        }
    }

    private static void extractLinksFromDumpsDir(Job job, File directory) {
        for (File file : directory.listFiles()) {
            if (file.getAbsolutePath().endsWith(".xml")) {
                System.out.println(file.getAbsolutePath());
                try {
                    Set<String> links = LinkExtract.extractLinksFromFile(new FileInputStream(file));
                    File linkFile = new File(job.getDirectory(), "links.txt");
                    File globalLinkFile = new File("all_links.txt");
                    for (String link : links) {
                        writeLineToFile(linkFile, link);
                        writeLineToFile(globalLinkFile, link);
                    }
                    if (links.isEmpty()) {
                        System.out.println("No links found in " + file.getName());
                    }
                } catch (Exception e) { //todo
                    e.printStackTrace();
                }
            }
        }
    }

    private static void writeLineToFile(File file, String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.append(line);
            writer.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
