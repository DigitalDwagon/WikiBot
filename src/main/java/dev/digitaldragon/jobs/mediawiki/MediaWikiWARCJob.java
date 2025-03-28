package dev.digitaldragon.jobs.mediawiki;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.time.Instant;
import java.util.List;

@Getter
public class MediaWikiWARCJob extends Job {
    @Setter private String logsUrl;
    @Setter private String archiveUrl = "disabled";

    private String WARC_BASE = "wikibot_go_";

    private JobStatus status = JobStatus.QUEUED;
    private Instant startTime = null;
    private int failedTaskCode = 0;
    private File directory;

    private String id;
    private String name;
    private String url;
    private JobMeta meta;

    public MediaWikiWARCJob(String id, String url, JobMeta meta) {
        this.id = id;
        this.meta = meta;
        this.name = url;
        this.url = url;
        this.directory = new File("jobs/" + id);
        directory.mkdirs();
    }

    @Override
    public void run() {
        startTime = Instant.now();
        log("wikibot v" + WikiBot.getVersion() + " job " + id);
        //log("WARC archival of wikis is experimental. These jobs run silently after normal wikiteam jobs.");
        String warcFilename = WARC_BASE + Instant.now().getEpochSecond() + "_" + id; // wikibot_go_1723841562_20bf797d-8f8a-47da-9b2a-c182a95287a8
        String[] args = new String[]{
                "docker", "run", "--rm",
                "-v", directory.getAbsolutePath() + ":/grab/wikibot",
                "--entrypoint", "/usr/local/bin/wget-lua",
                "atdr.meo.ws/archiveteam/grab-base@sha256:50245bd61b56b05f481546866d2e93d1d5bd4afb56644c9889120d3741476de7", //20240513.01
                "--user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36 (compatible; " +  "WikiBot/" + WikiBot.getVersion() + " - Wikibot preserves public wikis; wikibot@digitaldragon.dev; +https://wikibot.digitaldragon.dev)",
                "--verbose",
                "--host-lookups", "dns",
                "--hosts-file", "/dev/null",
                "--resolvconf-file", "resolv.conf",
                "--dns-servers", "9.9.9.10,149.112.112.10,2620:fe::10,2620:fe::fe:10",
                "--reject-reserved-subnets",
                "--content-on-error",
                "--output-file", "wikibot/wget.log",
                "--output-document", "wikibot/wget.tmp",
                "--truncate-output",
                "-e", "robots=off",
                "--recursive",
                "--level=inf",
                "--no-parent",
                "--timeout", "10",
                "--tries", "999", //limits the number of times you can wget.actions.CONTINUE in lua
                "--span-hosts",
                "--page-requisites",
                "--waitretry", "0",
                "--warc-file", "wikibot/" + warcFilename,
                "--warc-header", "operator: DigitalDragon <warc@digitaldragon.dev>",
                "--warc-header", "x-wikibot-version: " + WikiBot.getVersion(),
                "--warc-header", "x-wikibot-job-id: " + id,
                "--warc-dedup-url-agnostic",
                "--warc-compression-use-zstd",
                //"--warc-zstd-dict-no-include", TODO: make and use a dictionary
                "--header", "Connection: keep-alive",
                "--header", "Accept-Language: en-US;q=0.9, en;q=0.8",
                "--lua-script", "wikibot/mediawiki.lua",
                url
        };
        writeResourceToJobDirectory("mediawiki.lua", "mediawiki.lua");
        writeResourceToJobDirectory("JSON.lua", "JSON.lua");
        writeResourceToJobDirectory("table_show.lua", "table_show.lua");
        RunCommand wget = new RunCommand(null, args, directory, this::log);
        wget.run();
        int wgetExitCode = wget.waitFor();
        if (wgetExitCode == 0) {
            log("wget completed successfully!");
            log("WARC file: " + warcFilename + ".warc.zst");
            status = JobStatus.COMPLETED;
            logsUrl = WikiBot.getLogFiles().uploadLogs(this);
            WikiBot.getBus().post(new JobSuccessEvent(this));
        } else {
            log("wget failed with exit code " + wgetExitCode);
            log("WARC file: " + warcFilename + ".warc.zst");
            status = JobStatus.FAILED;
            logsUrl = WikiBot.getLogFiles().uploadLogs(this);
            WikiBot.getBus().post(new JobFailureEvent(this));
        }
    }

    private void writeResourceToJobDirectory(String resource, String destinationName) {
        try (InputStream stream = MediaWikiWARCJob.class.getClassLoader().getResourceAsStream(resource)) {
            //write stream to file
            if (stream == null) {
                throw new RuntimeException("Couldn't find resource: " + resource);
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(directory, destinationName)))) {
                writer.write(new String(stream.readAllBytes()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean abort() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return status == JobStatus.RUNNING;
    }

    @Override
    public JobType getType() {
        return JobType.MEDIAWIKIWARC;
    }

    @Override
    public String getRunningTask() {
        return "Wget-AT";
    }

    @Override
    public List<String> getAllTasks() {
        return List.of("Wget-AT");
    }
}
