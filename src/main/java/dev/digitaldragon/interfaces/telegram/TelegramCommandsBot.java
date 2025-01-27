package dev.digitaldragon.interfaces.telegram;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.generic.AbortHelper;
import dev.digitaldragon.interfaces.generic.ReuploadHelper;
import dev.digitaldragon.interfaces.generic.StatusHelper;
import dev.digitaldragon.jobs.JobLaunchException;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperJob;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Job;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.text.ParseException;
import java.util.UUID;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

public class TelegramCommandsBot extends AbilityBot {
    private static final String THANKS_MESSAGE = "Thanks for your request! Your job has been queued. Watch our feed in https://t.me/usewikibot for updates about this job. If archiving is successful, you'll see an item pop up on https://archive.org/details/@digitaldragons";
    private TelegramSilentReplySender reply_silent = new TelegramSilentReplySender(sender);
    private static String arrayToString(String[] array) {
        StringBuilder sb = new StringBuilder();
        for (String s : array) {
            sb.append(s).append(" ");
        }
        String string = sb.toString();
        return string.trim();
    }

    public void tryToExecute(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TelegramCommandsBot() {
        super(WikiBot.getConfig().getTelegramConfig().token(), "wikiteambot");
    }

    @Override
    public long creatorId() {
        return 5307122986L; // ID 5307122986 for @DigitalDwagon
    }

    public Ability start() {
        String startMessage = "Welcome to wikibot! Wikibot is a bot used to download MediaWiki and DokuWiki sites, and upload them to the Internet Archive for preservation. Learn about commands: https://cdn.digitaldragon.dev/wikibot/help.html";

        return Ability
                .builder()
                .name("start")
                .info("Displays help information for wikibot")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> reply_silent.sendReplyMessage(startMessage, ctx.chatId(), ctx.update().getMessage().getMessageId()))
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
                    System.out.println(arrayToString(ctx.arguments()));
                    JobMeta meta = new JobMeta(ctx.user().getUserName());
                    meta.setPlatform(JobMeta.JobPlatform.TELEGRAM);
                    try {
                        WikiTeam3Job job = new WikiTeam3Job(arrayToString(ctx.arguments()), meta, UUID.randomUUID().toString());
                        JobManager.submit(job);
                    } catch (JobLaunchException e) {
                        message = e.getMessage();
                    } catch (ParseException e) {
                        message = "Invalid parameters or options! Hint: make sure that your --explain is in quotes if it has more than one word. (-e \"no coverage\")";
                    }
                    if (message == null) message = THANKS_MESSAGE;

                    reply_silent.sendReplyMessage(message, ctx.chatId(), ctx.update().getMessage().getMessageId());
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
                    System.out.println(arrayToString(ctx.arguments()));
                    JobMeta meta = new JobMeta(ctx.user().getUserName());
                    meta.setPlatform(JobMeta.JobPlatform.TELEGRAM);
                    try {
                        DokuWikiDumperJob job = new DokuWikiDumperJob(arrayToString(ctx.arguments()), meta, UUID.randomUUID().toString());
                        JobManager.submit(job);
                    } catch (JobLaunchException e) {
                        message = e.getMessage();
                    } catch (ParseException e) {
                        message = "Invalid parameters or options! Hint: make sure that your --explain is in quotes if it has more than one word. (-e \"no coverage\")";
                    }
                    if (message == null) message = THANKS_MESSAGE;

                    reply_silent.sendReplyMessage(message, ctx.chatId(), ctx.update().getMessage().getMessageId());
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
                    reply_silent.sendReplyMessage(StatusHelper.getStatus(jobId), ctx.chatId(), ctx.update().getMessage().getMessageId());
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
                    reply_silent.sendReplyMessage(message, ctx.chatId(), ctx.update().getMessage().getMessageId());
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
                    reply_silent.sendReplyMessage(message, ctx.chatId(), ctx.update().getMessage().getMessageId());
                })
                .setStatsEnabled(true)
                .build();
    }
}
