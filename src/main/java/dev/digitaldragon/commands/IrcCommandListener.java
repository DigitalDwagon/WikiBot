package dev.digitaldragon.commands;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.DokuWikiDumperPlugin;
import dev.digitaldragon.archive.WikiTeam3Plugin;
import dev.digitaldragon.util.BulkArchiveParser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.Channel;
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
        boolean allowed = false;
        Optional<SortedSet<ChannelUserMode>> modes = event.getChannel().getUserModes(event.getActor());
        if (modes.isPresent()) {
            for (ChannelUserMode mode : modes.get()) {
                allowed = mode.getNickPrefix() == '@' || mode.getNickPrefix() == '+';
            }
        }

        TextChannel discordChannel = WikiBot.getLogsChannel();
        if (discordChannel == null) {
            channel.sendMessage(nick + ": Something went wrong.");
        }


        if (!allowed) {
            event.getChannel().sendMessage(event.getActor().getNick() + ": Requires (@) or (+).");
            return;
        }
        String[] parts = event.getMessage().split(" ", 3);
        if (parts.length < 3) {
            channel.sendMessage(nick + ": Not enough arguments!");
            return;
        }

        String url = parts[1];
        String explain = parts[2].split("-", 2)[0];
        String opts = "-" + parts[2].split("-", 2)[1]; //todo jank lol
        if (opts.equals("-")) {
            channel.sendMessage(nick + ": No options given!");
            return;
        }
        explain = explain.trim();
        opts = opts.trim();

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            channel.sendMessage(nick + ": Invalid URL! Remember: the URL parser expects a protocol (http://, https://, etc.) - domains don't count!");
            return;
        }


        if (event.getMessage().startsWith("!doku")) {
            String options = DokuWikiDumperPlugin.parseCommandLineOptions(opts);

            if (event.getMessage().startsWith("!dokusingle ")) {
                if (explain.isEmpty()) {
                    channel.sendMessage(nick + ": No explanation given!");
                    return;
                }
                DokuWikiDumperPlugin.startJob(discordChannel, url, explain, nick, nick, options);
            }
            if (event.getMessage().startsWith("!dokubulk ")) {
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

                    DokuWikiDumperPlugin.startJob(discordChannel, jobUrl, note, nick, nick, DokuWikiDumperPlugin.parseCommandLineOptions(opts));
                }
                channel.sendMessage(nick + ": Launched " + tasks.size() + " jobs!");
            }
        }

        if (event.getMessage().startsWith("!mediawiki")) {
            String options = WikiTeam3Plugin.parseCommandLineOptions(opts);
            if (event.getMessage().startsWith("!mediawikisingle ")) {
                if (explain.isEmpty()) {
                    channel.sendMessage(nick + ": No explanation given!");
                    return;
                }
                WikiTeam3Plugin.startJob(discordChannel, url, explain, nick, nick, options);
            }
            if (event.getMessage().startsWith("!mediawikibulk ")) {
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

                    WikiTeam3Plugin.startJob(discordChannel, jobUrl, note, nick, nick, WikiTeam3Plugin.parseCommandLineOptions(opts));
                }
                channel.sendMessage(nick + ": Launched " + tasks.size() + " jobs!");
            }
        }
    }

    @Handler
    public void helpCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!help"))
            return;
        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();
        channel.sendMessage(nick + ": Bot operator is DigitalDragons.");
        channel.sendMessage(nick + ": !dokusingle <url> <explanation> <--options> - Archive a DokuWiki with DokuWikiDumper.");
        channel.sendMessage(nick + ": !dokubulk <file url> <--options> - Archive DokuWikis in bulk via a text file. Each line should be a URL followed by an explanation, separated by a space. Explanation optional, but highly encouraged.");
        channel.sendMessage(nick + ": Supported DokuWikiDumper options are: --retry --ignore-disposition-header-missing --hard-retry --delay --threads --ignore-errors --ignore-action-disabled-edit --no-resume --insecure --content --media --html --pdf --auto --current-only");
        channel.sendMessage(nick + ": !mediawikisingle <url> <explanation> <--options> - Archive a MediaWiki with WikiTeam3.");
        channel.sendMessage(nick + ": !mediawikibulk <file url> <--options> - The same as dokubulk, but using WikiTeam3 tools.");
        channel.sendMessage(nick + ": Supported WikiTeam3 options are: --delay --retries --api_chunksize --xml --images --bypass-cdn-image-compression --xmlapiexport --xmlrevisions --curonly");
    }
}
