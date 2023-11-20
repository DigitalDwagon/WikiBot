package dev.digitaldragon.interfaces.telegram;

import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.interfaces.api.UpdatesWebsocket;
import dev.digitaldragon.interfaces.generic.*;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import dev.digitaldragon.util.EnvConfig;
import net.badbird5907.lightning.annotation.EventHandler;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

public class TelegramCommandsBot extends AbilityBot {
    private static final String THANKS_MESSAGE = "Thanks for your request! Your job has been queued. Watch our feed in https://t.me/usewikibot for updates about this job. If archiving is successful, you'll see an item pop up on https://archive.org/details/@digitaldragons";
    private static String arrayToString(String[] array) {
        StringBuilder sb = new StringBuilder();
        for (String s : array) {
            sb.append(s).append(" ");
        }
        String string = sb.toString();
        return string.trim();
    }

    private void tryToExecute(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TelegramCommandsBot() {
        super(EnvConfig.getConfigs().get("telegram_api_token"), "wikiteambot");
    }

    @Override
    public long creatorId() {
        return 5307122986L; // ID 5307122986 for @DigitalDwagon
    }

    public Ability start() {
        return Ability
                .builder()
                .name("start")
                .info("Displays help information for wikibot")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> silent.send("Welcome to wikibot! Wikibot is a bot used to download MediaWiki and DokuWiki sites, and upload them to the Internet Archive for preservation. Learn about commands: https://cdn.digitaldragon.dev/wikibot/help.html", ctx.chatId()))
                .setStatsEnabled(true)
                .build();
    }

    public Ability mw() {
        return Ability
                .builder()
                .name("mw")
                .info("Download a MediaWiki")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    String message = null;
                    try {
                        System.out.println(arrayToString(ctx.arguments()));
                        message = WikiTeam3Helper.beginJob(arrayToString(ctx.arguments()), ctx.user().getUserName());
                    } catch (UserErrorException e) {
                        message = e.getMessage();
                    }
                    if (message != null)
                        silent.send(message, ctx.chatId());
                    else
                        silent.send(THANKS_MESSAGE, ctx.chatId());
                })
                .setStatsEnabled(true)
                .build();
    }

    public Ability dw() {
        return Ability
                .builder()
                .name("dw")
                .info("Download a DokuWiki")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    String message = null;
                    try {
                        System.out.println(arrayToString(ctx.arguments()));
                        message = DokuWikiDumperHelper.beginJob(arrayToString(ctx.arguments()), ctx.user().getUserName());
                    } catch (UserErrorException e) {
                        message = e.getMessage();
                    }

                    if (message != null)
                        silent.send(message, ctx.chatId());
                    else
                        silent.send(THANKS_MESSAGE, ctx.chatId());
                })
                .setStatsEnabled(true)
                .build();
    }

    public Ability jobStatus() {
        return Ability
                .builder()
                .name("status")
                .info("Get running and queued job count")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    String jobId;
                    try {
                        jobId = ctx.firstArg();
                    } catch (Exception e) {
                        jobId = null;
                    }
                    silent.send(StatusHelper.getStatus(jobId), ctx.chatId());
                })
                .setStatsEnabled(true)
                .build();
    }

    public Ability jobAbort() {
        return Ability
                .builder()
                .name("abort")
                .info("Abort (stop) a currently running job")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    String message = null;
                    String jobId;
                    try {
                        jobId = ctx.firstArg();
                        message = AbortHelper.abortJob(jobId);
                    } catch (Exception e) {
                        message = "Please provide a job ID.";
                    }
                    silent.send(message, ctx.chatId());
                })
                .setStatsEnabled(true)
                .build();
    }

    public Ability jobReupload() {
        return Ability
                .builder()
                .name("reupload")
                .info("Reupload a job that failed uploading to the Internet Archive.")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    String message = null;
                    String jobId;
                    try {
                        jobId = ctx.firstArg();
                        message = ReuploadHelper.beginJob(jobId, ctx.user().getUserName());
                    } catch (Exception e) {
                        message = "Please provide a job ID.";
                    }
                    silent.send(message, ctx.chatId());
                })
                .setStatsEnabled(true)
                .build();
    }

    @EventHandler
    public void onJobSuccess(JobSuccessEvent event) {
        Job job = event.getJob();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(EnvConfig.getConfigs().get("telegram_chat_id"));
        sendMessage.setText(String.format("@%s Success! Job for %s completed.%n", job.getUserName(), job.getName()) +
                "ID: " + job.getId() + "\n" +
                "Archive URL: " + job.getArchiveUrl() + "\n" +
                "Explanation: " + job.getExplanation() + "\n" +
                "Logs: " + job.getLogsUrl());
        tryToExecute(sendMessage);
    }

    @EventHandler
    public void onJobFailure(JobFailureEvent event) {
        Job job = event.getJob();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(EnvConfig.getConfigs().get("telegram_chat_id"));
        sendMessage.setText(String.format("@%s Job for %s failed with exit code %s.%n", job.getUserName(), job.getName(), job.getFailedTaskCode()) +
                "ID: " + job.getId() + "\n" +
                "Archive URL: " + job.getArchiveUrl() + "\n" +
                "Explanation: " + job.getExplanation() + "\n" +
                "Logs: " + job.getLogsUrl());
        tryToExecute(sendMessage);
    }

    @EventHandler
    public void onJobAbort(JobAbortEvent event) {
        UpdatesWebsocket.sendLogMessageToClients(event.getJob(), "ABORTED");

    }

    @EventHandler
    public void onJobQueued(JobQueuedEvent event) {
        Job job = event.getJob();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(EnvConfig.getConfigs().get("telegram_chat_id"));
        //        IRCClient.sendMessage(job.getUserName(), "Queued job! (" + job.getType() + "). You will be notified when it finishes. Use !status " + job.getId() + " for details.");

        sendMessage.setText(String.format("@%s Job for %s queued!%nYou will be notified when it finishes. Use !status %s for details.", job.getUserName(), job.getName(), job.getId()));
        tryToExecute(sendMessage);
    }


}
