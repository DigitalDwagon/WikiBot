package dev.digitaldragon.interfaces.irc;

import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.interfaces.generic.*;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.BiFunction;

public class IrcCommandListener {
    private boolean submissionsEnabled = true;

    @Handler
    public void message(ChannelMessageEvent event) {
        Map<String, BiFunction<String, String, String>> commandHandlers = new HashMap<>();
        commandHandlers.put("!dokusingle", DokuWikiDumperHelper::beginJob);
        commandHandlers.put("!dw", DokuWikiDumperHelper::beginJob);
        commandHandlers.put("!pw", PukiWikiDumperHelper::beginJob);
        commandHandlers.put("!mediawikisingle", WikiTeam3Helper::beginJob);
        commandHandlers.put("!mw", WikiTeam3Helper::beginJob);
        commandHandlers.put("!reupload", ReuploadHelper::beginJob);

        String message = event.getMessage();
        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();

        String[] parts = message.split(" ", 2);

        String command = parts[0];

        if (commandHandlers.containsKey(command)) {
            if (parts.length < 2) {
                channel.sendMessage(nick + ": Not enough arguments!");
                return;
            }

            String opts = parts[1];

            try {
                checkUserPermissions(channel, event.getActor(), true);
                String resultMessage = commandHandlers.get(command).apply(opts, nick);
                if (resultMessage != null) {
                    channel.sendMessage(nick + ": " + resultMessage);
                }
            } catch (UserErrorException exception) {
                channel.sendMessage(nick + ": " + exception.getMessage());
            }
        }
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

    @Handler
    public void abortCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!abort"))
            return;

        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();
        try {
            checkUserPermissions(channel, event.getActor(), false);
        } catch (UserErrorException e) {
            channel.sendMessage(nick + ": " + e.getMessage());
            return;
        }

        if (event.getMessage().split(" ").length < 2) {
            channel.sendMessage(nick + ": Not enough arguments!");
            return;
        }
        String jobId = event.getMessage().split(" ")[1];

        String message = AbortHelper.abortJob(jobId);
        if (message != null)
            event.getChannel().sendMessage(nick + ": " + message);
    }

    @Handler
    public void statusCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!status"))
            return;

        String jobId;
        if (event.getMessage().split(" ").length < 2) {
            jobId = null;
        } else {
            jobId = event.getMessage().split(" ")[1];
        }

        String message = StatusHelper.getStatus(jobId);
        event.getChannel().sendMessage(event.getActor().getNick() + ": " + message);
    }

    @Handler
    public void pauseCommand(ChannelMessageEvent event) {
        if (!event.getMessage().equals("!pause"))
            return;

        if (!isOped(event.getChannel(), event.getActor()))
            return;

        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();

        submissionsEnabled = !submissionsEnabled;
        channel.sendMessage(nick + ": Submissions are now " + (submissionsEnabled ? "enabled." : "disabled."));
    }

    /*@Handler
    public void wikiDetection(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!a "))
            return;

        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();

        String[] parts = event.getMessage().split(" ", 3);
        if (parts.length < 3) {
            channel.sendMessage(nick + ": Not enough arguments!");
            return;
        }
        String url = parts[1];

        try {
            Wiki wiki = Wiki.detectWiki(url);
            wiki.run(url, nick, parts[2]);
        } catch (UserErrorException e) {
            channel.sendMessage(nick + ": " + e.getMessage());
        }

    }*/

    @Handler
    public void helpCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!help"))
            return;
        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();
        channel.sendMessage(nick + ": https://wikibot.digitaldragon.dev/help");
    }

    /*@Handler
    public void mdumpCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!wmdump"))
            return;
        if (!isOped(event.getChannel(), event.getActor()))
            return;

        Job wmjob = new DailyWikimediaDumpJob(UUID.randomUUID().toString());
        JobManager.submit(wmjob);
    }*/

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

    /*@Handler
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

    @Handler
    public void testWarc(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!warctest"))
            return;

        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();

        String[] parts = event.getMessage().split(" ", 2);
        if (parts.length < 2) {
            channel.sendMessage(nick + ": Not enough arguments!");
            return;
        }

        MediaWikiWARCJob job = new MediaWikiWARCJob(UUID.randomUUID().toString(), parts[1], new JobMeta(nick));
        JobManager.submit(job);
    }*/

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