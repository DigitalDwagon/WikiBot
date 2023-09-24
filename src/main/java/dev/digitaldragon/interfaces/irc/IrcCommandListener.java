package dev.digitaldragon.interfaces.irc;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.DokuWikiDumperPlugin;
import dev.digitaldragon.archive.Uploader;
import dev.digitaldragon.archive.WikiTeam3Plugin;
import dev.digitaldragon.backfeed.LinkExtract;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.parser.CommandLineParser;
import dev.digitaldragon.util.BulkArchiveParser;
import dev.digitaldragon.util.EnvConfig;
import dev.digitaldragon.util.TransferUploader;
import net.dv8tion.jda.api.entities.TextChannel;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import java.io.*;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class IrcCommandListener {
    @Handler
    public void message(ChannelMessageEvent event) {
        String[] commands = new String[]{
                "dw",
                "dokusingle ",
                "dokubulk ",
                "mw ",
                "mediawikisingle ",
                "mediawikibulk ",
                "reupload "
        };
        //return if event does not start with one of the above commands
        boolean startsWithCommand = false;
        String prefix = "!";
        for (String command : commands) {
            if (event.getMessage().startsWith(prefix + command + " ")) {
                startsWithCommand = true;
                break;
            }
        }
        if (!startsWithCommand)
            return;

        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();

        String[] parts = event.getMessage().split(" ", 2);
        if (parts.length < 2) {
            channel.sendMessage(nick + ": Not enough arguments!");
            return;
        }
        String opts = parts[1];

        TextChannel discordChannel;
        try {
            discordChannel = getLogsChannel();
            checkUserPermissions(channel, event.getActor());
        } catch (UserErrorException e) {
            channel.sendMessage(nick + ": " + e.getMessage());
            return;
        }

        handleDokuCommands(event, channel, discordChannel, nick, opts);
        handleMediaWikiCommands(event, channel, discordChannel, nick, opts);
        handleReuploadCommands(event, channel, discordChannel, nick, opts);
    }

    private void checkUserPermissions(Channel channel, User user) throws UserErrorException {
        if (!isVoiced(channel, user) && !Boolean.parseBoolean(EnvConfig.getConfigs().get("is_test"))) {
            throw new UserErrorException("Requires (@) or (+).");
        }

        if (Boolean.parseBoolean(EnvConfig.getConfigs().get("pause_submissions"))) {
            if (isOped(channel, user)) {
                channel.sendMessage(user.getNick() + ": WARN - submissions are paused for a pending update. Please abort this job and try again later if it is non-urgent.");
            } else {
                throw new UserErrorException("Submissions are paused for a pending update. Please try again later.");
            }
        }
    }

    private TextChannel getLogsChannel() throws UserErrorException {
        TextChannel discordChannel = WikiBot.getLogsChannel();
        if (discordChannel == null) {
            throw new UserErrorException("Something went wrong.");
        }
        return discordChannel;
    }

    private void handleDokuCommands(ChannelMessageEvent event, Channel channel, TextChannel discordChannel, String nick, String opts) { //todo this method is long and kind of messy
        if (!event.getMessage().startsWith("!doku") && !event.getMessage().startsWith("!dw"))
            return;

        CommandLineParser parser = DokuWikiDumperPlugin.getCommandLineParser();
        parser.addBooleanOption("old-backend");
        try {
            parser.parse(opts.split(" "));
        } catch (IllegalArgumentException e) {
            channel.sendMessage(nick + ": " + e.getMessage());
            return;
        }

        if (parser.getOption("url") == null) {
            channel.sendMessage(nick + ": URL is required! Note: A new bot update now requires URLs in the form of an option, eg \"--url https://wikipedia.org\"");
            return;
        }
        String url = parser.getOption("url").toString();

        if (event.getMessage().startsWith("!dokusingle ") || event.getMessage().startsWith("!dw ")) {
            if (parser.getOption("explain") == null) {
                channel.sendMessage(nick + ": Explanation is required! Note: A new bot update now requires explanations in the form of an option, eg \"--explain Closing soon\"");
                return;
            }
            String explain = parser.getOption("explain").toString();


            if (parser.getOption("old-backend") == Boolean.TRUE) {
                DokuWikiDumperPlugin.startJob(discordChannel, url, explain, nick, nick, DokuWikiDumperPlugin.parserToOptions(parser));
            } else {
                Job job = new DokuWikiDumperJob(nick, UUID.randomUUID().toString(), url, DokuWikiDumperPlugin.parserToOptions(parser), explain);
                JobManager.submit(job);
            }
        }
        if (event.getMessage().startsWith("!dokubulk ")) {
            String options = DokuWikiDumperPlugin.parserToOptions(parser);

            Map<String, String> tasks;
            try {
                tasks = BulkArchiveParser.parse(url);
            } catch (Exception e) {
                channel.sendMessage(nick + ": " + e.getMessage());
                return;
            }
            for (Map.Entry<String, String> entry : tasks.entrySet()) {
                String jobUrl = entry.getKey();
                String note = entry.getValue();

                if (parser.getOption("old-backend") == Boolean.TRUE) {
                    DokuWikiDumperPlugin.startJob(discordChannel, jobUrl, note, nick, nick, options);
                } else {
                    Job job = new DokuWikiDumperJob(nick, UUID.randomUUID().toString(), jobUrl, WikiTeam3Plugin.parserToOptions(parser), note);
                    JobManager.submit(job);
                }
            }
            channel.sendMessage(nick + ": Launched " + tasks.size() + " jobs!");
        }
    }

    private void handleMediaWikiCommands(ChannelMessageEvent event, Channel channel, TextChannel discordChannel, String nick, String opts) { //todo this method is long and kind of messy
        if (!event.getMessage().startsWith("!mediawiki") && !event.getMessage().startsWith("!mw"))
            return;

        CommandLineParser parser = WikiTeam3Plugin.getCommandLineParser();
        parser.addBooleanOption("old-backend");
        try {
            parser.parse(opts.split(" "));
        } catch (IllegalArgumentException e) {
            channel.sendMessage(nick + ": " + e.getMessage());
            return;
        }

        if (parser.getOption("url") == null && parser.getOption("api") == null && parser.getOption("index") == null) {
            channel.sendMessage(nick + ": You need to specify --url, --api, or --index! Note: A new bot update now requires URLs in the form of an option, eg \"--url https://wikipedia.org\"");
            return;
        }

        String jobName = parser.getOption("url") == null ? null : parser.getOption("url").toString();
        if (jobName == null)
            jobName = parser.getOption("api") == null ? null : parser.getOption("api").toString();
        if (jobName == null)
            jobName = parser.getOption("index") == null ? null : parser.getOption("index").toString();


        if (event.getMessage().startsWith("!mediawikisingle ") || event.getMessage().startsWith("!mw ")) {
            if (parser.getOption("explain") == null) {
                channel.sendMessage(nick + ": Explanation is required! Note: A new bot update now requires explanations in the form of an option, eg \"--explain Closing soon\"");
                return;
            }
            String explain = parser.getOption("explain").toString();

            if (parser.getOption("old-backend") == Boolean.TRUE) {
                WikiTeam3Plugin.startJob(discordChannel, explain, nick, nick, WikiTeam3Plugin.parserToOptions(parser));
            } else {
                Job job = new WikiTeam3Job(nick, UUID.randomUUID().toString(), jobName, WikiTeam3Plugin.parserToOptions(parser), explain);
                JobManager.submit(job);
            }
        }
    }

    private void handleReuploadCommands(ChannelMessageEvent event, Channel channel, TextChannel discordChannel, String nick, String opts) {
        if (!event.getMessage().startsWith("!reupload"))
            return;

        boolean oldbackend = false;
        if (opts.endsWith(" oldbackend")) {
            oldbackend = true;
            opts = opts.replace(" oldbackend", "");
        }
        if (opts.contains(" ")) {
            channel.sendMessage(nick + ": Too many arguments!");
            return;
        }
        if (oldbackend) {
            Uploader.reupload(opts, nick, nick, discordChannel);
        } else {
            Job job = new ReuploadJob(nick, UUID.randomUUID().toString(), opts);
            JobManager.submit(job);
        }
    }

    @Handler
    public void abortCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!abort"))
            return;

        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();
        try {
            checkUserPermissions(channel, event.getActor());
        } catch (UserErrorException e) {
            channel.sendMessage(nick + ": " + e.getMessage());
            return;
        }

        if (event.getMessage().split(" ").length < 2) {
            channel.sendMessage(nick + ": Not enough arguments!");
            return;
        }
        String jobId = event.getMessage().split(" ")[1];
        if (JobManager.abort(jobId)) {
            channel.sendMessage(nick + ": Aborted job " + jobId + "!");
        } else {
            channel.sendMessage(nick + ": Failed to abort job " + jobId + "! It might not exist, be in a task that can't be aborted, or have already finished.");
        }
    }

    @Handler
    public void statusCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!status"))
            return;

        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();
        if (event.getMessage().split(" ").length < 2) {
            channel.sendMessage(nick + ": " + JobManager.getActiveJobs().size() + " running jobs. " + JobManager.getQueuedJobs().size() + " jobs waiting to run.");
            return;
        }
        String jobId = event.getMessage().split(" ")[1];
        Job job = JobManager.get(jobId);
        if (job == null) {
            channel.sendMessage(nick + ": Job " + jobId + " does not exist!");
            return;
        }
        StringBuilder message = new StringBuilder();
        message.append(nick).append(": Job ").append(jobId).append(" (").append(job.getType()).append(")").append(" is ");
        if (job.isRunning()) {
            message.append("running");
        } else {
            message.append("not running");
        }

        if (job.getRunningTask() != null) {
            message.append(" (task ").append(job.getRunningTask()).append("). ");
        } else {
            message.append(". ");
        }

        message.append("Status: ");
        message.append(job.getStatus().toString());
        message.append(". Started: ");
        message.append(Duration.between(job.getStartTime(), Instant.now()).toSeconds()).append(" seconds ago. ");
        message.append("\"").append(job.getExplanation()).append("\"");


        channel.sendMessage(message.toString());
    }


    @Handler
    public void helpCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!help"))
            return;
        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();
        channel.sendMessage(nick + ": https://cdn.digitaldragon.dev/wikibot/help.html");
    }

    @Handler
    public void checkCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!check"))
            return;
        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();

        String[] parts = event.getMessage().split(" ", 2);
        if (parts.length < 2) {
            channel.sendMessage(nick + ": Not enough arguments!");
            return;
        }
        String opts = parts[1];
        opts = URLEncoder.encode(opts);
        String url = "https://archive.org/search?query=originalurl%3A%28*" + opts + "*%29";
        channel.sendMessage(nick + ": " + url);
    }

    private boolean isVoiced(Channel channel, User user) {
        Optional<SortedSet<ChannelUserMode>> modes = channel.getUserModes(user);
        if (modes.isPresent()) {
            for (ChannelUserMode mode : modes.get()) {
                return mode.getNickPrefix() == '@' || mode.getNickPrefix() == '+';
            }
        }
        return false;
    }

    private boolean isOped(Channel channel, User user) {
        Optional<SortedSet<ChannelUserMode>> modes = channel.getUserModes(user);
        if (modes.isPresent()) {
            for (ChannelUserMode mode : modes.get()) {
                return mode.getNickPrefix() == '@';
            }
        }
        return false;
    }
}
