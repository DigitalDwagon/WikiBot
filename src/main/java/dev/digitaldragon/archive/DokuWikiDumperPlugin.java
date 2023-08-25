package dev.digitaldragon.archive;

import dev.digitaldragon.parser.CommandLineParser;
import dev.digitaldragon.util.AfterTask;
import dev.digitaldragon.util.CommandTask;
import dev.digitaldragon.util.IRCClient;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.UUID;

public class DokuWikiDumperPlugin extends ListenerAdapter {
    /*@Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("dokuwikiarchive")) {
            return;
        }
        if (Boolean.parseBoolean(EnvConfig.getConfigs().get("disable_doku_archive"))) {
            event.reply("This command is disabled. This can happen for a number of reasons:\n- You're accidentally using the testing bot, when you should be using the main one\n- There is an ongoing technical issue, and archiving had to be temporarily halted")
                    .setEphemeral(true).queue();
            return;
        }

        String options = parseOptions(event);
        //validate server is okay
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
            String url = Objects.requireNonNull(event.getOption("url")).getAsString();
            String note = Objects.requireNonNull(event.getOption("explain")).getAsString();

            //ensure URL is good
            try {
                URL uri = new URL(url);
            } catch (MalformedURLException e) {
                event.reply("Invalid URL.").setEphemeral(true).queue();
                return;
            }

            event.reply("Launching job for <" + url + ">").queue();
            startJob(channel, url, note, event.getUser(), options);
        }

        //bulk command execution
        if (Objects.equals(event.getSubcommandName(), "bulk")) {
            Message.Attachment bulk = Objects.requireNonNull(event.getOption("file")).getAsAttachment();
            if (!Objects.equals(bulk.getContentType(), "text/plain; charset=utf-8")) {
                event.reply("Your uploaded file is invalid.").setEphemeral(true).queue();
                return;
            }


            try {
                URL fileUrl = new URL(bulk.getUrl());
                HttpURLConnection conn = (HttpURLConnection) fileUrl.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;


                    Map<String, String> tasks = new HashMap<>();
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty())
                            continue;

                        String[] parts = line.split(" ", 2);
                        String url = parts[0];
                        try {
                            URL verifyValid = new URL(url);
                        } catch (MalformedURLException e) {
                            event.reply("Invalid URL: " + url).setEphemeral(true).queue();
                            return;
                        }
                        String note = parts.length > 1 ? parts[1] : "No note provided."; // check if second part exists
                        tasks.put(url, note);
                    }

                    for (Map.Entry<String, String> entry : tasks.entrySet()) {
                        String url = entry.getKey();
                        String note = entry.getValue();

                        startJob(channel, url, note, event.getUser(), options);
                    }
                    event.reply(String.format("Spawned %s jobs for %s", tasks.size(), bulk.getFileName())).queue();

                } else {
                    event.reply("Sorry, the server returned a bad response code: " + responseCode).queue();
                }
            } catch (IOException e) {
                event.reply("Sorry, there was an issue downloading your file.").queue();
            }
        }
    }*/

    public static void startJob(TextChannel channel, String url, String note, String userMention, String userName, String options) {
        String threadName;
        int maxLength = 100;
        if (url.length() <= maxLength) {
            threadName = url;
        } else {
            threadName = url.substring(0, maxLength - 3) + "...";
        }

        CommandTask task = new CommandTask("dokuWikiDumper " + options + url, 1, "DokuWikiDumper");
        task.setSuccessCode(0);
        task.setAlwaysSuccessful(false);

        channel.createThreadChannel(threadName).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                .queue(thread -> {
                    String jobId = UUID.randomUUID().toString();
                    IRCClient.sendMessage(userName, String.format("Launched job %s for %s! (DokuWikiDumper)", jobId, url));
                    RunJob.startArchive(url, note, userMention, userName, thread, jobId, AfterTask.NONE, task);
                    thread.sendMessage(String.format("Running job on <%s> with DokuWikiDumper (<>) (for %s). `%s` ```%s``` \n Job ID: %s", url, userName, options, note, jobId)).queue(message -> message.pin().queue());
                });
    }

    public static String parseDiscordOptions(SlashCommandInteractionEvent event) {
        StringBuilder options = new StringBuilder();
        processIntRangeOption(event, "delay", 1, 10, "--delay", options);
        processIntRangeOption(event, "retry", 1, 50, "--retry", options);
        processIntRangeOption(event, "hard_retry", 1, 50, "--hard-retry", options);
        processIntRangeOption(event, "threads", 1, 50, "--threads", options);

        processBooleanOption(event, "upload", "--upload", options, true);
        processBooleanOption(event, "auto", "--auto", options, true);

        processBooleanOption(event, "ignore_disposition", "--ignore-disposition-header-missing", options);
        processBooleanOption(event, "current_only", "--current-only", options);
        processBooleanOption(event, "no_resume", "--no-resume", options);
        processBooleanOption(event, "insecure", "--insecure", options);
        //processBooleanOption(event, "ignore_errors", "--ignore-errors", options); todo
        processBooleanOption(event, "ignore_disabled_edit", "--ignore-action-disabled-edit", options);
        processBooleanOption(event, "content", "--content", options);
        processBooleanOption(event, "media", "--media", options);
        processBooleanOption(event, "html", "--html", options);
        processBooleanOption(event, "pdf", "--pdf", options);
        return options.toString();
    }

    private static void processIntRangeOption(SlashCommandInteractionEvent event, String option, int min, int max, String command, StringBuilder options) {
        if (event.getOption(option) == null)
            return;

        Integer optionValue =  event.getOption(option).getAsInt();
        if (optionValue >= min && optionValue <= max) {
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

    /*public static String parseCommandLineOptions(String args) {
        String[] validArgs = {
                "--retry",
                "--ignore-disposition-header-missing",
                "--hard-retry",
                "--delay",
                "--threads",
                "--ignore-errors",
                "--ignore-action-disabled-edit",
                "--no-resume",
                "--insecure",
                "--content",
                "--media",
                "--html",
                "--pdf",
                "--auto",
                "--current-only"
        };

        StringBuilder result = new StringBuilder();
        Matcher m = Pattern.compile("--\\w+\\b|\\d+").matcher(args);
        String lastArg = null;
        while (m.find()) {
            String arg = m.group(0);
            if (arg.startsWith("--") && Arrays.asList(validArgs).contains(arg)) {
                lastArg = arg;
                result.append(arg);
                result.append(' ');
            } else if (lastArg != null && Character.isDigit(arg.charAt(0))) {
                result.append(arg);
                result.append(' ');
            }
        }
        result.append("--upload ");
        return result.toString().trim() + " "; // code always expects a trailing space after options, so we add one in here. todo hack
    }*/

    public static CommandLineParser getCommandLineParser() {
        CommandLineParser parser = new CommandLineParser();
        parser.addBooleanOption("ignore-disposition-header-missing");
        parser.addIntOption("retry");
        parser.addIntOption("hard-retry");
        parser.addDoubleOption("delay");
        parser.addIntOption("threads");
        //parser.addBooleanOption("ignore-errors"); todo
        parser.addBooleanOption("ignore-action-disabled-edit");
        parser.addBooleanOption("insecure");
        parser.addBooleanOption("content");
        parser.addBooleanOption("media");
        parser.addBooleanOption("html");
        parser.addBooleanOption("pdf");
        parser.addBooleanOption("auto");
        parser.addBooleanOption("current-only");
        parser.addStringOption("explain");
        parser.addUrlOption("url");
        return parser;
    }

    public static String parserToOptions(CommandLineParser commandLineParser) {
        StringBuilder options = new StringBuilder();
        parseInt("retry", commandLineParser, options);
        parseInt("hard-retry", commandLineParser, options);
        parseInt("threads", commandLineParser, options);

        parseDouble("delay", commandLineParser, options);


        parseBoolean("ignore-disposition-header-missing", commandLineParser, options);
        //parseBoolean("ignore-errors", commandLineParser, options); todo
        parseBoolean("ignore-action-disabled-edit", commandLineParser, options);
        parseBoolean("insecure", commandLineParser, options);
        parseBoolean("content", commandLineParser, options);
        parseBoolean("media", commandLineParser, options);
        parseBoolean("html", commandLineParser, options);
        parseBoolean("pdf", commandLineParser, options);
        parseBoolean("auto", commandLineParser, options);
        parseBoolean("current-only", commandLineParser, options);

        options.append("--upload ");

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

}
