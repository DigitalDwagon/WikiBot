package dev.digitaldragon.jobs.wikimedia;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.JobStatus;
import dev.digitaldragon.jobs.JobType;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
public class WikimediaIncrementalJob extends Job {
    private String id;
    private final String name = "Wikimedia Incremental Dump";
    private final String userName = "AutomaticDragon";
    @Setter
    private JobStatus status = JobStatus.QUEUED;
    private final String runningTask = "Run";
    private Instant startTime = null;
    private File directory;
    private final String explanation = "This job is automatically created by the bot to download the Wikimedia incremental dumps every day.";
    @Setter
    private String archiveUrl = "https://archive.org/details/@DigitalDragons";
    @Setter
    private String logsUrl = null;
    private int failedTaskCode = 0;
    private boolean aborted = false;
    private JobMeta meta;
    private final JobType jobType = JobType.WIKIMEDIADUMPINCR;
    private List<String> allTasks = List.of("Run");

    public WikimediaIncrementalJob(String id) {
        this.id = id;
        this.directory = new File("jobs/" + id);
        directory.mkdirs();
    }
/*
The Plan:
- Get the primary dumps page.
- Get all of the wiki ids from the <strong> tags
- Get all of the individual dump directory pages (https://dumps.wikimedia.org/other/incr/zhwikinews/)
- For all of the subdirectories of this page:
	- Check SQLite to see if they have already been updated (if SQLite doesn't know, check archive.org for the identifier)
	- If they haven't, write to SQLite job queue in this format: wiki / date / links
- For all of the items in SQLite that need to be done:
	- For each link, download and then upload to archive.org
	- Mark done in SQLite
 */


    public void run() {
        findWikis(wiki -> {
            System.out.printf("Wiki %s%n", wiki);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            findDumpsForWiki(wiki, (wikiId, date) -> {
                System.out.printf("Dump for %s on %s%n", wikiId, date);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                saveDumpInfo(wikiId, date);
            });
        });

    }

    private void findWikis(Consumer<String> forEach) {
        String dumpsUrl = "https://dumps.wikimedia.org/other/incr/";
        //get page html as jsoup doc
        try {
            Document document = Jsoup.connect(dumpsUrl).get();
            document.select("strong").forEach(element -> {
                String wikiId = element.text();
                if (wikiId.contains(" ")) return;
                forEach.accept(wikiId);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void findDumpsForWiki(String wikiId, BiConsumer<String, String> forEach) {
        String dumpsUrl = "https://dumps.wikimedia.org/other/incr/" + wikiId + "/";
        //get page html as jsoup doc
        try {
            Document document = Jsoup.connect(dumpsUrl).get();
            document.select("a").forEach(element -> {
                String dumpUrl = element.attr("href");
                if (!dumpUrl.endsWith("/") || dumpUrl.equals("../")) return;
                dumpUrl = dumpUrl.substring(0, dumpUrl.length() - 1);
                forEach.accept(wikiId, dumpUrl);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDumpInfo(String wikiId, String date) {
        String dumpUrl = "https://dumps.wikimedia.org/other/incr/" + wikiId + "/" + date + "/";
        List<String> links = new ArrayList<>();
        //get page html as jsoup doc
        try {
            Document document = Jsoup.connect(dumpUrl).get();
            document.select("a").forEach(element -> {
                String link = element.attr("href");
                if (link.endsWith("/")) return;
                links.add(link);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        //save to sqlite
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:wikimedia-incremental.sqlite");
            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS jobs (done BOOLEAN, wiki TEXT, date TEXT, links TEXT)");
            PreparedStatement statement = connection.prepareStatement("INSERT INTO jobs (done, wiki, date, links) VALUES (?, ?, ?, ?)");
            statement.setBoolean(1, false);
            statement.setString(2, wikiId);
            statement.setString(3, date);
            statement.setString(4, String.join(" ", links));
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean dumpExists(String wikiId, String date) {
    try {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:wikimedia-incremental.sqlite");
        PreparedStatement statement = connection.prepareStatement("SELECT * FROM jobs WHERE wiki = ? AND date = ?");
        statement.setString(1, wikiId);
        statement.setString(2, date);
        ResultSet resultSet = statement.executeQuery();

        if (resultSet.next()) {
            return resultSet.getBoolean("done");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return false;
}

    @Override
    public boolean abort() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public JobType getType() {
        return null;
    }
}
