package dev.digitaldragon.interfaces.discord;

import com.beust.jcommander.JCommander;
import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.interfaces.generic.WikiTeam3Helper;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Args;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Job;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DiscordCommandListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        System.out.println("got event");
        if (event.getName().equals("mediawiki_dump")) onMediaWikiSlashCommandInteraction(event);
    }


    public void onMediaWikiSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        List<String> opts = new ArrayList<>();
        for (var option : event.getOptions()) {
            String name = option.getName();
            if (name.equals("ignore-disposition-missing")) name = "ignore-disposition-header-missing";

            if (option.getName().equals("extra-args")) {
                String unsplit = option.getAsString();
                if (!unsplit.contains("\"")) //hack to make single quotes work lol
                    unsplit = unsplit.replace("'", "\"");
                unsplit = unsplit.replace("”", "\"");
                unsplit = unsplit.replace("“", "\"");

                String[] split = unsplit.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)");
                opts.addAll(List.of(split));
                continue;
            }

            opts.add("--" + name);
            if (option.getType() == OptionType.BOOLEAN)
                continue;
            opts.add(option.getAsString());


        }
        System.out.println("opts: " + opts);

        String[] unparsedArgs = opts.toArray(new String[0]);
        WikiTeam3Args args = new WikiTeam3Args();
        JCommander.newBuilder()
                .addObject(args)
                .build()
                .parse(unparsedArgs);
        try {
            args.check();
        } catch (UserErrorException e) {
            event.reply(e.getMessage()).setEphemeral(true).queue();
            return;
        }

        String jobId = UUID.randomUUID().toString();
        Job job = new WikiTeam3Job(event.getUser().getName(), jobId, args);
        job.getMeta().setPlatform(JobMeta.JobPlatform.DISCORD);
        job.getMeta().setDiscordUserId(event.getUser().getId());
        WikiBot.getDiscordClient().getJobListener().setJobChannel(jobId, event.getChannel());


        JobManager.submit(job);
        event.reply("Done! Running job `" + jobId + "`. I will notify you when it updates. Use `/status` with the job ID or use the `Status` button to manually check the job.")
                .addEmbeds(DiscordClient.getStatusEmbed(job).build())
                .addActionRow(DiscordClient.getJobActionRow(job))
                .setEphemeral(true).queue();

    }


}
