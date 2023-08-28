package dev.digitaldragon.commands;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.DokuWikiDumperPlugin;
import dev.digitaldragon.archive.Uploader;
import dev.digitaldragon.archive.WikiTeam3Plugin;
import dev.digitaldragon.backfeed.LinkExtract;
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
        if (!event.getMessage().startsWith("!doku") && !event.getMessage().startsWith("!mediawiki") && !event.getMessage().startsWith("!reupload"))
            return;
        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();

        TextChannel discordChannel = WikiBot.getLogsChannel();
        if (discordChannel == null) {
            channel.sendMessage(nick + ": Something went wrong.");
        }

        if (!isVoiced(channel, event.getActor()) && !Boolean.parseBoolean(EnvConfig.getConfigs().get("is_test"))) {
            event.getChannel().sendMessage(event.getActor().getNick() + ": Requires (@) or (+).");
            return;
        }

        String[] parts = event.getMessage().split(" ", 2);
        if (parts.length < 2) {
            channel.sendMessage(nick + ": Not enough arguments!");
            return;
        }
        String opts = parts[1];

        if (event.getMessage().startsWith("!doku")) {
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

            if (event.getMessage().startsWith("!dokusingle ")) {
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

        if (event.getMessage().startsWith("!mediawiki")) {
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


            if (event.getMessage().startsWith("!mediawikisingle ")) {
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
            /*if (event.getMessage().startsWith("!mediawikibulk ")) {
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

                    //WikiTeam3Plugin.startJob(discordChannel, jobUrl, note, nick, nick, WikiTeam3Plugin.parseCommandLineOptions(opts));
                }
                channel.sendMessage(nick + ": Launched " + tasks.size() + " jobs!");
            }*/
        }

        if (event.getMessage().startsWith("!reupload")) {
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
    }

    @Handler
    public void abortCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!abort"))
            return;

        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();
        if (!isVoiced(channel, event.getActor()) && !Boolean.parseBoolean(EnvConfig.getConfigs().get("is_test"))) {
            event.getChannel().sendMessage(event.getActor().getNick() + ": Requires (@) or (+).");
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
            channel.sendMessage(nick + ": Not enough arguments!");
            return;
        }
        String jobId = event.getMessage().split(" ")[1];
        Job job = JobManager.get(jobId);
        if (job == null) {
            channel.sendMessage(nick + ": Job " + jobId + " does not exist!");
            return;
        }
        StringBuilder message = new StringBuilder();
        message.append(nick).append(": Job").append(jobId).append(" (").append(job.getType()).append(")").append(" is ");
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
        channel.sendMessage(nick + ": Bot operator is DigitalDragons.");
        channel.sendMessage(nick + ": !dokusingle <--options> - Archive a DokuWiki with DokuWikiDumper. --explain <your explanation> and --url <target DokuWiki URL) are required.");
        channel.sendMessage(nick + ": !dokubulk <--options> - Archive DokuWikis in bulk via a text file specified with --url <file URL>. Each line should be a URL followed by an explanation, separated by a space. Explanation optional, but highly encouraged.");
        channel.sendMessage(nick + ": Supported DokuWikiDumper options are: --retry --ignore-disposition-header-missing --hard-retry --delay --threads --ignore-action-disabled-edit --insecure --content --media --html --pdf --auto --current-only");
        channel.sendMessage(nick + ": !mediawikisingle <--options> - Archive a MediaWiki with WikiTeam3. --explain <your explanation> and --url <target DokuWiki URL) are required.");
        //channel.sendMessage(nick + ": !mediawikibulk <--options> - The same as dokubulk, but using WikiTeam3 tools."); currently disabled on IRC side.
        channel.sendMessage(nick + ": Supported WikiTeam3 options are: --delay --retries --api_chunksize --xml --images --bypass-cdn-image-compression --xmlapiexport --xmlrevisions --curonly --api --index --url");
        channel.sendMessage(nick + ": !reupload <job ID> - Reupload a job that failed to upload the first time.");
        channel.sendMessage(nick + ": !check <search> - Generate an Internet Archive search link for a given string. Checks the originalurl field.");
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

    @Handler
    public void testParser(ChannelMessageEvent event) throws FileNotFoundException {
        if (!event.getMessage().startsWith("!testparser"))
            return;

        File file = new File("test.xml");
        Set<String> urls = LinkExtract.extractLinksFromFile(new FileInputStream(file));

        //write to file
        File write = new File("outlinks_test.txt");
        for (String url : urls) {
            writeLineToFile(write, url);
        }
        try {
            String url = TransferUploader.uploadFileToTransferSh(write);
            event.getChannel().sendMessage("Done! " + url);
        } catch (IOException e) {
            e.printStackTrace();
            event.getChannel().sendMessage("Failed upload!");

        }
    }

    @Handler
    public void testNewBackend(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!testnewbackend"))
            return;

        //Job job = new WikiTeam3Job("testuser", UUID.randomUUID().toString(), "TestJob", "a");
        //job.run();
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

    private static void writeLineToFile(File file, String line) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.append(line);
            writer.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
