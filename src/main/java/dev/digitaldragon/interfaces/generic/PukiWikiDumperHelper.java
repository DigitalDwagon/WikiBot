package dev.digitaldragon.interfaces.generic;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperArgs;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperJob;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperArgs;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperJob;

import java.util.UUID;

public class PukiWikiDumperHelper {
    /**
     * Begins a new job with the given unparsed arguments and username.
     *
     * @param unparsedArgs the command line arguments to parse
     * @param userName the name of the user initiating the job
     * @return a string representing the result of the job initiation
     * @throws UserErrorException if there is an error with user input
     */
    public static String beginJob(String unparsedArgs, String userName) throws UserErrorException {
        PukiWikiDumperArgs args = new PukiWikiDumperArgs();
        if (!unparsedArgs.contains("\"")) //hack to make single quotes work lol
            unparsedArgs = unparsedArgs.replace("'", "\"");
        unparsedArgs = unparsedArgs.replace("”", "\"");
        unparsedArgs = unparsedArgs.replace("“", "\"");

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
     * Begins a new job with the given parsed command line arguments and username.
     *
     * @param args the DokuWikiDumperArgs object
     * @param userName the name of the user initiating the job
     * @return a string representing the result of the job initiation
     * @throws UserErrorException if there is an error with user input
     */
    public static String beginJob(PukiWikiDumperArgs args, String userName) throws UserErrorException {
        String explain = args.getExplain();
        args.check();

        Job job = new PukiWikiDumperJob(userName, UUID.randomUUID().toString(),args);
        JobManager.submit(job);

        return null;
    }
}
