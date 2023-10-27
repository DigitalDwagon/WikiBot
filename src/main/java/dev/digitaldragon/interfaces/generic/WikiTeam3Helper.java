package dev.digitaldragon.interfaces.generic;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.WikiBot;
import dev.digitaldragon.archive.WikiTeam3Plugin;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.wikiteam.WikiTeam3Args;
import dev.digitaldragon.jobs.wikiteam.WikiTeam3Job;
import dev.digitaldragon.parser.CommandLineParser;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.UUID;

public class WikiTeam3Helper {
    /**
     * Begins a job with the given unparsed arguments and username.
     *
     * @param unparsedArgs The unparsed arguments for the job.
     * @param userName The name of the user.
     * @return A string representing the result of the job initiation.
     * @throws UserErrorException If there is an error with the user input.
     */ //TODO switch to the new Args class-type parser here to enable compatibility with more platforms and modules.
    public static String beginJob(String unparsedArgs, String userName) throws UserErrorException {
        WikiTeam3Args args = new WikiTeam3Args();
        try {
            JCommander.newBuilder()
                    .addObject(args)
                    .build()
                    .parse(unparsedArgs.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)"));
        } catch (ParameterException e) {
            return "Invalid parameters or options! Hint: make sure that your --explain is in quotes if it has more than one word. (-e \"no coverage\")";
        }
        return beginJob(args, userName);
    }

    /**
     * Begins a job with the given arguments and username.
     *
     * @param args The parsed arguments for the job.
     * @param userName The name of the user.
     * @return A string representing the result of the job initiation.
     * @throws UserErrorException If there is an error with the user input.
     */
    public static String beginJob(WikiTeam3Args args, String userName) throws UserErrorException {
        args.check();
        if (args.getUrl() == null && args.getApi() == null && args.getIndex() == null)
            return "You need to specify --url, --api, or --index! Note: URLs are required in the form of an option, eg \"--url https://wikipedia.org\"";
        if (args.getExplain() == null)
            return "An explanation is required! Note: Explanations are required in the form of an option, eg \"--explain Closing soon\"";

        String jobName = args.getUrl();
        if (jobName == null)
            jobName = args.getApi();
        if (jobName == null)
            jobName = args.getIndex();

        String explain = args.getExplain();

        Job job = new WikiTeam3Job(userName, UUID.randomUUID().toString(), jobName, args.get(), explain);
        JobManager.submit(job);
        return null;
    }
}
