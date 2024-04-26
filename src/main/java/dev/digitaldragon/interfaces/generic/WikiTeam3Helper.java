package dev.digitaldragon.interfaces.generic;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Args;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Job;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiTeam3Helper {
    public static String[] splitCommandLine(String commandLine) {
        List<String> parts = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(commandLine);

        while (m.find()) {
            parts.add(m.group(1).replace("\"", ""));
        }

        return parts.toArray(new String[0]);
    }

    /**
     * Begins a job with the given unparsed arguments and username.
     *
     * @param unparsedArgs The unparsed arguments for the job.
     * @param userName The name of the user.
     * @return A string representing the result of the job initiation.
     */ //TODO switch to the new Args class-type parser here to enable compatibility with more platforms and modules.
    public static String beginJob(String unparsedArgs, String userName) {
        WikiTeam3Args args = new WikiTeam3Args();
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
     * Begins a job with the given arguments and username.
     *
     * @param args The parsed arguments for the job.
     * @param userName The name of the user.
     * @return A string representing the result of the job initiation.
     */
    public static String beginJob(WikiTeam3Args args, String userName, String jobId) {

        try {
            args.check();
        } catch (UserErrorException e) {
            return e.getMessage();
        }

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

        Job job = new WikiTeam3Job(userName, jobId, args);
        JobManager.submit(job);
        return null;
    }

    public static String beginJob(WikiTeam3Args args, String userName) {
        return beginJob(args, userName, UUID.randomUUID().toString());
    }
}
