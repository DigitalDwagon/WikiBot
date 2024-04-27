package dev.digitaldragon.interfaces.discord;

import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DiscordButtonListener extends ListenerAdapter {
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getButton().getId().startsWith("status_")) {
            onStatusButton(event);
        }
        if (event.getButton().getId().startsWith("abort_")) {
            onAbortButton(event);
        }
    }

    private void onAbortButton(ButtonInteractionEvent event) {
        String jobId = event.getButton().getId().split("_")[1];
        Job job = JobManager.get(jobId);
        if (job == null) {
            event.reply("Job not found").setEphemeral(true).queue();
            return;
        }
        if (job.abort()) {
            event.reply("Job aborted").addEmbeds(DiscordClient.getStatusEmbed(job).build()).setEphemeral(true).queue();
        } else {
            event.reply("Failed to abort job! It may already be completed, or be in a task that can't be aborted.").addEmbeds(DiscordClient.getStatusEmbed(job).build()).setEphemeral(true).queue();
        }

    }

    private void onStatusButton(ButtonInteractionEvent event) {

        //get job id from after _
        String jobId = event.getButton().getId().split("_")[1];
        //get job from id
        Job job = JobManager.get(jobId);
        if (job == null) {
            event.reply("Job not found").setEphemeral(true).queue();
            return;
        }
        event.replyEmbeds(DiscordClient.getStatusEmbed(job).build()).addActionRow(DiscordClient.getJobActionRow(job)).setEphemeral(true).queue();
    }

}
