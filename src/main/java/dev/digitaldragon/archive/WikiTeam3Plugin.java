package dev.digitaldragon.archive;

import dev.digitaldragon.parser.CommandLineParser;
import dev.digitaldragon.util.AfterTask;
import dev.digitaldragon.util.CommandTask;
import dev.digitaldragon.util.IRCClient;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class WikiTeam3Plugin extends ListenerAdapter {
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

    public static CommandLineParser getCommandLineParser() {
        CommandLineParser parser = new CommandLineParser();
        parser.addDoubleOption("delay");
        parser.addIntOption("retries");
        parser.addIntOption("api_chunksize");
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
        return parser;
    }

    public static String parserToOptions(CommandLineParser commandLineParser) {
        StringBuilder options = new StringBuilder();
        parseDouble("delay", commandLineParser, options);

        parseInt("retries", commandLineParser, options);
        parseInt("api_chunksize", commandLineParser, options);

        parseBoolean("xml", commandLineParser, options);
        parseBoolean("images", commandLineParser, options);
        parseBoolean("bypass-cdn-image-compression", commandLineParser, options);
        parseBoolean("xmlapiexport", commandLineParser, options);
        parseBoolean("xmlrevisions", commandLineParser, options);
        parseBoolean("curonly", commandLineParser, options);
        parseBoolean("insecure", commandLineParser, options);
        parseBoolean("force", commandLineParser, options);
        parseUrl("api", commandLineParser, options);
        parseUrl("index", commandLineParser, options);

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
