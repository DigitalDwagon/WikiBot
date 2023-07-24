package dev.digitaldragon.commands;

import net.engio.mbassy.listener.Handler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.kitteh.irc.client.library.element.Channel;
import org.kitteh.irc.client.library.element.mode.ChannelMode;
import org.kitteh.irc.client.library.element.mode.ChannelUserMode;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.helper.MessageEvent;

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

        if (!allowed) {
            event.getChannel().sendMessage(event.getActor().getNick() + ": Requires (@) or (+).");
            return;
        }
        String[] parts = event.getMessage().split(" ");
        String url = parts[1];
        String explain = "";
        String opts = "";
        for (int i = 2; i < parts.length; i++) {
            if (!parts[i].startsWith("-"))
                explain += " " + parts[i];
            else
                opts += " " + parts[i];
        }
        if (explain.isEmpty()) {
            channel.sendMessage(nick + ": No explanation given!");
            return;
        }
        explain = explain.trim();
        opts = opts.trim();

        if (event.getMessage().startsWith("!doku")) {
            if (event.getMessage().startsWith("!dokusingle ")) {
                event.getChannel().sendMessage("Just testing. (dokuwiki single)");


            }
            if (event.getMessage().startsWith("!dokubulk ")) {
                event.getChannel().sendMessage("Just testing. (dokuwiki bulk)");
            }
        }

    }
}
