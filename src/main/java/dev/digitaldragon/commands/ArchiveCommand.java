package dev.digitaldragon.commands;

import dev.digitaldragon.Main;
import dev.digitaldragon.archive.DokuWikiArchive;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

public class ArchiveCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("dokuwikiarchive")) {
            return;
        }

        //validate server is okay
        Guild testServer = Main.getInstance().getGuildById("349920496550281226");
        if (testServer == null) {
            event.reply("Something went wrong.").queue();
            return;
        }
        TextChannel channel = (TextChannel) testServer.getGuildChannelById("1112606638017368124");
        if (channel == null) {
            event.reply("Something went wrong.").queue();
            return;
        }


        String url = Objects.requireNonNull(event.getOption("url")).getAsString(); // Assuming the option name is "url"
        String note = Objects.requireNonNull(event.getOption("explain")).getAsString(); // Assuming the option name is "explain"
        String options = parseOptions(event);

        //ensure URL is good
        try {
            URL uri = new URL(url);
        } catch (MalformedURLException e) {
            event.reply("Invalid URL.").setEphemeral(true).queue();
            return;
        }

        String threadName;
        int maxLength = 100;
        if (url.length() <= maxLength) {
            threadName = url;
        } else {
            threadName = url.substring(0, maxLength - 3) + "...";
        }

        channel.createThreadChannel(threadName)
                .queue(thread -> {
                    DokuWikiArchive.ArchiveWiki(url, note, event.getUser(), thread, options);
                    event.reply(thread.getAsMention()).setEphemeral(true).queue();
                    thread.sendMessage(String.format("Running archivation job on <%s> (for %s). `%s` ```%s```", url, event.getUser().getAsMention(), options, note)).queue(message -> message.pin().queue());
                });
    }

    public String parseOptions(SlashCommandInteractionEvent event) {
        StringBuilder options = new StringBuilder();
        if (event.getOption("ignore_disposition") != null)
            if (event.getOption("ignore_disposition").getAsBoolean())
                options.append("--ignore-disposition-header-missing ");

        if (event.getOption("delay") != null)
            if (event.getOption("delay").getAsInt() <= 10)
                options.append("--delay ").append(event.getOption("delay").getAsInt()).append(" ");

        if (event.getOption("retry") != null)
            if (event.getOption("retry").getAsInt() <= 50 && event.getOption("retry").getAsInt() >= 1)
                options.append("--retry ").append(event.getOption("retry").getAsInt()).append(" ");

        if (event.getOption("hard_retry") != null)
            if (event.getOption("hard_retry").getAsInt() <= 50 && event.getOption("hard_retry").getAsInt() >= 1)
                options.append("--hard-retry ").append(event.getOption("hard_retry").getAsInt()).append(" ");

        if (event.getOption("current_only") != null)
            if (event.getOption("current_only").getAsBoolean())
                options.append("--current-only ");

        if (event.getOption("threads") != null)
            if (event.getOption("threads").getAsInt() <= 50 && event.getOption("threads").getAsInt() >= 1)
                options.append("--threads ").append(event.getOption("threads").getAsInt()).append(" ");

        if (event.getOption("auto") != null) {
            if (event.getOption("auto").getAsBoolean())
                options.append("--auto ");
        } else {
            options.append("--auto ");
        }

        if (event.getOption("no_resume") != null)
            if (event.getOption("no_resume").getAsBoolean())
                options.append("--no-resume ");

        if (event.getOption("insecure") != null)
            if (event.getOption("insecure").getAsBoolean())
                options.append("--insecure ");

        if (event.getOption("ignore_errors") != null)
            if (event.getOption("ignore_errors").getAsBoolean())
                options.append("--ignore-errors ");

        if (event.getOption("ignore_disabled_edit") != null)
            if (event.getOption("ignore_disabled_edit").getAsBoolean())
                options.append("--ignore-action-disabled-edit ");

        if (event.getOption("upload") != null) {
            if (event.getOption("upload").getAsBoolean())
                options.append("--upload ");
        } else {
            options.append("--upload ");
        }

        if (event.getOption("content") != null)
            if (event.getOption("content").getAsBoolean())
                options.append("--content ");

        if (event.getOption("media") != null)
            if (event.getOption("media").getAsBoolean())
                options.append("--media ");

        if (event.getOption("html") != null)
            if (event.getOption("html").getAsBoolean())
                options.append("--html ");

        if (event.getOption("pdf") != null)
            if (event.getOption("pdf").getAsBoolean())
                options.append("--pdf ");

        return options.toString();
    }
}
