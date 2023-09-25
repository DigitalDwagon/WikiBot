package dev.digitaldragon.interfaces.irc;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.DokuWikiDumperPlugin;
import dev.digitaldragon.archive.Uploader;
import dev.digitaldragon.archive.WikiTeam3Plugin;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.interfaces.generic.DokuWikiDumperHelper;
import dev.digitaldragon.interfaces.generic.WikiTeam3Helper;
import dev.digitaldragon.jobs.*;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperJob;
import dev.digitaldragon.jobs.wikiteam.WikiTeam3Args;
import dev.digitaldragon.jobs.wikiteam.WikiTeam3Job;
import dev.digitaldragon.parser.CommandLineParser;
import dev.digitaldragon.util.BulkArchiveParser;
import dev.digitaldragon.util.EnvConfig;
import net.dv8tion.jda.api.entities.TextChannel;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class IrcCommandListener {
    @Handler
    public void message(ChannelMessageEvent event) {
        String[] commands = new String[]{
                "dw",
                "dokusingle",
                "dokubulk",
                "mw",
                "mediawikisingle",
                "mediawikibulk",
                "reupload"
        };
        //return if event does not start with one of the above commands
        boolean startsWithCommand = false;
        String prefix = "!";
        for (String command : commands) {
            if (event.getMessage().startsWith(prefix + command + " ")) {
                startsWithCommand = true;
                System.out.println(true);
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
            discordChannel = WikiBot.getLogsChannelSafely();
            checkUserPermissions(channel, event.getActor());
        } catch (UserErrorException e) {
            channel.sendMessage(nick + ": " + e.getMessage());
            return;
        }

        try {
            handleDokuCommands(event, nick, opts);
            handleMediaWikiCommands(event, nick, opts);
            handleReuploadCommands(event, channel, discordChannel, nick, opts);
        } catch (UserErrorException exception) {
            channel.sendMessage(nick + ": " + exception.getMessage());
            return;
        }

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

    private void handleDokuCommands(ChannelMessageEvent event, String nick, String opts) throws UserErrorException {
        if (!event.getMessage().startsWith("!doku") && !event.getMessage().startsWith("!dw"))
            return;

        String message = DokuWikiDumperHelper.beginJob(opts, nick);
        if (message != null)
            event.getChannel().sendMessage(nick + ": " + message);
    }

    private void handleMediaWikiCommands(ChannelMessageEvent event, String nick, String opts) throws UserErrorException {
        if (!event.getMessage().startsWith("!mediawiki") && !event.getMessage().startsWith("!mw"))
            return;

        String message = WikiTeam3Helper.beginJob(opts, nick);
        if (message != null)
            event.getChannel().sendMessage(nick + ": " + message);

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
        String url = "https://archive.org/search?query=originalurl%3A%28%2A" + opts + "%2A%29";
        channel.sendMessage(nick + ": " + url);
    }

    @Handler
    public void testParserCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!testparser"))
            return;

        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();

        String[] parts = event.getMessage().split(" ", 2);
        if (parts.length < 2) {
            channel.sendMessage(nick + ": Not enough arguments!");
            return;
        }

        String opts = parts[1];
        WikiTeam3Args args = new WikiTeam3Args();

        //split opts on spaces, except when the spaces are in ""

        try {
            JCommander.newBuilder()
                    .addObject(args)
                    .build()
                    .parse(opts.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)"));
            args.check();
        } catch (UserErrorException e) {
            channel.sendMessage(nick + ": " + e.getMessage());
            return;
        } catch (ParameterException e) {
            channel.sendMessage(nick + ": Invalid parameters or options!");
            return;
        }
        channel.sendMessage(nick + ": " + args.get());
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