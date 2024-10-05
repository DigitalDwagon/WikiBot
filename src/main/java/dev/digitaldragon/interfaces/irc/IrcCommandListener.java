package dev.digitaldragon.interfaces.irc;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.interfaces.generic.*;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.mediawiki.MediaWikiWARCJob;
import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;

import java.net.URLEncoder;
import java.util.*;
import java.util.function.BiFunction;

public class IrcCommandListener {
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
        // !bulk has its own listener for now, so skipped here.

        // --- Non-job specific commands ---
        commands.put("help", () -> {
            channel.sendMessage(nick + ": https://wikibot.digitaldragon.dev/help");
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

        if (WikiBot.getConfig().getWikiTeam3Config().warcEnabled()) commands.put("warctest", () -> {
            JobManager.submit(new MediaWikiWARCJob(UUID.randomUUID().toString(), parts[1], new JobMeta(nick)));
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

        commands.put("mediawikisingle", () -> runHelper(channel, user, message, WikiTeam3Helper::beginJob));
        commands.put("mw", () -> runHelper(channel, user, message, WikiTeam3Helper::beginJob));
        commands.put("dokusingle", () -> runHelper(channel, user, message, DokuWikiDumperHelper::beginJob));
        commands.put("dw", () -> runHelper(channel, user, message, DokuWikiDumperHelper::beginJob));
        commands.put("pukisingle", () -> runHelper(channel, user, message, PukiWikiDumperHelper::beginJob));
        commands.put("pw", () -> runHelper(channel, user, message, PukiWikiDumperHelper::beginJob));
        commands.put("reupload", () -> runHelper(channel, user, message, ReuploadHelper::beginJob));

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
        if (reply != null) channel.sendMessage(user.getNick() + ": " + message);
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