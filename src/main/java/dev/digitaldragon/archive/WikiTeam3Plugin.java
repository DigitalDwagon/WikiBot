package dev.digitaldragon.archive;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.parser.CommandLineParser;
import dev.digitaldragon.util.AfterTask;
import dev.digitaldragon.util.CommandTask;
import dev.digitaldragon.util.EnvConfig;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiTeam3Plugin extends ListenerAdapter {
    /*@Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("mediawikiarchive")) {
            return;
        }
        if (Boolean.parseBoolean(EnvConfig.getConfigs().get("disable_mediawiki_archive"))) {
            event.reply("This command is disabled. This can happen for a number of reasons:\n- You're accidentally using the testing bot, when you should be using the main one\n- There is an ongoing technical issue, and archiving had to be temporarily halted")
                    .setEphemeral(true).queue();
            return;
        }

        String options = parseDiscordOptions(event);
        //validate server is okay
        boolean isTest = Boolean.parseBoolean(EnvConfig.getConfigs().get("is_test"));
        if (isTest) {
            System.out.println(options);
            return;
        }
        Guild testServer = event.getJDA().getGuildById("349920496550281226");
        if (testServer == null) {
            event.reply("Something went wrong.").queue();
            return;
        }
        TextChannel channel = (TextChannel) testServer.getGuildChannelById("1112606638017368124");
        if (channel == null) {
            event.reply("Something went wrong.").queue();
            return;
        }

        // Single command execution
        if (Objects.equals(event.getSubcommandName(), "single")) {
            String url = "";
            if (event.getOption("url") != null)
                url = Objects.requireNonNull(event.getOption("url")).getAsString();

            String note = Objects.requireNonNull(event.getOption("explain")).getAsString();

            //ensure URL is good
            if (!url.isEmpty())
                try {
                    URL uri = new URL(url);
                } catch (MalformedURLException e) {
                    event.reply("Invalid URL.").setEphemeral(true).queue();
                    return;
                }

            event.reply("Launching job for <" + url + ">").queue();
            startJob(channel, url, note, event.getUser(), options);

        }

        if (Objects.equals(event.getSubcommandName(), "bulk")) {
            event.reply("Sorry, this functionality isn't implemented yet").setEphemeral(true).queue();
        }
    }*/

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

        /*CommandTask integrityCheckTask = new CommandTask("grep -E '<title(.*?)>' *.xml -c;grep -E '<page(.*?)>' *.xml -c;grep \"</page>\" *.xml -c;grep -E '<revision(.*?)>' *.xml -c;grep \"</revision>\" *.xml -c", 3, "IntegrityCheck");
        integrityCheckTask.setAlwaysSuccessful(true); //todo this just prints the information out, it doesn't halt uploading if there's no integrity.
        //todo this does not work as it needs to be run in the dump directory.
        */

        //CommandTask compressionTask = new CommandTask("find . -mindepth 1 -maxdepth 1 -type d -exec sh -c '(cd \"{}\" && 7za a -t7z \"wikidump.7z\" *)' \\; -exec sh -c '(cd \"{}\" && 7za a -t7z \"history.7z\" *.json *.xml *.txt *.html)' \\;\n", 2, "CompressMediaWiki");



        channel.createThreadChannel(threadName).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                .queue(thread -> {
                    WikiBot.ircClient.sendMessage(EnvConfig.getConfigs().get("ircchannel").trim(), userName + ": Launched job " + jobId + "! (WikiTeam3)");
                    thread.sendMessage(String.format("Running job with WikiTeam3 <https://github.com/saveweb/wikiteam3> (for %s). `%s` ```%s``` Job ID: %s", userName, options, note, jobId)).queue(message -> message.pin().queue());
                    RunJob.startArchive(jobId, note, userMention, userName, thread, jobId, AfterTask.MEDIAWIKI, downloadTask);
                });
    }

    public static String parseDiscordOptions(SlashCommandInteractionEvent event) {
        StringBuilder options = new StringBuilder();
        /*processIntRangeOption(event, "delay", 0, 200, "--generator-arg='--delay", options); // trailing ' is appended by int range option
        processIntRangeOption(event, "retry", 0, 50, "--generator-arg='--retries", options);
        processIntRangeOption(event, "api_chunksize", 1, 500, "--generator-arg='--api_chunksize", options);


        processBooleanOption(event, "xml", "--generator-arg='--xml'", options, true);
        processBooleanOption(event, "images", "--generator-arg='--images'", options, true);
        processBooleanOption(event, "bypass_compression", "--generator-arg='--bypass-cdn-image-compression'", options);
        processBooleanOption(event, "xml_api_export", "--generator-arg='--xmlapiexport'", options);
        processBooleanOption(event, "xml_revisions", "--generator-arg='--xmlrevisions'", options);
        processBooleanOption(event, "current_only", "--generator-arg='--curonly'", options);*/

        processIntRangeOption(event, "delay", 0, 200, "--delay", options); // trailing ' is appended by int range option
        processIntRangeOption(event, "retry", 0, 50, "--retries", options);
        processIntRangeOption(event, "api_chunksize", 1, 500, "--api_chunksize", options);


        processBooleanOption(event, "xml", "--xml", options, true);
        processBooleanOption(event, "images", "--images", options, true);
        processBooleanOption(event, "bypass_compression", "--bypass-cdn-image-compression", options);
        processBooleanOption(event, "xml_api_export", "--xmlapiexport", options);
        processBooleanOption(event, "xml_revisions", "--xmlrevisions", options);
        processBooleanOption(event, "current_only", "--curonly", options);

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


    /*public static String parseCommandLineOptions(String args) {
        String[] validArgs = {
                "--delay",
                "--retries",
                "--api_chunksize",
                "--xml",
                "--images",
                "--bypass-cdn-image-compression",
                "--xmlapiexport",
                "--xmlrevisions",
                "--curonly",
                "--api",
                "--index"
                // --delay --retries --api_chunksize --xml --images --bypass-cdn-image-compression --xmlapiexport --xmlrevisions --curonly --api --index
        };

        StringBuilder result = new StringBuilder();
        Matcher m = Pattern.compile("--\\w+\\b|\\d+|(http[s]?://[^\\s]*)").matcher(args);
        String lastArg = null;

        while (m.find()) {
            String arg = m.group(0);
            if (arg.startsWith("--")) {
                lastArg = arg;
                result.append(arg).append(' ');
            } else if (lastArg != null) {
                result.append(arg).append(' ');
            }
        }
        return result.toString().trim() + " "; // code always expects a trailing space after options, so we add one in here. todo hack
    }*/

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
