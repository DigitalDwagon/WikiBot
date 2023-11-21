package dev.digitaldragon.jobs.wikimedia;

import dev.digitaldragon.jobs.*;
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
import java.util.*;

@Getter
public class DailyWikimediaDumpJob implements Job {
    private String id = null;
    private final String name = "Wikimedia Incremental Dump";
    private final String userName = "AutomaticDragon";
    private JobStatus status = null;
    private String runningTask = null;
    private Instant startTime = null;
    private File directory = null;
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

    public void run() {
        this.startTime = Instant.now();
        this.status = JobStatus.RUNNING;
        this.runningTask = "DOWNLOAD";
        System.out.println("go");
        parseWikiNames();
        System.out.println("done parsing wiki names");

        String incrementalDumpUrl = "https://dumps.wikimedia.org/other/incr/";
        try {
            extractWikis(incrementalDumpUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("done extracting wikis");
        for (WikimediaWiki wiki : wikis) {
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
                downloadAndVerifyFile(wiki.getStubsUrl(), wiki.getStubsMD5());
                System.out.println(wiki.getName());
                Thread.sleep(5000);
            } catch (IOException | NoSuchAlgorithmException | InterruptedException e) {
                e.printStackTrace();
            }
            break;

        }


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
        System.out.println("got dumps page");
        //find the <li> blocks
        doc.select("li").forEach(li -> {
            WikimediaWiki wiki = new WikimediaWiki();
            //String date;
            System.out.println(li);
            //the <strong> tag inside the <li> block contains the wiki ID
            if (!li.text().contains("(done:all)")) return;

            wiki.setId(li.select("strong").text());
            // the <a> tags inside the <li> block contain the URLs
            li.select("a").forEach(a -> {
                String href = a.attr("href");
                if (href.contains("stubs")) {
                    wiki.setStubsUrl(incrementalDumpUrl + href);
                    //wiki.setDumpDate(href.split("-")[1]);
                } else if (href.contains("pages")) {
                    wiki.setPagesUrl(incrementalDumpUrl + href);
                    //if (!wiki.getDumpDate().equals(href.split("-")[1])) throw new RuntimeException("Dump date mismatch");
                } else if (href.contains("maxrev")) {
                    wiki.setMaxrevUrl(incrementalDumpUrl + href);
                    //if (!wiki.getDumpDate().equals(href.split("-")[1])) throw new RuntimeException("Dump date mismatch");
                } else if (href.contains("md5sums")) {
                    wiki.setMd5sumsUrl(incrementalDumpUrl + href);
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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(this.getClass().getResourceAsStream("wikis.txt"))))) {
            String line;
            // Read each line from the BufferedReader and add it to the list
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                String[] split = line.split(";", 1);
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
            String actualMd5 = calculateMd5Checksum(downloadLocation.getAbsolutePath());
            if (expectedMd5.equals(actualMd5)) {
                System.out.println("Downloaded " + fileUrl + " successfully!");
            } else {
                throw new RuntimeException("MD5 checksum mismatch " + fileUrl + " expected " + expectedMd5 + " got " + actualMd5);
                //TODO automatic retry
            }
            return downloadLocation;
        }
    }

    private void uploadFileToArchive(File file, WikimediaWiki wiki) throws IOException {
        String wikiName = wiki.getName();
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
