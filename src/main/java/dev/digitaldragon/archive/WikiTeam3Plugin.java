package dev.digitaldragon.archive;

import dev.digitaldragon.jobs.wikiteam.WikiTeam3Job;
import dev.digitaldragon.parser.CommandLineParser;
import dev.digitaldragon.util.AfterTask;
import dev.digitaldragon.util.CommandTask;
import dev.digitaldragon.util.IRCClient;
import dev.digitaldragon.warcs.WarcproxManager;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class WikiTeam3Plugin extends ListenerAdapter {
    /**
     * Starts a WikiTeam3 archiving job in the given text channel.
     * Deprecated: use {@link WikiTeam3Job} and {@link dev.digitaldragon.jobs.JobManager} instead.
     *
     * @param channel the text channel where the job will be started
     * @param note note for the job
     * @param userMention the mention of the user starting the job
     * @param userName the name of the user starting the job
     * @param options the options for the job
     */
    @Deprecated
    public static void startJob(TextChannel channel, String note, String userMention, String userName, String options) {
        String jobId = UUID.randomUUID().toString();
        String threadName;
        int maxLength = 100;
        if (jobId.length() <= maxLength) {
            threadName = jobId;
        } else {
            threadName = jobId.substring(0, maxLength - 3) + "...";
        }

        if (threadName.isEmpty())
            threadName = "Unnamed Job";

        //CommandTask downloadTask = new CommandTask("launcher wiki.txt " + options, 2, "DownloadMediaWiki");
        CommandTask downloadTask = new CommandTask("wikiteam3dumpgenerator " + options, 1, "WikiTeam3");
        downloadTask.setSuccessCode(0);
        downloadTask.setAlwaysSuccessful(false);

        channel.createThreadChannel(threadName).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                .queue(thread -> {
                    IRCClient.sendMessage(userName, "Launched job " + jobId + "! (WikiTeam3)");
                    thread.sendMessage(String.format("Running job with WikiTeam3 <https://github.com/saveweb/wikiteam3> (for %s). `%s` ```%s``` Job ID: %s", userName, options, note, jobId)).queue(message -> message.pin().queue());
                    RunJob.startArchive(jobId, note, userMention, userName, thread, jobId, AfterTask.MEDIAWIKI, downloadTask);
                });
    }

    /**
     * Parses the Discord options from a SlashCommandInteractionEvent and returns them as a WikiTeam3 --options string.
     *
     * @param event the SlashCommandInteractionEvent containing the options
     * @return the parsed options as a string
     */
    public static String parseDiscordOptions(SlashCommandInteractionEvent event) {
        StringBuilder options = new StringBuilder();

        processIntRangeOption(event, "delay", 0, 200, "--delay", options); // trailing ' is appended by int range option
        processIntRangeOption(event, "retry", 0, 50, "--retries", options);
        processIntRangeOption(event, "api_chunksize", 1, 500, "--api_chunksize", options);


        processBooleanOption(event, "xml", "--xml", options, true);
        processBooleanOption(event, "images", "--images", options, true);
        processBooleanOption(event, "bypass_compression", "--bypass-cdn-image-compression", options);
        processBooleanOption(event, "xml_api_export", "--xmlapiexport", options);
        processBooleanOption(event, "xml_revisions", "--xmlrevisions", options);
        processBooleanOption(event, "current_only", "--curonly", options);
        processBooleanOption(event, "force", "--force", options);
        processBooleanOption(event, "disable_image_verification", "--disable-image-verify", options);

        processUrlOption(event, "api", "--api", options);
        processUrlOption(event, "index", "--index", options);
        return options.toString();
    }

    private static void processIntRangeOption(SlashCommandInteractionEvent event, String option, int min, int max, String command, StringBuilder options) {
        if (event.getOption(option) == null)
            return;

        Integer optionValue =  event.getOption(option).getAsInt();
        if (optionValue >= min && optionValue <= max) {
            //options.append(command).append(" ").append(optionValue).append("' ");
            options.append(command).append(" ").append(optionValue).append(" ");
        }
    }

    private static void processBooleanOption(SlashCommandInteractionEvent event, String option, String command, StringBuilder options) {
        processBooleanOption(event, option, command, options, false);
    }


    private static void processBooleanOption(SlashCommandInteractionEvent event, String option, String command, StringBuilder options, boolean defaultValue) {
        boolean optionValue = event.getOption(option) != null ? event.getOption(option).getAsBoolean() : defaultValue;
        if (optionValue) {
            options.append(command).append(" ");
        }
    }

    private static void processUrlOption(SlashCommandInteractionEvent event, String option, String command, StringBuilder options) {
        if (event.getOption(option) == null)
            return;
        String optionValue =  event.getOption(option).getAsString();
        if (optionValue == null)
            return;

        try {
            URL uri = new URL(optionValue);
        } catch (MalformedURLException e) {
            event.reply("Invalid URL in " + option).setEphemeral(true).queue();
            return;
        }

        options.append(command).append(" ").append(optionValue).append(" ");
    }

    /**
     * Returns the command line parser for parsing options for WikiTeam3.
     *
     * @return the command line parser
     */
    public static CommandLineParser getCommandLineParser() {
        CommandLineParser parser = new CommandLineParser();
        parser.addDoubleOption("delay");
        parser.addIntOption("retries");
        parser.addIntOption("api_chunksize");
        parser.addIntOption("index-check-threshold");
        parser.addBooleanOption("xml");
        parser.addBooleanOption("images");
        parser.addBooleanOption("bypass-cdn-image-compression");
        parser.addBooleanOption("xmlapiexport");
        parser.addBooleanOption("xmlrevisions");
        parser.addBooleanOption("curonly");
        parser.addBooleanOption("insecure");
        parser.addUrlOption("api");
        parser.addUrlOption("index");
        parser.addUrlOption("url");
        parser.addStringOption("explain");
        parser.addBooleanOption("force");
        parser.addBooleanOption("disable-image-verify");
        parser.addBooleanOption("warc-images");
        parser.addBooleanOption("warc-pages");
        parser.addBooleanOption("warc-pages-history");
        return parser;
    }

    /**
     * Converts a CommandLineParser made by {@link WikiTeam3Plugin#getCommandLineParser()} into a string in --option format.
     *
     * @param commandLineParser the command line parser object
     * @return the string representing the parsed options
     */
    public static String parserToOptions(CommandLineParser commandLineParser) {
        StringBuilder options = new StringBuilder();
        parseDouble("delay", commandLineParser, options);

        parseInt("retries", commandLineParser, options);
        parseInt("api_chunksize", commandLineParser, options);
        parseInt("index-check-threshold", commandLineParser, options);

        parseBoolean("xml", commandLineParser, options);
        parseBoolean("images", commandLineParser, options);
        parseBoolean("bypass-cdn-image-compression", commandLineParser, options);
        parseBoolean("xmlapiexport", commandLineParser, options);
        parseBoolean("xmlrevisions", commandLineParser, options);
        parseBoolean("curonly", commandLineParser, options);
        parseBoolean("insecure", commandLineParser, options);
        parseBoolean("force", commandLineParser, options);
        parseBoolean("disable-image-verify", commandLineParser, options);
        parseBoolean("insecure", commandLineParser, options);
        parseBoolean("force", commandLineParser, options);
        parseBoolean("warc-images", commandLineParser, options);
        parseBoolean("warc-pages", commandLineParser, options);
        parseBoolean("warc-pages-history", commandLineParser, options);
        parseUrl("api", commandLineParser, options);
        parseUrl("index", commandLineParser, options);

        if (commandLineParser.getOption("warc-images") == Boolean.TRUE ||
                commandLineParser.getOption("warc-pages") == Boolean.TRUE ||
                commandLineParser.getOption("warc-pages-history") == Boolean.TRUE) {
            options.append("--warc-proxy localhost:8000 --warc-ca-path ");
            options.append(WarcproxManager.getWarcproxCa().getAbsolutePath());
            options.append(" ");

        }

        if (commandLineParser.getOption("url") != null) {
            options.append(commandLineParser.getOption("url"));
            options.append(" ");
        }

        //options.append("--upload ");

        return options.toString();
    }

    private static void parseBoolean(String name, CommandLineParser commandLineParser, StringBuilder options) {
        if (commandLineParser.getOption(name) == Boolean.TRUE) {
            options.append("--").append(name).append(" ");
        }
    }

    private static void parseInt(String name, CommandLineParser commandLineParser, StringBuilder options) {
        if (commandLineParser.getOption(name) instanceof Integer) {
            options.append("--").append(name).append(" ").append(commandLineParser.getOption(name)).append(" ");
        }
    }

    private static void parseDouble(String name, CommandLineParser commandLineParser, StringBuilder options) {
        if (commandLineParser.getOption(name) instanceof Double) {
            options.append("--").append(name).append(" ").append(commandLineParser.getOption(name)).append(" ");
        }
    }
    private static void parseUrl(String name, CommandLineParser commandLineParser, StringBuilder options) {
        if (commandLineParser.getOption(name) instanceof String) {
            options.append("--").append(name).append(" ").append(commandLineParser.getOption(name)).append(" ");
        }
    }

}
