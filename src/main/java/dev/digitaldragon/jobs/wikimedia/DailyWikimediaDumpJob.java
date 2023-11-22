package dev.digitaldragon.jobs.wikimedia;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.ThreadChannel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Getter
public class DailyWikimediaDumpJob implements Job {
    private String id ;
    private final String name = "Wikimedia Incremental Dump";
    private final String userName = "AutomaticDragon";
    private JobStatus status;
    private String runningTask = null;
    private Instant startTime = null;
    private File directory;
    private final String explanation = "This job is automatically created by the bot to download the Wikimedia incremental dumps every day.";
    @Setter
    private String archiveUrl = "https://archive.org/details/@DigitalDragons";
    @Setter
    private String logsUrl = null;
    @Setter
    private ThreadChannel threadChannel = null;
    private GenericLogsHandler handler;
    private int failedTaskCode;
    private boolean aborted;
    private List<WikimediaWiki> wikis = new ArrayList<>();
    private Map<String, String> wikiInfo = new HashMap<>();

    public DailyWikimediaDumpJob(String id) {
        this.id = id;
        this.status = JobStatus.QUEUED;
        this.directory = new File("jobs/" + id + "/");
        this.directory.mkdirs();
        this.handler = new GenericLogsHandler(this);
    }

    private void fail(String message) {
        this.status = JobStatus.FAILED;
        this.runningTask = null;
        handler.onMessage(message);
        handler.end();
        WikiBot.getBus().post(new JobFailureEvent(this));
    }

    public void run() {
        this.startTime = Instant.now();
        this.status = JobStatus.RUNNING;
        this.runningTask = "DOWNLOAD";
        System.out.println("go");
        handler.onMessage("Parsing the wikis.txt file...");
        parseWikiNames();
        handler.onMessage("Requesting the incremental dumps page from the Wikimedia servers...");

        String incrementalDumpUrl = "https://dumps.wikimedia.org/other/incr/";
        try {
            extractWikis(incrementalDumpUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("done extracting wikis");
        List<String> identifiers = new ArrayList<>();
        for (WikimediaWiki wiki : wikis) {
            handler.onMessage("Fetching md5 sums for " + wiki.getId() + "...");
            try {
                extracMD5sums(wiki.getMd5sumsUrl(), wiki);
                Thread.sleep(1000);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(wiki.getMd5sumsUrl());
            System.out.println(wiki.getStubsMD5());
            System.out.println(wiki.getPagesMD5());
            System.out.println(wiki.getMaxrevMD5());
            addWikiName(wiki);

            try {
                identifiers.add(getArchiveIndentifier(wiki));

                downloadAndUpload(wiki.getStubsUrl(), wiki.getStubsMD5(), wiki);
                downloadAndUpload(wiki.getPagesUrl(), wiki.getPagesMD5(), wiki);
                downloadAndUpload(wiki.getMaxrevUrl(), wiki.getMaxrevMD5(), wiki);
                downloadAndUpload(wiki.getMd5sumsUrl(), null, wiki);
                handler.onMessage(wiki.getId() + " ---> " + "https://archive.org/details/" + getArchiveIndentifier(wiki));

                System.out.println(wiki.getName());
                Thread.sleep(5000);
            } catch (IOException | NoSuchAlgorithmException | InterruptedException e) {
                e.printStackTrace();
            }
            break;

        }

        handler.onMessage("Done processing all wikis!");
        handler.onMessage("Items produced in this run:");
        for (String identifier : identifiers) {
            handler.onMessage(identifier);
        }

        logsUrl = CommonTasks.uploadLogs(this);

        status = JobStatus.COMPLETED;
        runningTask = null;
        handler.end();
        WikiBot.getBus().post(new JobSuccessEvent(this));
    }


    private void downloadAndUpload(String url, String md5, WikimediaWiki wiki) throws IOException, NoSuchAlgorithmException, InterruptedException {
        File downloadedFile = downloadAndVerifyFile(url, md5);
        RunCommand uploadCommand = new RunCommand(getFirstFileUploadCommand(downloadedFile, wiki), directory, handler);
        int exitCode = CommonTasks.runAndVerify(uploadCommand, handler, "Upload " + downloadedFile.getName());
        if (exitCode != 0) {
            failedTaskCode = exitCode;
            fail("Failed to upload " + downloadedFile.getName());
        }
        Thread.sleep(500);
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

    private void extractWikis(String incrementalDumpUrl) throws IOException {
        Document doc = Jsoup.connect(incrementalDumpUrl).get();
        handler.onMessage("Got the incremental dumps page from the Wikimedia servers!");
        handler.onMessage("Extracting the wikis...");
        //find the <li> blocks
        doc.select("li").forEach(li -> {
            WikimediaWiki wiki = new WikimediaWiki();
            //String date;
            //System.out.println(li);
            //the <strong> tag inside the <li> block contains the wiki ID
            if (!li.text().contains("(done:all)")) return;

            wiki.setId(li.select("strong").text());
            // the <a> tags inside the <li> block contain the URLs
            li.select("a").forEach(a -> {
                String href = a.attr("href");
                if (href.contains("stubs")) {
                    wiki.setStubsUrl(incrementalDumpUrl + href);
                    wiki.setDumpDate(href.split("-")[1]);
                } else if (href.contains("pages")) {
                    wiki.setPagesUrl(incrementalDumpUrl + href);
                    System.out.println(href.split("-")[1]);
                    //if (!wiki.getDumpDate().equals(href.split("-")[1])) handler.onMessage("Dump date mismatch! " + wiki.getId() + " found " + wiki.getDumpDate() + " but extracted " + href.split("-")[1]);;
                } else if (href.contains("maxrev")) {
                    wiki.setMaxrevUrl(incrementalDumpUrl + href);
                    //do not extract the date, because its not included here
                } else if (href.contains("md5sums")) {
                    wiki.setMd5sumsUrl(incrementalDumpUrl + href);
                    System.out.println(href.split("-")[1]);
                    //if (!wiki.getDumpDate().equals(href.split("-")[1])) handler.onMessage("Dump date mismatch! " + wiki.getId() + " found " + wiki.getDumpDate() + " but extracted " + href.split("-")[1]);;
                    //if (!wiki.getDumpDate().equals(href.split("-")[1])) throw new RuntimeException("Dump date mismatch");
                }
            });

            //if any of the URLs are null, don't add the wiki
            if (wiki.getStubsUrl() == null || wiki.getPagesUrl() == null || wiki.getMaxrevUrl() == null || wiki.getMd5sumsUrl() == null) {
                System.out.println("invalid wiki record");
                System.out.println(li);
            } else {
                wikis.add(wiki);
            }
        });
    }

    private void extracMD5sums(String url, WikimediaWiki wiki) throws IOException {
        //MD5 sums are in the format of <md5sum> <filename> followed by a newline
        //use wildcard search for "stubs" and "pages" to get the MD5 sums for those files

        List<String> md5sums = readTextFileFromUrl(url);
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

    private void parseWikiNames() {
        //wikiid;url;name;language;locallanguage
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/wikis.txt")))) {
            String line;
            // Read each line from the BufferedReader and add it to the list
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                String[] split = line.split(";", 2);
                wikiInfo.put(split[0], split[1]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addWikiName(WikimediaWiki wiki) {
        String wikiId = wiki.getId();
        String info = wikiInfo.get(wikiId);
        String[] split = info.split(";");
        wiki.setName(split[1]);
    }

    private File downloadAndVerifyFile(String fileUrl, String expectedMd5) throws IOException, NoSuchAlgorithmException {
        // Create a URL object
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

            // Verify MD5 checksum
            if (expectedMd5 == null) return downloadLocation;
            String actualMd5 = calculateMd5Checksum(downloadLocation.getAbsolutePath());
            System.out.println(expectedMd5);
            System.out.println(actualMd5);
            if (expectedMd5.equals(actualMd5)) {
                handler.onMessage("Downloaded " + fileUrl + " successfully with md5 " + expectedMd5);
            } else {
                handler.onMessage("Failed to download " + fileUrl + " with md5 mismatch!");
                handler.onMessage("Expected " + expectedMd5 + " but got " + actualMd5);
                failedTaskCode = 1;
                fail("Failed to download " + fileUrl + " with md5 mismatch!");
                //TODO automatic retry
            }
            return downloadLocation;
        }
    }

    private String getArchiveIndentifier(WikimediaWiki wiki) {
        return "test_incr-" + wiki.getId() + "-" + wiki.getDumpDate() + "_dd4";
    }

    private String getFirstFileUploadCommand(File file, WikimediaWiki wiki) throws IOException {
        String wikiName = wiki.getName();
        DateTimeFormatter formatterInput = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate localDate = LocalDate.parse(wiki.getDumpDate(), formatterInput);
        DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        String date = localDate.format(formatterOutput);
        String identifier = getArchiveIndentifier(wiki);
        String command = "ia upload " + identifier + " "
                + file.getAbsolutePath() + " --metadata=\"mediatype:web\" --metadata=\"title:Wikimedia incremental dump files for " + wikiName + " on " + date + "\" "
                + "--metadata=\"creator:Wikimedia projects editors\" --metadata=\"subject:wiki;incremental;dumps;" + wiki.getId() + ";" + wikiName + ";Wikimedia\" "
                + "--metadata=\"description:These are the incremental dump files for " + wikiName + " that were generated by the Wikimedia Foundation on " + date + ".\" "
                + "--metadata=\"licenseurl:http://creativecommons.org/licenses/by-sa/3.0/\" --metadata=\"rights:Permission is granted under the Wikimedia Foundation's <a href=\\\"https://wikimediafoundation.org/wiki/Terms_of_Use\\\" rel=\\\"nofollow\\\">Terms of Use</a>. There is also additional <a href=\\\"https://archive.org/download/wikimediadownloads/legal.html\\\" rel=\\\"nofollow\\\">copyright information available</a>\" "
                + "--metadata=\"date:" + wiki.getDumpDate() + "\" "
                + "--retries 10";
        return command;
    }

    private String getAltFileUploadCommand(File file, WikimediaWiki wiki) {
        waitForIaItem(getArchiveIndentifier(wiki));
        String command = "ia upload " + getArchiveIndentifier(wiki) + " " + file.getAbsolutePath();
        return command;
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

    private void waitForIaItem(String identifier) {
        String url = "https://archive.org/metadata/" + identifier;
        while (true) {
            try {
                if (!readTextFileFromUrl(url).contains("{\"error\":\"missing identifier\"}"))
                    break;
                Thread.sleep(10000);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<String> readTextFileFromUrl(String fileUrl) throws IOException {
        List<String> lines = new ArrayList<>();
        URL url = new URL(fileUrl);

        // Open a connection to the URL
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            // Read each line from the BufferedReader and add it to the list
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        return lines;
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

    }
}
