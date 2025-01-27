package dev.digitaldragon.interfaces.discord;

import com.beust.jcommander.ParameterException;
import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobLaunchException;
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

import java.text.ParseException;
import java.util.ArrayList;
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

        if (event.getName().equals("status") || event.getName().equals("abort")) {
            onStatusOrAbortSlashCommandInteraction(event);
            return;
        }


        try {
            JobMeta meta = new JobMeta(event.getUser().getName());
            meta.setPlatform(JobMeta.JobPlatform.DISCORD);
            meta.setDiscordUserId(event.getUser().getId());

            String[] args = getArgsFromOptions(event);

            Job job = null;
            switch (event.getName()) {
                case "mediawiki_dump" -> job = new WikiTeam3Job(new WikiTeam3Args(args, meta), meta, UUID.randomUUID().toString());
                case "dokuwiki_dump" -> job = new DokuWikiDumperJob(new DokuWikiDumperArgs(args, meta), meta, UUID.randomUUID().toString());
                case "pukiwiki_archive" -> job = new PukiWikiDumperJob(new PukiWikiDumperArgs(args, meta), meta, UUID.randomUUID().toString());
            }
            assert job != null;
            JobManager.submit(job);
            WikiBot.getDiscordClient().getJobListener().setJobChannel(job.getId(), event.getChannel());

            event.reply("Done! Running job `" + job.getId() + "`. I will notify you when it updates. Use `/status` with the job ID or use the `Info` button to manually check the job.")
                    .addEmbeds(DiscordClient.getStatusEmbed(job).build())
                    .addActionRow(DiscordClient.getJobActionRow(job))
                    .setEphemeral(false).queue();
        } catch (JobLaunchException e) {
            event.reply(e.getMessage()).setEphemeral(true).queue();
        } catch (ParseException | ParameterException e) {
            event.reply("Invalid parameters or options! Hint: make sure that your --explain is in quotes if it has more than one word. (-e \"no coverage\")").setEphemeral(true).queue();
        }

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
