package dev.digitaldragon.jobs;

import dev.digitaldragon.backfeed.LinkExtract;
import dev.digitaldragon.util.UploadObject;

import java.io.*;
import java.util.Set;

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

    public static int runUpload(Job job, File directory, StringLogHandler handler, RunCommand uploadCommand, JobType type) {
        handler.onMessage("----- Bot: Task " + job.getRunningTask() + " started -----");
        if (directory.listFiles() == null) {
            return 999;
        }

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                if (type == JobType.WIKITEAM3) {
                    uploadCommand = new RunCommand("wikiteam3uploader " + file.getName() + " --zstd-level 22 --parallel", directory, handler);
                }
                if (type == JobType.DOKUWIKIDUMPER) {
                    uploadCommand = new RunCommand("dokuWikiUploader " + file.getName(), directory, handler);
                }
                break;
            }
        }
        if (uploadCommand == null) {
            return 999;
        }
        return CommonTasks.runAndVerify(uploadCommand, handler, job.getRunningTask());
    }


    public static void extractLinks(Job job) {
        for (File file : job.getDirectory().listFiles()) {
            System.out.println(file.getName());
            System.out.println(file.getAbsolutePath());
            if (file.isDirectory()) {
                extractLinksFromDumpsDir(job, file);
            }
        }
    }

    private static void extractLinksFromDumpsDir(Job job, File directory) {
        for (File file : directory.listFiles()) {
            System.out.println(file.getName());
            System.out.println(file.getAbsolutePath());
            if (file.getAbsolutePath().endsWith(".xml")) {
                try {
                    Set<String> links = LinkExtract.extractLinksFromFile(new FileInputStream(file));
                    File linkFile = new File(job.getDirectory(), "links.txt");
                    for (String link : links) {
                        writeLineToFile(linkFile, link);
                    }
                    if (links.isEmpty()) {
                        System.out.println("No links found in " + file.getName());
                    }
                } catch (FileNotFoundException e) {
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
