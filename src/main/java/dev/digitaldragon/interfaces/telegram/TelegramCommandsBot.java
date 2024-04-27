package dev.digitaldragon.interfaces.telegram;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.generic.*;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

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
                    message = WikiTeam3Helper.beginJob(arrayToString(ctx.arguments()), ctx.user().getUserName());
                    if (message != null)
                        reply_silent.sendReplyMessage(message, ctx.chatId(), ctx.update().getMessage().getMessageId());
                    else
                        reply_silent.sendReplyMessage(THANKS_MESSAGE, ctx.chatId(), ctx.update().getMessage().getMessageId());
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
                    message = DokuWikiDumperHelper.beginJob(arrayToString(ctx.arguments()), ctx.user().getUserName());

                    if (message != null)
                        reply_silent.sendReplyMessage(message, ctx.chatId(), ctx.update().getMessage().getMessageId());
                    else
                        reply_silent.sendReplyMessage(THANKS_MESSAGE, ctx.chatId(), ctx.update().getMessage().getMessageId());
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
