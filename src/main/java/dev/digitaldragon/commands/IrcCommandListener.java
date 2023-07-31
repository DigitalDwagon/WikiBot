package dev.digitaldragon.commands;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.DokuWikiDumperPlugin;
import dev.digitaldragon.archive.WikiTeam3Plugin;
import dev.digitaldragon.parser.CommandLineParser;
import dev.digitaldragon.util.BulkArchiveParser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.Actor;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

public class IrcCommandListener {
    @Handler
    public void message(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!doku") && !event.getMessage().startsWith("!mediawiki"))
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
        channel.sendMessage(nick + ": Supported DokuWikiDumper options are: --retry --ignore-disposition-header-missing --hard-retry --delay --threads --ignore-errors --ignore-action-disabled-edit --insecure --content --media --html --pdf --auto --current-only");
        channel.sendMessage(nick + ": !mediawikisingle <--options> - Archive a MediaWiki with WikiTeam3. --explain <your explanation> and --url <target DokuWiki URL) are required.");
        channel.sendMessage(nick + ": !mediawikibulk <--options> - The same as dokubulk, but using WikiTeam3 tools.");
        channel.sendMessage(nick + ": Supported WikiTeam3 options are: --delay --retries --api_chunksize --xml --images --bypass-cdn-image-compression --xmlapiexport --xmlrevisions --curonly --api --index --url");
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
