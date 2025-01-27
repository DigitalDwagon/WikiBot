package dev.digitaldragon.interfaces.discord;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobManager;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperArgs;
import dev.digitaldragon.jobs.dokuwiki.DokuWikiDumperJob;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Args;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Job;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperArgs;
import dev.digitaldragon.jobs.pukiwiki.PukiWikiDumperJob;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DiscordCommandListener extends ListenerAdapter {
    private static final String[] PERMITTED_ROLES = {
            "1195125742653149294", // Miraheze: MediaWiki Support Volunteers
            "834935892166574130", // Miraheze: Discord Moderators
            "803977072875405422", // Miraheze: Discord Administrators
            "1019837414035959849", // Miraheze: Wiki Creators
            "407534159909748746", // Miraheze: Site Reliability Engineers
            "407534772542242816", // Miraheze: Stewards
            "407534909746577418", // Miraheze: MediaWiki Engineers
            "1112838174071324744", // !digiserver: wikibot
    };

    private static final String[] PERMITTED_USERS = {
            "287696585142304769", // digitaldragon
    };

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        System.out.println("got event");
        boolean permitted = false;
        if (!event.getName().equals("status")) {
            // check if user has the role in the guild they're in
            for (String role : PERMITTED_ROLES) {
                Member member = event.getMember();
                if (member == null) continue;
                if (member.getRoles().stream().anyMatch(r -> r.getId().equals(role))) {
                    permitted = true;
                    break;
                }
            }
            // check if user is in the list of permitted users
            for (String user : PERMITTED_USERS) {
                if (event.getUser().getId().equals(user)) {
                    permitted = true;
                    break;
                }
            }

            //check if user has a permitted role in another guild
            if (!permitted) {
                for (var guild : event.getJDA().getGuilds()) {
                    for (String role : PERMITTED_ROLES) {
                        Member member = guild.getMember(event.getUser());
                        if (member == null) continue;
                        if (member.getRoles().stream().anyMatch(r -> r.getId().equals(role))) {
                            permitted = true;
                            break;
                        }
                    }
                }
            }

            if (!permitted) {
                event.reply("You do not have permission to use this command!").setEphemeral(true).queue();
                return;
            }

        }


        if (event.getName().equals("mediawiki_dump")) onMediaWikiSlashCommandInteraction(event);
        if (event.getName().equals("dokuwiki_dump")) onDokuWikiSlashCommandInteraction(event);
        if (event.getName().equals("pukiwiki_dump")) onPukiWikiSlashCommandInteraction(event);
        if (event.getName().equals("status") || event.getName().equals("abort")) onStatusOrAbortSlashCommandInteraction(event);
    }


    public <T> T processArgs(T args, SlashCommandInteractionEvent event) {
        try {
            System.out.println(Arrays.toString(getArgsFromOptions(event)));
            JCommander.newBuilder()
                    .addObject(args)
                    .build()
                    .parse(getArgsFromOptions(event));
        } catch (ParameterException e) {
            event.reply("Invalid parameters or options! Double check your extra-args").setEphemeral(true).queue();
            return null;
        }
        return args;
    }

    private void submitJob(SlashCommandInteractionEvent event, Job job) {
        job.getMeta().setPlatform(JobMeta.JobPlatform.DISCORD);
        job.getMeta().setDiscordUserId(event.getUser().getId());
        WikiBot.getDiscordClient().getJobListener().setJobChannel(job.getId(), event.getChannel());

        JobManager.submit(job);
        event.reply("Done! Running job `" + job.getId() + "`. I will notify you when it updates. Use `/status` with the job ID or use the `Info` button to manually check the job.")
                .addEmbeds(DiscordClient.getStatusEmbed(job).build())
                .addActionRow(DiscordClient.getJobActionRow(job))
                .setEphemeral(false).queue();
    }

    public void onMediaWikiSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        WikiTeam3Args args = processArgs(new WikiTeam3Args(), event);
        JobMeta meta = new JobMeta(event.getUser().getName());
        meta.setPlatform(JobMeta.JobPlatform.DISCORD);
        Job job = new WikiTeam3Job(args, meta, UUID.randomUUID().toString());
        submitJob(event, job);
    }

    public void onDokuWikiSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        DokuWikiDumperArgs args = processArgs(new DokuWikiDumperArgs(), event);
        JobMeta meta = new JobMeta(event.getUser().getName());
        meta.setPlatform(JobMeta.JobPlatform.DISCORD);
        Job job = new DokuWikiDumperJob(args, meta, UUID.randomUUID().toString());
        submitJob(event, job);
    }

    public void onPukiWikiSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        PukiWikiDumperArgs args = processArgs(new PukiWikiDumperArgs(), event);
        JobMeta meta = new JobMeta(event.getUser().getName());
        meta.setPlatform(JobMeta.JobPlatform.DISCORD);
        Job job = new PukiWikiDumperJob(args, meta, UUID.randomUUID().toString());
        submitJob(event, job);
    }

    public void onStatusOrAbortSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        OptionMapping jobOption = event.getOption("job");
        if (jobOption == null) {
            event.reply("No job ID provided!").setEphemeral(true).queue();
            return;
        }
        String jobId = jobOption.getAsString();
        Job job = JobManager.get(jobId);
        if (job == null) {
            event.reply("Job not found!").setEphemeral(true).queue();
            return;
        }
        String message = "";
        if (event.getName().equals("abort")) {
            message = JobManager.abort(job.getId()) ? "Job aborted!" : "Failed to abort job!";
        }


        event.reply(message)
                .addEmbeds(DiscordClient.getStatusEmbed(job).build())
                .addActionRow(DiscordClient.getJobActionRow(job))
                .setEphemeral(true).queue();
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
