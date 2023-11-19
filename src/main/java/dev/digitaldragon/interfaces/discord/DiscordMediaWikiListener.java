package dev.digitaldragon.interfaces.discord;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.interfaces.generic.WikiTeam3Helper;
import dev.digitaldragon.jobs.wikiteam.WikiTeam3Args;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class DiscordMediaWikiListener extends ListenerAdapter {/*
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("mediawikiarchive"))
            return;

        TextChannel channel = WikiBot.getLogsChannel();
        if (channel == null) {
            event.reply("Something went wrong.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(true).queue();
        if (Objects.equals(event.getSubcommandName(), "single")) {
            try {
                WikiTeam3Helper.beginJob(parseDiscordOptions(event), event.getUser().getName());
            } catch (UserErrorException exception) {
                event.getHook().editOriginal(exception.getMessage()).queue();
            }
            event.getHook().editOriginal("Launched!").queue();
        }

        if (Objects.equals(event.getSubcommandName(), "bulk")) {
            event.getHook().editOriginal("Sorry, this command is disabled for now.").queue();
        }
    }

    private String getUrlOption(SlashCommandInteractionEvent event, String option) {
        String url = Objects.requireNonNull(event.getOption(option)).getAsString();
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            event.getHook().editOriginal("Invalid URL.").queue();
            return null;
        }
        return url;
    }

    public static WikiTeam3Args parseDiscordOptions(SlashCommandInteractionEvent event) {
        WikiTeam3Args args = new WikiTeam3Args();

        if (event.getOption("delay") != null)
            args.setDelay(event.getOption("delay").getAsDouble());
        if (event.getOption("retry") != null)
            args.setRetries(event.getOption("retry").getAsInt());
        if (event.getOption("api_chunksize") != null)
            args.setApiChunkSize(event.getOption("api_chunksize").getAsInt());

        args.setXml(getBooleanOptionSafely(event, "xml"));
        args.setImages(getBooleanOptionSafely(event, "images"));
        args.setBypassCdnImageCompression(getBooleanOptionSafely(event, "bypass_compression"));
        args.setXmlApiExport(getBooleanOptionSafely(event, "xml_api_export"));
        args.setXmlRevisions(getBooleanOptionSafely(event, "xml_revisions"));
        args.setCurrentOnly(getBooleanOptionSafely(event, "current_only"));
        args.setForce(getBooleanOptionSafely(event, "force"));
        args.setDisableImageVerify(getBooleanOptionSafely(event, "disable_image_verification"));

        if (event.getOption("api") != null)
            args.setApi(event.getOption("api").getAsString());
        if (event.getOption("index") != null)
            args.setIndex(event.getOption("index").getAsString());

        return args;
    }

    private static boolean getBooleanOptionSafely(SlashCommandInteractionEvent event, String option) {
        if (event.getOption(option) != null)
            return event.getOption(option).getAsBoolean();
        return false;
    }

    private static void processBooleanOption(SlashCommandInteractionEvent event, String option, String command, StringBuilder options) {
        processBooleanOption(event, option, command, options, false);
    }


    private static void processBooleanOption(SlashCommandInteractionEvent event, String option, String command, StringBuilder options, boolean defaultValue) {
        boolean optionValue = event.getOption(option) != null ? event.getOption(option).getAsBoolean() : defaultValue;
        if (optionValue) {
            options.append(command).append(" ");
        }
    }*/
}
