package dev.digitaldragon.interfaces.discord;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperArgs;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperJob;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Args;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Job;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperArgs;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperJob;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DiscordCommandListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        System.out.println("got event");
        if (event.getName().equals("mediawiki_dump")) onMediaWikiSlashCommandInteraction(event);
        if (event.getName().equals("dokuwiki_dump")) onDokuWikiSlashCommandInteraction(event);
        if (event.getName().equals("pukiwiki_dump")) onPukiWikiSlashCommandInteraction(event);

    }


    public <T> T processArgs(T args, SlashCommandInteractionEvent event) {
        try {
            System.out.println(Arrays.toString(getArgsFromOptions(event)));
            JCommander.newBuilder()
                    .addObject(args)
                    .build()
                    .parse(getArgsFromOptions(event));
            // Assuming args has a check() method
            if (args instanceof WikiTeam3Args) ((WikiTeam3Args) args).check();
            if (args instanceof DokuWikiDumperArgs) ((DokuWikiDumperArgs) args).check();
            if (args instanceof PukiWikiDumperArgs) ((PukiWikiDumperArgs) args).check();
        } catch (ParameterException e) {
            event.reply("Invalid parameters or options! Double check your extra-args").setEphemeral(true).queue();
            return null;
        } catch (UserErrorException e) {
            event.reply(e.getMessage()).setEphemeral(true).queue();
            return null;
        }
        return args;
    }

    private void submitJob(SlashCommandInteractionEvent event, Job job) {
        String jobId = UUID.randomUUID().toString();
        job.getMeta().setPlatform(JobMeta.JobPlatform.DISCORD);
        job.getMeta().setDiscordUserId(event.getUser().getId());
        WikiBot.getDiscordClient().getJobListener().setJobChannel(jobId, event.getChannel());

        JobManager.submit(job);
        event.reply("Done! Running job `" + jobId + "`. I will notify you when it updates. Use `/status` with the job ID or use the `Status` button to manually check the job.")
                .addEmbeds(DiscordClient.getStatusEmbed(job).build())
                .addActionRow(DiscordClient.getJobActionRow(job))
                .setEphemeral(true).queue();
    }

    public void onMediaWikiSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        WikiTeam3Args args = processArgs(new WikiTeam3Args(), event);
        Job job = new WikiTeam3Job(event.getUser().getName(), UUID.randomUUID().toString(), args);
        submitJob(event, job);
    }

    public void onDokuWikiSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        DokuWikiDumperArgs args = processArgs(new DokuWikiDumperArgs(), event);
        Job job = new DokuWikiDumperJob(event.getUser().getName(), UUID.randomUUID().toString(), args.getUrl(), args.get(), args.getExplain());
        submitJob(event, job);
    }

    public void onPukiWikiSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        PukiWikiDumperArgs args = processArgs(new PukiWikiDumperArgs(), event);
        Job job = new PukiWikiDumperJob(event.getUser().getName(), UUID.randomUUID().toString(), args);
        submitJob(event, job);
    }


    private String[] getArgsFromOptions(SlashCommandInteractionEvent event) {
        System.out.println("getting args from options");
        List<String> opts = new ArrayList<>();
        for (var option : event.getOptions()) {
            System.out.println(option.getName() + " " + option.getType() + " " + option.getAsString());
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
        return opts.toArray(new String[0]);
    }


}
