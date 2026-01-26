package dev.digitaldragon.interfaces.telegram;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
import dev.digitaldragon.jobs.events.JobCompletedEvent;
import net.badbird5907.lightning.annotation.EventHandler;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class TelegramJobListener {
    @EventHandler
    public void onJobCompleted(JobCompletedEvent event) {
        Job job = event.getJob();
        JobMeta meta = job.getMeta();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(WikiBot.getConfig().getTelegramConfig().channelId());

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(String.format("@%s Success! Job for %s completed.%n", meta.getUserName(), meta.getTargetUrl().orElse("unknown")))
                .append("ID: ").append(job.getId()).append("\n")
                .append("Archive URL: ").append(job.getArchiveUrl()).append("\n")
                .append("Logs: ").append(job.getLogsUrl());
        if (meta.getExplain().isPresent()) messageBuilder.append("\nExplanation: ").append(meta.getExplain().get());
        sendMessage.setText(messageBuilder.toString());

        TelegramClient.getBot().tryToExecute(sendMessage);
    }


    @EventHandler
    public void onJobFailure(JobFailureEvent event) {
        Job job = event.getJob();
        JobMeta meta = job.getMeta();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(WikiBot.getConfig().getTelegramConfig().channelId());

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(String.format("@%s Job for %s failed with exit code %s.%n", meta.getUserName(), meta.getTargetUrl().orElse("unknown"), job.getFailedTaskCode()))
                .append("ID: ").append(job.getId()).append("\n")
                .append("Logs: ").append(job.getLogsUrl()).append("\n");
        if (meta.getExplain().isPresent()) messageBuilder.append("\nExplanation: ").append(meta.getExplain().get());
        sendMessage.setText(messageBuilder.toString());

        TelegramClient.getBot().tryToExecute(sendMessage);
    }

    @EventHandler
    public void onJobAbort(JobAbortEvent event) {
        Job job = event.getJob();
        JobMeta meta = job.getMeta();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(WikiBot.getConfig().getTelegramConfig().channelId());

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(String.format("@%s Job for %s was aborted.%n", meta.getUserName(), meta.getTargetUrl().orElse("unknown")))
                .append("ID: ").append(job.getId()).append("\n")
                .append("Logs: ").append(job.getLogsUrl()).append("\n");
        if (meta.getExplain().isPresent()) messageBuilder.append("\nExplanation: ").append(meta.getExplain().get());
        sendMessage.setText(messageBuilder.toString());

        TelegramClient.getBot().tryToExecute(sendMessage);
    }

    @EventHandler
    public void onJobQueued(JobQueuedEvent event) {
        Job job = event.getJob();
        JobMeta meta = job.getMeta();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(WikiBot.getConfig().getTelegramConfig().channelId());
        //        IRCClient.sendMessage(job.getUserName(), "Queued job! (" + job.getType() + "). You will be notified when it finishes. Use !status " + job.getId() + " for details.");

        sendMessage.setText(String.format("@%s Job for %s queued!%nYou will be notified when it finishes. Use !status %s for details.", meta.getUserName(), meta.getTargetUrl().orElse("unknown"), job.getId()));
        TelegramClient.getBot().tryToExecute(sendMessage);
    }
}
