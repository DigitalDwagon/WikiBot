package dev.digitaldragon.jobs.wikimedia;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.events.JobCompletedEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobRunningEvent;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class DailyWikimediaDumpJob extends Job {
    private String id ;
    private final String name = "Wikimedia Incremental Dump";
    private final String userName = "AutomaticDragon";
    @Setter
    private JobStatus status;
    private String runningTask = null;
    private Instant startTime = null;
    private File directory;
    private final String explanation = "This job is automatically created by the bot to download the Wikimedia incremental dumps every day.";
    @Setter
    private String archiveUrl = "https://archive.org/details/@DigitalDragons";
    @Setter
    private String logsUrl = null;
    private int failedTaskCode;
    private boolean aborted;
    private final List<WikimediaWiki> wikis = new ArrayList<>();
    private final Map<String, String> wikiInfo = new HashMap<>();
    private final List<File> inflightFiles = new ArrayList<>();
    private JobMeta meta;

    public DailyWikimediaDumpJob(String id) {
        this.id = id;
        this.status = JobStatus.QUEUED;
        this.directory = new File("jobs/" + id + "/");
        this.directory.mkdirs();
        this.meta = new JobMeta("Wikibot-internal-queue", JobMeta.JobPlatform.API);
        meta.setExplain("This job is automatically created by the bot to download the Wikimedia incremental dumps every day.");
        meta.setTargetUrl("https://dumps.wikimedia.org/other/incr/");
    }

    private void fail(String message) {
        this.status = JobStatus.FAILED;
        log(message);
        CommonTasks.uploadLogs(this);
        WikiBot.getBus().post(new JobFailureEvent(this));
        throw new RuntimeException(message);
    }

    public void run() {
        this.startTime = Instant.now();
        this.status = JobStatus.RUNNING;
        WikiBot.getBus().post(new JobRunningEvent(this));
        this.runningTask = "DOWNLOAD";
        this.log("Parsing the wikis.txt file...");
        parseWikiNames();
        this.log("Requesting the incremental dumps page from the Wikimedia servers...");

        String incrementalDumpUrl = "https://dumps.wikimedia.org/other/incr/";
        try {
            extractWikis(incrementalDumpUrl);
        } catch (IOException e) {
            this.log("Failed to get the incremental dumps page from the Wikimedia servers!");
            this.log("Error: " + e.getMessage());
            failedTaskCode = 1;
            fail("Failed to get the incremental dumps page from the Wikimedia servers!");
        }
        this.log("");
        this.log("");
        for (WikimediaWiki wiki : wikis) {
            this.log("---");
            this.log(wiki.getId());
            this.log(wiki.getStubsUrl());
            this.log(wiki.getPagesUrl());
            this.log(wiki.getMaxrevUrl());
            this.log(wiki.getMd5sumsUrl());
        }
        this.log("");
        this.log("");

        List<String> identifiers = new ArrayList<>();
        for (WikimediaWiki wiki : wikis) {
            try {//todo this definitely needs multithreading
                this.log("");
                this.log("");
                this.log("Processing " + wiki.getId() + "...");
                this.log("Fetching md5 sums for " + wiki.getId() + "...");
                File md5 = downloadFile(wiki.getMd5sumsUrl());
                extractMD5sums(md5, wiki);
                Thread.sleep(1000);


                md5Matches(downloadFile(wiki.getStubsUrl()), wiki.getStubsMD5());
                md5Matches(downloadFile(wiki.getPagesUrl()), wiki.getPagesMD5());
                md5Matches(downloadFile(wiki.getMaxrevUrl()), wiki.getMaxrevMD5());
                this.log("");

                String identifier = uploadInflightFiles(wiki);
                this.log(wiki.getId() + " ---> " + "https://archive.org/details/" + identifier);
                identifiers.add(identifier);
                cleanupInflightFiles();
                this.log("Finished processing " + wiki.getId() + "!");
                this.log("Waiting before processing the next wiki...");
            } catch (IOException | NoSuchAlgorithmException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.log("");
        this.log("");
        this.log("Done processing all wikis!");
        this.log("Items produced in this run:");
        for (String identifier : identifiers) {
            this.log(identifier);
        }

        logsUrl = CommonTasks.uploadLogs(this);

        status = JobStatus.COMPLETED;
        runningTask = null;
        archiveUrl = "https://archive.org/details/@digitaldragons";
        WikiBot.getBus().post(new JobCompletedEvent(this));
    }

    private void extractWikis(String incrementalDumpUrl) throws IOException {
        Document doc = Jsoup.connect(incrementalDumpUrl).get();
        this.log("Got the incremental dumps page from the Wikimedia servers!");
        this.log("Extracting the wikis...");
        doc.select("li").forEach(li -> {
            WikimediaWiki wiki = new WikimediaWiki();
            if (!li.text().contains("(done:all)")) return;

            wiki.setId(li.select("strong").text());
            li.select("a").forEach(a -> {
                String href = a.attr("href");
                if (href.contains("stubs")) {
                    wiki.setStubsUrl(incrementalDumpUrl + href);
                    wiki.setDumpDate(href.split("-")[1]);
                } else if (href.contains("pages")) {
                    wiki.setPagesUrl(incrementalDumpUrl + href);
                } else if (href.contains("maxrev")) {
                    wiki.setMaxrevUrl(incrementalDumpUrl + href);
                } else if (href.contains("md5sums")) {
                    wiki.setMd5sumsUrl(incrementalDumpUrl + href);
                }
            });

            //if any of the URLs are null, don't add the wiki
            if (wiki.getStubsUrl() == null || wiki.getPagesUrl() == null || wiki.getMaxrevUrl() == null || wiki.getMd5sumsUrl() == null) {
                this.log("Failed to extract " + wiki.getId() + " because one or more link URLs were null!");
                this.log(li.toString());
            } else {
                wikis.add(wiki);
            }
        });
    }

    private void extractMD5sums(File file, WikimediaWiki wiki) throws IOException {
        //same as above but from a local .txt file
        List<String> md5sums = Files.lines(file.toPath()).toList();
        for (String md5sum : md5sums) {
            String[] split = md5sum.split(" ");
            String md5 = split[0];
            String filename = split[1];
            if (filename.contains("stubs")) {
                wiki.setStubsMD5(md5);
            } else if (filename.contains("pages")) {
                wiki.setPagesMD5(md5);
            } else if (filename.contains("maxrev")) {
                wiki.setMaxrevMD5(md5);
            }
        }
    }

    private String getArchiveIndentifier(WikimediaWiki wiki) {
        return "incr-" + wiki.getId() + "-" + wiki.getDumpDate();
    }

    private String uploadInflightFiles(WikimediaWiki wiki) {
        runningTask = "Upload " + wiki.getId();
        String wikiName = wiki.getName();
        DateTimeFormatter formatterInput = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate localDate = LocalDate.parse(wiki.getDumpDate(), formatterInput);
        DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String date = localDate.format(formatterOutput);
        String identifier = getArchiveIndentifier(wiki);
        String files = inflightFiles.stream().map(File::getAbsolutePath).collect(Collectors.joining(" "));
        String command = "ia upload " + identifier
                + " " + files
                + " --metadata=\"mediatype:data\""
                + " --metadata=\"title:Wikimedia incremental dump files for " + wikiName + " on " + date + "\""
                + " --metadata=\"creator:Wikimedia projects editors\""
                + " --metadata=\"subject:wiki;incremental;dumps;" + wiki.getId() + ";" + wikiName + ";Wikimedia\""
                + " --metadata=\"description:These are the incremental dump files for " + wikiName + " that were generated by the Wikimedia Foundation on " + date + ".\""
                + " --metadata=\"licenseurl:http://creativecommons.org/licenses/by-sa/3.0/\""
                + " --metadata=\"rights:Permission is granted under the Wikimedia Foundation's <a href=\\\"https://wikimediafoundation.org/wiki/Terms_of_Use\\\" rel=\\\"nofollow\\\">Terms of Use</a>. There is also additional <a href=\\\"https://archive.org/download/wikimediadownloads/legal.html\\\" rel=\\\"nofollow\\\">copyright information available</a>\""
                + " --metadata=\"date:" + wiki.getDumpDate() + "\""
                + " --metadata=\"creator:Wikimedia Foundation\""
                + " --retries 50";

        RunCommand uploadCommand = new RunCommand(command, null, directory, this::log);
        int exitCode = uploadCommand.waitFor();

        if (exitCode != 0) {
            failedTaskCode = exitCode;
            fail("Failed to upload " + files);
        }

        return identifier;
    }

    @Override
    public boolean abort() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return this.getStatus() == JobStatus.RUNNING;
    }

    @Override
    public JobType getType() {
        return JobType.WIKIMEDIADUMPINCR;
    }

    @Override
    public List<String> getAllTasks() {
        return null;
    }

    private File downloadFile(String fileUrl) throws MalformedURLException {
        this.log("Downloading " + fileUrl + " ...");
        URL url = new URL(fileUrl);
        String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        File downloadLocation = new File(directory.getAbsolutePath() + "/" + fileName);

        // Open a connection to the URL
        try (BufferedInputStream in = new BufferedInputStream(url.openStream())) {
            // Get the file name from the URL

            // Create a FileOutputStream to save the downloaded file
            try (FileOutputStream fos = new FileOutputStream(downloadLocation)) {
                byte[] data = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(data, 0, 1024)) != -1) {
                    fos.write(data, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            //TODO retry download depending on status code
            failedTaskCode = 1;
            this.log(e.getMessage());
            fail("Failed to download " + fileUrl + " because of an IOException!");
        }
        inflightFiles.add(downloadLocation);
        return downloadLocation;
    }

    private void cleanupInflightFiles() {
        this.log("");
        this.log("Cleaning up inflight files...");
        for (File file : inflightFiles) {
            this.log("Deleting " + file.getName());
            file.delete();
        }
        this.log("");
        inflightFiles.clear();
    }

    private boolean md5Matches(File file, String expectedMd5) throws NoSuchAlgorithmException, IOException {
        String actualMd5 = calculateMd5Checksum(file.getAbsolutePath());
        return expectedMd5.equals(actualMd5);
    }

    private static String calculateMd5Checksum(String filePath) throws NoSuchAlgorithmException, IOException {
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        try (InputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md5Digest.update(buffer, 0, bytesRead);
            }
        }

        // Convert the byte array to a hex string
        StringBuilder md5Checksum = new StringBuilder();
        for (byte b : md5Digest.digest()) {
            md5Checksum.append(String.format("%02x", b));
        }

        return md5Checksum.toString();
    }

    private void parseWikiNames() {
        //wikiid;url;name;language;locallanguage
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/wikis.txt")))) {
            String line;
            // Read each line from the BufferedReader and add it to the list
            while ((line = reader.readLine()) != null) {
                String[] split = line.split(";", 2);
                wikiInfo.put(split[0], split[1]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Getter
    @Setter
    private class WikimediaWiki {
        private String name;
        private String url;
        private String stubsUrl;
        private String stubsMD5;
        private String pagesUrl;
        private String pagesMD5;
        private String maxrevUrl;
        private String maxrevMD5;
        private String md5sumsUrl;
        private String id;
        private String dumpDate;

        private String getName() {
            if (name == null) {
                String info = wikiInfo.get(id);
                String[] split = info.split(";");
                name = split[1];
            }
            return name;
        }

    }
}
