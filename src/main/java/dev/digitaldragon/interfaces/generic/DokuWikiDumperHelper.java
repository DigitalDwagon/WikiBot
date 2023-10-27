package dev.digitaldragon.interfaces.generic;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.DokuWikiDumperPlugin;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperJob;
import dev.digitaldragon.parser.CommandLineParser;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.UUID;

public class DokuWikiDumperHelper {
    /**
     * Begins a new job with the given unparsed arguments and username.
     *
     * @param unparsedArgs the command line arguments to parse
     * @param userName the name of the user initiating the job
     * @return a string representing the result of the job initiation
     * @throws UserErrorException if there is an error with user input
     */
    public static String beginJob(String unparsedArgs, String userName) throws UserErrorException {
        CommandLineParser parser = DokuWikiDumperPlugin.getCommandLineParser();
        parser.addBooleanOption("old-backend");
        try {
            parser.parse(unparsedArgs.split(" "));
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }

        if (parser.getOption("url") == null) {
            return "URL is required! Note: A new bot update now requires URLs in the form of an option, eg \"--url https://wikipedia.org\"";
        }

        if (parser.getOption("explain") == null) {
            return "An explanation is required! Note: A new bot update now requires explanations in the form of an option, eg \"--explain Closing soon\"";
        }

        return beginJob(parser, userName);
    }

    /**
     * Begins a new job with the given parsed command line arguments and username.
     *
     * @param parser the parsed command line arguments
     * @param userName the name of the user initiating the job
     * @return a string representing the result of the job initiation
     * @throws UserErrorException if there is an error with user input
     */
    public static String beginJob(CommandLineParser parser, String userName) throws UserErrorException {
        TextChannel discordChannel = WikiBot.getLogsChannelSafely();
        String explain = parser.getOption("explain").toString();
        String url = parser.getOption("url").toString();

        if (parser.getOption("old-backend") == Boolean.TRUE) {
            DokuWikiDumperPlugin.startJob(discordChannel, url, explain, userName, userName, DokuWikiDumperPlugin.parserToOptions(parser));
        } else {
            Job job = new DokuWikiDumperJob(userName, UUID.randomUUID().toString(), url, DokuWikiDumperPlugin.parserToOptions(parser), explain);
            JobManager.submit(job);
        }

        return null;
    }
}
