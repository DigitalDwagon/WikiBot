package dev.digitaldragon.interfaces.irc;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.primitives.Chars;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.interfaces.generic.*;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.Wiki;
import dev.digitaldragon.jobs.mediawiki.MediaWikiWARCJob;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Args;
import dev.digitaldragon.jobs.wikimedia.DailyWikimediaDumpJob;
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
        boolean garbled = event.getMessage().startsWith("!s") && !event.getMessage().startsWith("!status");
        if (!event.getMessage().startsWith("!status") && !garbled)
            return;

        String jobId;
        if (event.getMessage().split(" ").length < 2) {
            jobId = null;
        } else {
            jobId = event.getMessage().split(" ")[1];
        }

        String message = StatusHelper.getStatus(jobId);
        if (message != null) {
            if (garbled) {
                message = getFunnyMessage(event.getActor().getNick(), message);
            }
            event.getChannel().sendMessage(event.getActor().getNick() + ": " + message);
        }
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

    @Handler
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


        /*MediaWiki wiki = new MediaWiki(url);
        boolean success = wiki.findInfo();
        if (!success) {
            channel.sendMessage(nick + ": Something went wrong...");
            return;
        }
        channel.sendMessage(nick + ": API: " + wiki.getApiUrl() + " Index: " + wiki.getIndexUrl());
        channel.sendMessage(nick + ": " + wiki.getTotalRevisions()  + " revisions");
        channel.sendMessage(nick + ": " + wiki.getTotalMediaSize() + " media bytes");
        try {
            wiki.run(nick, parts[2]);
        } catch (UserErrorException e) {
            channel.sendMessage(nick + ": " + e.getMessage());
        }*/

    }


    /*@Handler
    public void resumeJobsCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!rdump"))
            return;
        if (!isOped(event.getChannel(), event.getActor()))
            return;

        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();

        for (Job job : JobManager.getJobs()) {
            if (job instanceof WikiTeam3Job) {
                DokuWikiDumperJob dokuJob = (DokuWikiDumperJob) job;
                if (dokuJob.isPaused()) {
                    dokuJob.setPaused(false);
                    channel.sendMessage(nick + ": Resumed job " + dokuJob.getJobId());
                }
            }
        }

        if (event.getMessage().split(" ").length < 2) {
            channel.sendMessage(nick + ": Not enough arguments!");
            return;
        }
        String jobId = event.getMessage().split(" ")[1];

        String message = AbortHelper.abortJob(jobId);
        if (message != null)
            event.getChannel().sendMessage(nick + ": " + message);
    }*/


    @Handler
    public void helpCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!help"))
            return;
        String nick = event.getActor().getNick();
        Channel channel = event.getChannel();
        channel.sendMessage(nick + ": https://wikibot.digitaldragon.dev/help");
    }

    @Handler
    public void mdumpCommand(ChannelMessageEvent event) {
        if (!event.getMessage().startsWith("!wmdump"))
            return;
        if (!isOped(event.getChannel(), event.getActor()))
            return;

        Job wmjob = new DailyWikimediaDumpJob(UUID.randomUUID().toString());
        JobManager.submit(wmjob);
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

    public static String getFunnyMessage(String username, String message) {
        List<Character> chars = Chars.asList(message.toCharArray());
        Collections.shuffle(chars);
        message = new String(Chars.toArray(chars));

        //10% chance:
        if (Math.random() < 0.1) {
            List<String> easterEggMessages = new ArrayList<>();
            easterEggMessages.add("I'm sorry," + username +  ". I'm afraid I can't do that."); // HAL 9000
            easterEggMessages.add("Who do you think you are, fireonlive?"); // joke about fireonlive's sutats spam
            easterEggMessages.add("It's time to stop."); // original
            easterEggMessages.add("The command is a lie."); // portal "The cake is a lie"
            //easterEggMessages.add("wikibot - the less cool archivebot since 2023"); // original
            easterEggMessages.add("All your wiki are belong to us."); // internet "All your base are belong to us"
            easterEggMessages.add("You're a wizard, " + username + "."); // harry potter "You're a wizard, Harry."
            //easterEggMessages.add("It's dangerous to archive alone. Take this!"); // zelda "It's dangerous to go alone. Take this!"
            easterEggMessages.add("This statement is false."); // paradox (portal)
            easterEggMessages.add("I'll be back."); // original
            //easterEggMessages.add("There's no crying in archiving!"); // a league of their own "There's no crying in baseball!"
            //10
            easterEggMessages.add("I'm not locked in here with you. You're locked in here with me!"); // watchmen
            easterEggMessages.add("It's a trap!"); // star wars
            easterEggMessages.add("The first rule of Archive Club is to always talk about Archive Club."); // fight club "The first rule of Fight Club is: You do not talk about Fight Club."
            easterEggMessages.add("Show me the data!"); // jerry maguire "Show me the money!"
            easterEggMessages.add("This is the way."); // the mandalorian
            easterEggMessages.add("I'm not a robot, you're a robot!"); // original
            easterEggMessages.add("That's a bold strategy," + username +". Let's see if it pays off."); // dodgeball "That's a bold strategy, Cotton. Let's see if it pays off for 'em."
            easterEggMessages.add("I've got a jar of dirt! I've got a jar of dirt!"); // pirates of the caribbean
            easterEggMessages.add("I can haz status?"); // lolcats "I can haz cheezburger?"
            //20
            easterEggMessages.add("Press Alt + F4 for a surprise! (Users of penguin-themed operating systems should instead open a terminal and enter ':(){ :|:& };:')"); // "original"
            easterEggMessages.add("As seen on TV!"); // makes fun of the "as seen on TV" ads, minecraft splash text
            easterEggMessages.add("That's no moon..."); // star wars
            easterEggMessages.add("Now bug-free!"); // original based on Minecraft splash text
            easterEggMessages.add("Yes sir, Mister " + username + "!"); // portal "Yes sir, Mister Johnson!"
            easterEggMessages.add("No step on snek."); // meme
            //easterEggMessages.add("Dead links. Dead links everywhere. https://irc.digitaldragon.dev/uploads/344bd7146de4b822/image.png"); // meme
            //easterEggMessages.add("Nobody expects the ArchiveTeam inquisition!"); // original
            easterEggMessages.add("I like trains."); // asdfmovie
            //easterEggMessages.add("https://irc.digitaldragon.dev/uploads/2f2619de2036ae3f/image.png"); // meme
            //30
            easterEggMessages.add("This is my swamp!"); // shrek
            easterEggMessages.add("Built with Minecraft-compatible technology!");

            //pick a random message from the list
            Random random = new Random();
            message = easterEggMessages.get(random.nextInt(easterEggMessages.size()));

        }
        return message;
    }
}