package dev.digitaldragon.db;

import com.google.gson.Gson;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperArgs;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperJob;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
import dev.digitaldragon.jobs.events.JobCompletedEvent;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Args;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Job;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperArgs;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperJob;
import net.badbird5907.lightning.annotation.EventHandler;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SqliteManager {
    private static final Object lock = new Object();


    public void saveJob(Job job) {
        if (job instanceof LoadedJob) {
            return;
        }

        synchronized (lock) {
            try {
                Connection connection = DriverManager.getConnection("jdbc:sqlite:jobs.sqlite3");
                // string id, string url, jobtype type, string explain, string user, JobPlatform platform, discorduserid
                Statement statement = connection.createStatement();
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS jobs (id TEXT PRIMARY KEY, url TEXT, type TEXT, explain TEXT, user TEXT, platform TEXT, discorduserid TEXT, args TEXT, status TEXT, startTime TEXT, archiveUrl TEXT, logsUrl TEXT, failedTaskCode INTEGER, runningTask TEXT)");
                //save job via preparedstatement
                PreparedStatement preparedStatement = connection.prepareStatement("INSERT OR REPLACE INTO jobs (id, url, type, explain, user, platform, discorduserid, args, status, startTime, archiveUrl, logsUrl, failedTaskCode, runningTask) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                preparedStatement.setString(1, job.getId());
                preparedStatement.setString(2, job.getMeta().getTargetUrl().orElse(""));
                preparedStatement.setString(3, job.getType().name());
                preparedStatement.setString(4, job.getMeta().getExplain().orElse(""));
                preparedStatement.setString(5, job.getMeta().getUserName());
                preparedStatement.setString(6, job.getMeta().getPlatform() == null ? "" : job.getMeta().getPlatform().name());
                preparedStatement.setString(7, job.getMeta().getDiscordUserId().orElse(""));

                String args = "";
                if (job instanceof PukiWikiDumperJob) {
                    args = new Gson().toJson(((PukiWikiDumperJob) job).getArgs());
                }
                if (job instanceof DokuWikiDumperJob) {
                    args = new Gson().toJson(((DokuWikiDumperJob) job).getArgs());
                }
                if (job instanceof WikiTeam3Job) {
                    args = new Gson().toJson(((WikiTeam3Job) job).getArgs());
                }
                preparedStatement.setString(8, args);

                preparedStatement.setString(9, job.getStatus().name());
                preparedStatement.setString(10, job.getStartTime() == null ? "" : job.getStartTime().toString());
                preparedStatement.setString(11, job.getArchiveUrl());
                preparedStatement.setString(12, job.getLogsUrl());
                preparedStatement.setInt(13, job.getFailedTaskCode());
                preparedStatement.setString(14, job.getRunningTask());



                preparedStatement.executeUpdate();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onJobCompleted(JobCompletedEvent event) {
        saveJob(event.getJob());
    }

    @EventHandler
    public void onJobAbort(JobAbortEvent event) {
        saveJob(event.getJob());
    }

    @EventHandler
    public void onJobFailure(JobFailureEvent event) {
        saveJob(event.getJob());
    }

    @EventHandler
    public void onJobQueued(JobQueuedEvent event) {
        saveJob(event.getJob());
    }

    public LoadedJob getJobInfo(String id) {
        synchronized (lock) {
            try {
                Connection connection = DriverManager.getConnection("jdbc:sqlite:jobs.sqlite3");
                Statement statement = connection.createStatement();
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM jobs WHERE id = ?");
                preparedStatement.setString(1, id);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    String url = resultSet.getString("url");
                    String type = resultSet.getString("type");
                    String explain = resultSet.getString("explain");
                    String user = resultSet.getString("user");
                    String platform = resultSet.getString("platform");
                    String discorduserid = resultSet.getString("discorduserid");
                    String status = resultSet.getString("status");
                    String startTime = resultSet.getString("startTime");
                    String archiveUrl = resultSet.getString("archiveUrl");
                    String logsUrl = resultSet.getString("logsUrl");
                    int failedTaskCode = resultSet.getInt("failedTaskCode");
                    String runningTask = resultSet.getString("runningTask");
                    LoadedJob job = new LoadedJob();
                    job.setId(id);
                    job.getMeta().setUserName(user);
                    job.getMeta().setTargetUrl(url);
                    if (!platform.isEmpty()) job.getMeta().setPlatform(JobMeta.JobPlatform.valueOf(platform));
                    job.getMeta().setDiscordUserId(discorduserid);
                    job.setStatus(JobStatus.valueOf(status));
                    job.setType(JobType.valueOf(type));
                    job.getMeta().setExplain(explain);
                    if (startTime != null && !startTime.isEmpty()) job.setStartTime(Instant.parse(startTime));
                    job.setArchiveUrl(archiveUrl);
                    job.setLogsUrl(logsUrl);
                    job.setFailedTaskCode(failedTaskCode);
                    job.setRunningTask(runningTask);
                    connection.close();
                    return job;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Object getArgs(String id) {
        synchronized (lock) {
            try {
                Connection connection = DriverManager.getConnection("jdbc:sqlite:jobs.sqlite3");
                Statement statement = connection.createStatement();
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT args FROM jobs WHERE id = ?");
                preparedStatement.setString(1, id);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    String args = resultSet.getString("args");
                    String type = getJobInfo(id).getType().name();
                    connection.close();
                    JobType jobType = JobType.valueOf(type);
                    if (jobType == JobType.PUKIWIKIDUMPER) {
                        return new Gson().fromJson(args, PukiWikiDumperArgs.class);
                    }
                    if (jobType == JobType.DOKUWIKIDUMPER) {
                        return new Gson().fromJson(args, DokuWikiDumperArgs.class);
                    }
                    if (jobType == JobType.WIKITEAM3) {
                        return new Gson().fromJson(args, WikiTeam3Args.class);
                    }
                    return args;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static void setFailed(String jobId) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:jobs.sqlite3");
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE jobs SET status = ? WHERE id = ?");
            preparedStatement.setString(1, JobStatus.FAILED.name());
            preparedStatement.setString(2, jobId);
            preparedStatement.executeUpdate();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        synchronized (lock) {
            try {
                Connection connection = DriverManager.getConnection("jdbc:sqlite:jobs.sqlite3");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM jobs");
                List<String> jobIds = new ArrayList<>();
                while (resultSet.next()) {
                    jobIds.add(resultSet.getString("id"));
                }
                resultSet.close();
                connection.close();

                for (String jobId : jobIds) {
                    System.out.printf("Loading job %s%n", jobId);
                    LoadedJob job = getJobInfo(jobId);
                    System.out.printf("Loaded job %s%n", job.getId());

                    if (job.getStatus() == JobStatus.QUEUED) {
                        File dir = new File("jobs/" + job.getId() + "/");

                        if (job.getType() == JobType.WIKITEAM3) {
                            //IRCClient.sendMessage(String.format("Resuming job %s that was running when the bot was last shut down", job.getId()));
                            WikiTeam3Args args = (WikiTeam3Args) getArgs(job.getId());
                            if (dir.exists() && CommonTasks.findDumpDir(job.getId()) != null && args.getResume() == null) {
                                args.setResume(job.getId());
                            }
                            setFailed(job.getId());
                            if (job.getMeta().getSilentMode() == null) {
                                job.getMeta().setSilentMode(JobMeta.SilentMode.END);
                            }
                            JobManager.submit(new WikiTeam3Job(
                                    args,
                                    job.getMeta(),
                                    job.getId()
                            ));

                        } else if (job.getType() == JobType.PUKIWIKIDUMPER) {
                            //IRCClient.sendMessage(String.format("Resuming job %s that was running when the bot was last shut down", job.getId()));
                            PukiWikiDumperArgs args = (PukiWikiDumperArgs) getArgs(job.getId());
                            if (dir.exists() && CommonTasks.findDumpDir(job.getId()) != null && args.getResume() == null) {
                                args.setResume(job.getId());
                            }
                            if (job.getMeta().getSilentMode() == null) {
                                job.getMeta().setSilentMode(JobMeta.SilentMode.END);
                            }
                            setFailed(job.getId());
                            JobManager.submit(new PukiWikiDumperJob(args, job.getMeta(), job.getId()));
                        } else if (job.getType() == JobType.DOKUWIKIDUMPER) {
                            //IRCClient.sendMessage(String.format("Resuming job %s that was running when the bot was last shut down", job.getId()));
                            DokuWikiDumperArgs args = (DokuWikiDumperArgs) getArgs(job.getId());
                            if (dir.exists() && CommonTasks.findDumpDir(job.getId()) != null && args.getResume() == null) {
                                args.setResume(job.getId());
                            }
                            if (job.getMeta().getSilentMode() == null) {
                                job.getMeta().setSilentMode(JobMeta.SilentMode.END);
                            }
                            setFailed(job.getId());
                            JobManager.submit(new DokuWikiDumperJob(args, job.getMeta(), job.getId()));
                        } else {
                            //IRCClient.sendMessage("DigitalDragons: " + job.getId() + " died.");
                        }


                    } else {
                        JobManager.submitJobDbOnly(job);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
