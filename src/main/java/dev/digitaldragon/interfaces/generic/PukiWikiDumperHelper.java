package dev.digitaldragon.interfaces.generic;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperArgs;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperJob;

import java.util.UUID;

public class PukiWikiDumperHelper {
    /**
     * Begins a new job with the given unparsed arguments and username.
     *
     * @param command the command line arguments to parse
     * @param userName the name of the user initiating the job
     * @return a string representing the result of the job initiation
     */
    public static String beginJob(String command, String userName) {
        PukiWikiDumperArgs args = new PukiWikiDumperArgs();

        try {
            JCommander.newBuilder()
                    .addObject(args)
                    .build()
                    .parse(Command.shellSplit(command).toArray(new String[0]));
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
    public static String beginJob(PukiWikiDumperArgs args, String userName, String jobId) {
        String explain = args.getExplain();

        try {
            args.check();
        } catch (UserErrorException e) {
            return e.getMessage();
        }

        Job job = new PukiWikiDumperJob(userName, jobId, args);
        JobManager.submit(job);

        return null;
    }

    public static String beginJob(PukiWikiDumperArgs args, String userName) {
        return beginJob(args, userName, UUID.randomUUID().toString());
    }


}
