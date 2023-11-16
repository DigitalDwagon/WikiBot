package dev.digitaldragon.interfaces.discord;

import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordReuploadListener extends ListenerAdapter {
    /*@Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("reupload"))
            return;
        event.deferReply().setEphemeral(true).queue();

        TextChannel channel = WikiBot.getLogsChannel();
        if (channel == null) {
            event.reply("Something went wrong.").setEphemeral(true).queue();
            return;
        }

        event.getHook().editOriginal("Starting...").queue();
        Uploader.reupload(event.getOption("jobid").getAsString(), event.getUser().getName(), event.getUser().getAsMention(), channel);
    }*/
}
