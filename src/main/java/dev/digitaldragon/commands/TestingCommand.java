package dev.digitaldragon.commands;

import dev.digitaldragon.archive.RunJob;
import dev.digitaldragon.util.CommandTask;
import dev.digitaldragon.util.EnvConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TestingCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        try {
            if (!event.getName().equals("testarchivetask")) {
                return;
            }
            if (Boolean.parseBoolean(EnvConfig.getConfigs().get("disable_testing_command"))) {
                event.reply("This command is disabled. This can happen for a number of reasons:\n- You're accidentally using the testing bot, when you should be using the main one\n- There is an ongoing technical issue, and archiving had to be temporarily halted")
                        .setEphemeral(true).queue();
                return;
            }


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

            String note = "testing task";
            User user = event.getUser();


            String threadName = "testing task";

            System.out.println("reached");

            CommandTask firstListTask = new CommandTask("dir", 1, "List Work Directory");
            firstListTask.setAlwaysSuccessful(true);

            System.out.println("reached");

            CommandTask makeFileTask = new CommandTask("New-Item test.txt", 2, "Make file");
            makeFileTask.setSuccessCode(90);

            System.out.println("reached");

            CommandTask secondListTask = new CommandTask("dir", 3, "Make file");
            secondListTask.setAlwaysSuccessful(true);

            System.out.println("reached");
            event.reply("ok").setEphemeral(true).queue();

            channel.createThreadChannel(threadName).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                    .queue(thread -> {
                        System.out.println("reached");
                        String jobId = UUID.randomUUID().toString();
                        RunJob.startArchive("Testing task", note, user, thread, jobId, firstListTask, secondListTask, makeFileTask);
                        thread.sendMessage(String.format("Running test archivation job (for %s). ```%s``` \n Job ID: %s", user.getAsTag(), note, jobId)).queue(message -> message.pin().queue());
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
