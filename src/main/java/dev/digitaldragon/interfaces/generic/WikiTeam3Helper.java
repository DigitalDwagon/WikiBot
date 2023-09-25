package dev.digitaldragon.interfaces.generic;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.WikiTeam3Plugin;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.wikiteam.WikiTeam3Job;
import dev.digitaldragon.parser.CommandLineParser;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.UUID;

public class WikiTeam3Helper {
    //TODO switch to the new Args class-type parser here to enable compatibility with more platforms and modules.
    public static String beginJob(String unparsedArgs, String userName) throws UserErrorException {
        CommandLineParser parser = WikiTeam3Plugin.getCommandLineParser();
        parser.addBooleanOption("old-backend");
        try {
            parser.parse(unparsedArgs.split(" "));
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }

        if (parser.getOption("url") == null && parser.getOption("api") == null && parser.getOption("index") == null) {
            return "You need to specify --url, --api, or --index! Note: URLs are required in the form of an option, eg \"--url https://wikipedia.org\"";
        }

        if (parser.getOption("explain") == null)
            return "An explanation is required! Note: A new bot update now requires explanations in the form of an option, eg \"--explain Closing soon\"";


        return beginJob(parser, userName);
    }

    public static String beginJob(CommandLineParser parser, String userName) throws UserErrorException {
        TextChannel discordChannel = WikiBot.getLogsChannelSafely();

        String jobName = parser.getOption("url") == null ? null : parser.getOption("url").toString();
        if (jobName == null)
            jobName = parser.getOption("api") == null ? null : parser.getOption("api").toString();
        if (jobName == null)
            jobName = parser.getOption("index") == null ? null : parser.getOption("index").toString();

        String explain = parser.getOption("explain").toString();

        if (parser.getOption("old-backend") == Boolean.TRUE) {
            WikiTeam3Plugin.startJob(discordChannel, explain, userName, userName, WikiTeam3Plugin.parserToOptions(parser));
        } else {
            Job job = new WikiTeam3Job(userName, UUID.randomUUID().toString(), userName, WikiTeam3Plugin.parserToOptions(parser), explain);
            JobManager.submit(job);
        }
        return null;
    }
}
