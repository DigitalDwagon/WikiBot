package dev.digitaldragon.interfaces.irc;

import com.beust.jcommander.ParameterException;
import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.interfaces.generic.AbortHelper;
import dev.digitaldragon.interfaces.generic.Command;
import dev.digitaldragon.interfaces.generic.ReuploadHelper;
import dev.digitaldragon.interfaces.generic.StatusHelper;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobLaunchException;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperJob;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Job;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperJob;
import dev.digitaldragon.jobs.queues.Queue;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import java.net.URLEncoder;
import java.text.ParseException;
import java.util.*;
import java.util.function.BiFunction;

public class IRCCommandListener {
    private boolean submissionsEnabled = true;

    @Handler
    public void onMessage(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!")) return;

        Channel channel = event.getChannel();
        User user = event.getActor();
        String nick = user.getNick();
        String rawMessage = event.getMessage();
        String[] parts = rawMessage.split(" ", 2);
        if (parts[0].length() < 2) return;
        String command = parts[0].substring(1).trim();
        String message = (parts.length > 1 && !parts[1].isBlank()) ? parts[1].trim() : null;

        Map<String, Runnable> commands = new HashMap<>();
        Map<String, String> aliases = new HashMap<>();
        // !bulk has its own listener for now, so skipped here.

        // --- Non-job specific commands ---
        commands.put("help", () -> {
            channel.sendMessage(nick + ": https://wiki.archiveteam.org/index.php/Wikibot");
        });

        commands.put("check", () -> {
            String reply = URLEncoder.encode(message);
            String url = "https://archive.org/search?query=originalurl%3A%28%2A" + reply + "%2A%29";
            channel.sendMessage(nick + ": " + url);
        });

        commands.put("pause", () -> {
            if (!isOped(event.getChannel(), event.getActor())) {
                channel.sendMessage(nick + ": You don't have permission to do that! Please ask an op.");
                return;
            }

            submissionsEnabled = !submissionsEnabled;
            channel.sendMessage(nick + ": Submissions are now " + (submissionsEnabled ? "enabled." : "disabled."));
        });

        commands.put("setqueue", () -> {
            if (!isOped(event.getChannel(), event.getActor())) {
                channel.sendMessage(nick + ": You don't have permission to do that! Please ask an op.");
                return;
            }
            List<String> args = message != null ? Command.shellSplit(message) : new ArrayList<>();
            if (args.size() != 3) {
                channel.sendMessage(nick + ": Invalid arguments! Usage: !setqueue <concurrency> <priority>");
                return;
            }
            try {
                String queueName = args.get(0);
                int concurrency = Integer.parseInt(args.get(1));
                int priority = Integer.parseInt(args.get(2));
                Queue queue = new Queue(queueName, concurrency, priority);
                WikiBot.getQueueManager().addOrChangeQueue(queue);

                channel.sendMessage(String.format("%s: Set queue %s to max concurrency: %s, priority: %s", nick, queue.getName(), concurrency, priority));
            } catch (Exception e) {
                channel.sendMessage(nick + ": Invalid arguments! Usage: !setqueue <concurrency> <priority>");
            }
        });

        commands.put("savedb", () -> {
            if (!isOped(event.getChannel(), event.getActor())) {
                channel.sendMessage(nick + ": You don't have permission to do that! Please ask an op.");
                return;
            }
            JobManager.getJobs().forEach((job) -> WikiBot.getSqliteManager().saveJob(job));
            channel.sendMessage(nick + ": Done");
        });

        commands.put("getqueue", () -> {
            Queue queue = WikiBot.getQueueManager().getQueue(message);
            if (queue == null) {
               channel.sendMessage(nick + ": Queue does not exist");
               return;
            }

            channel.sendMessage(String.format("%s: Queue %s - max concurrency: %s, priority: %s", nick, message, queue.getConcurrency(), queue.getPriority()));
        });

        commands.put("movejob", () -> {
            try {
                checkUserPermissions(channel, event.getActor(), false);
            } catch (UserErrorException e) {
                channel.sendMessage(nick + ": " + e.getMessage());
                return;
            }
            List<String> args = Command.shellSplit(message);
            if (args.size() != 2) {
                channel.sendMessage(nick + ": Invalid arguments! Usage: !movejob <job id> <queue>");
            }
            String jobId = args.get(0);
            String queue = args.get(1);
            Job job = JobManager.get(jobId);
            if (job == null) {
                channel.sendMessage(nick + ": Couldn't find job " + jobId);
                return;
            }
            job.getMeta().setQueue(queue);
            JobManager.launchJobs();
            channel.sendMessage(String.format("%s: Moved job %s to queue %s", nick, jobId, queue));
        });

        // --- Job-specific commands ---
        commands.put("status", () -> {
            String reply = StatusHelper.getStatus(message);
            channel.sendMessage(nick + ": " + reply);
        });

        commands.put("abort", () -> {
            try {
                checkUserPermissions(channel, event.getActor(), false);
            } catch (UserErrorException e) {
                channel.sendMessage(nick + ": " + e.getMessage());
                return;
            }

            if (message == null) {
                channel.sendMessage(nick + ": Not enough arguments!");
                return;
            }

            String reply = AbortHelper.abortJob(message);
            if (reply != null)
                channel.sendMessage(nick + ": " + reply);
        });

        commands.put("mw", () -> {
            try {
                checkUserPermissions(channel, user, true);
                JobMeta meta = new JobMeta(user.getNick());
                meta.setPlatform(JobMeta.JobPlatform.IRC);

                Job job = null;
                switch (command) {
                    case "mediawikisingle", "mw" -> job = new WikiTeam3Job(message, meta, UUID.randomUUID().toString());
                    case "dokusingle", "dw" -> job = new DokuWikiDumperJob(message, meta, UUID.randomUUID().toString());
                    case "pukisingle", "pw" -> job = new PukiWikiDumperJob(message, meta, UUID.randomUUID().toString());
                }
                assert job != null;

                JobManager.submit(job);
            } catch (UserErrorException | JobLaunchException e) {
                channel.sendMessage(user.getNick() + ": " + e.getMessage());
            } catch (ParseException | ParameterException e) {
                channel.sendMessage(user.getNick() + ": Invalid parameters or options! Hint: make sure that your --explain is in quotes if it has more than one word. (-e \"no coverage\")");
            }
        });
        aliases.put("mediawikisingle", "mw");
        aliases.put("dokusingle", "mw");
        aliases.put("dw", "mw");
        aliases.put("pukisingle", "mw");
        aliases.put("pw", "mw");

        commands.put("reupload", () -> runHelper(channel, user, message, ReuploadHelper::beginJob));

        if (aliases.containsKey(command)) {
            commands.get(aliases.get(command)).run();
        }

        if (commands.get(command) != null) {
            commands.get(command).run();
        }
    }

    public void runHelper(Channel channel, User user, String message, BiFunction<String, String, String> helper) {
        try {
            checkUserPermissions(channel, user, true);
        } catch (UserErrorException e) {
            channel.sendMessage(user.getNick() + ": " + e.getMessage());
        }

        String reply = helper.apply(message, user.getNick());
        if (reply != null) channel.sendMessage(user.getNick() + ": " + reply);
    }

    private void checkUserPermissions(Channel channel, User user, boolean shouldPause) throws UserErrorException {
        if (!isVoiced(channel, user)/* && !Boolean.parseBoolean(EnvConfig.getConfigs().get("is_test"))*/) {
            throw new UserErrorException("You don't have permission to do that! Please ask someone else to run this wiki for you.");
        }

        if (!submissionsEnabled && shouldPause) {
            if (isOped(channel, user)) {
                channel.sendMessage(user.getNick() + ": WARN - submissions are paused for a pending update. Please abort this job and try again later if it is non-urgent.");
            } else {
                throw new UserErrorException("Submissions are paused for a pending update. Please try again later.");
            }
        }
    }

    public static boolean isVoiced(Channel channel, User user) {
        Optional<SortedSet<ChannelUserMode>> modes = channel.getUserModes(user);
        if (modes.isPresent()) {
            for (ChannelUserMode mode : modes.get()) {
                return mode.getNickPrefix() == '@' || mode.getNickPrefix() == '+';
            }
        }
        return false;
    }

    public static boolean isOped(Channel channel, User user) {
        Optional<SortedSet<ChannelUserMode>> modes = channel.getUserModes(user);
        if (modes.isPresent()) {
            for (ChannelUserMode mode : modes.get()) {
                return mode.getNickPrefix() == '@';
            }
        }
        return false;
    }
}