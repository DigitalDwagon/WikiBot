package dev.digitaldragon.commands;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.DokuWikiDumperPlugin;
import dev.digitaldragon.archive.Uploader;
import dev.digitaldragon.archive.WikiTeam3Plugin;
import dev.digitaldragon.backfeed.LinkExtract;
import dev.digitaldragon.parser.CommandLineParser;
import dev.digitaldragon.util.BulkArchiveParser;
import dev.digitaldragon.util.TransferUploader;
import net.dv8tion.jda.api.entities.TextChannel;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.Actor;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

        if (!isVoiced(channel, event.getActor())) {
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

                DokuWikiDumperPlugin.startJob(discordChannel, url, explain, nick, nick, DokuWikiDumperPlugin.parserToOptions(parser));
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

                    DokuWikiDumperPlugin.startJob(discordChannel, jobUrl, note, nick, nick, options);
                }
                channel.sendMessage(nick + ": Launched " + tasks.size() + " jobs!");
            }
        }

        if (event.getMessage().startsWith("!mediawiki")) {
            CommandLineParser parser = WikiTeam3Plugin.getCommandLineParser();
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
            if (event.getMessage().startsWith("!mediawikisingle ")) {
                if (parser.getOption("explain") == null) {
                    channel.sendMessage(nick + ": Explanation is required! Note: A new bot update now requires explanations in the form of an option, eg \"--explain Closing soon\"");
                    return;
                }
                String explain = parser.getOption("explain").toString();
                WikiTeam3Plugin.startJob(discordChannel, explain, nick, nick, WikiTeam3Plugin.parserToOptions(parser));
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
            if (opts.contains(" ")) {
                channel.sendMessage(nick + ": Too many arguments!");
                return;
            }
            Uploader.reupload(opts, nick, nick, discordChannel);
        }
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
