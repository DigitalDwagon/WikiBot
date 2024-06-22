package dev.digitaldragon.interfaces.generic;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperArgs;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperJob;

import java.util.UUID;

public class DokuWikiDumperHelper {
    /**
     * Begins a new job with the given unparsed arguments and username.
     *
     * @param unparsedArgs the command line arguments to parse
     * @param userName the name of the user initiating the job
     * @return a string representing the result of the job initiation
     */
    public static String beginJob(String unparsedArgs, String userName) {
        DokuWikiDumperArgs args = new DokuWikiDumperArgs();
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
     */
    public static String beginJob(DokuWikiDumperArgs args, String userName) {
        String explain = args.getExplain();
        try {
            args.check();
        } catch (UserErrorException e) {
            return e.getMessage();
        }
        Job job = new DokuWikiDumperJob(userName, UUID.randomUUID().toString(), args.getUrl(), args, explain);
        JobManager.submit(job);

        return null;
    }
}
