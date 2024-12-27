package dev.digitaldragon.interfaces.generic;

import com.beust.jcommander.ParameterException;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Args;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Job;

import java.util.UUID;

public class WikiTeam3Helper {
    /**
     * Begins a job with the given unparsed arguments and username.
     *
     * @param command The unparsed arguments for the job.
     * @param userName The name of the user.
     * @return A string representing the result of the job initiation.
     */ //TODO switch to the new Args class-type parser here to enable compatibility with more platforms and modules.
    public static String beginJob(String command, String userName) {
        try {
            return beginJob(new WikiTeam3Args(Command.shellSplit(command).toArray(new String[0])), userName);
        } catch (ParameterException e) {
            return "Invalid parameters or options! Hint: make sure that your --explain is in quotes if it has more than one word. (-e \"no coverage\")";
        }
    }

    /**
     * Begins a job with the given arguments and username.
     *
     * @param args The parsed arguments for the job.
     * @param userName The name of the user.
     * @return A string representing the result of the job initiation.
     */
    public static String beginJob(WikiTeam3Args args, String userName, String jobId) {
        if (args.getUrl() == null && args.getApi() == null && args.getIndex() == null)
            return "You need to specify --url, --api, or --index! Note: URLs are required in the form of an option, eg \"--url https://wikipedia.org\"";

        String jobName = args.getUrl();
        if (jobName == null)
            jobName = args.getApi();
        if (jobName == null)
            jobName = args.getIndex();

        Job job = new WikiTeam3Job(userName, jobId, args);
        JobManager.submit(job);
        return null;
    }

    public static String beginJob(WikiTeam3Args args, String userName) {
        return beginJob(args, userName, UUID.randomUUID().toString());
    }
}
