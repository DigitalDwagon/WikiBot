package dev.digitaldragon.interfaces.discord;

import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordDokuWikiListener extends ListenerAdapter {/*
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("dokuwikiarchive"))
            return;
        TextChannel channel = WikiBot.getLogsChannel();
        if (channel == null) {
            event.reply("Something went wrong.").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(true).queue();
        if (Objects.equals(event.getSubcommandName(), "single")) {
            String url = getUrlOption(event, "url");
            if (url == null)
                return;
            String explain = Objects.requireNonNull(event.getOption("explain")).getAsString();
            event.getHook().editOriginal("Launching job for <" + url + ">").queue();
            //DokuWikiDumperPlugin.startJob(channel, url, explain, event.getUser().getAsMention(), event.getUser().getName(), DokuWikiDumperPlugin.parseDiscordOptions(event));
            String id = UUID.randomUUID().toString();
            Job job = new DokuWikiDumperJob(event.getUser().getName(), id, url, DokuWikiDumperPlugin.parseDiscordOptions(event), explain);
            JobManager.submit(job);

        }

        if (Objects.equals(event.getSubcommandName(), "bulk")) {
            Message.Attachment bulk = Objects.requireNonNull(event.getOption("file")).getAsAttachment();
            Map<String, String> tasks;
            try {
                tasks = BulkArchiveParser.parse(bulk.getUrl());
            } catch (Exception e) {
                event.getHook().editOriginal(e.getMessage()).queue();
                return;
            }
            for (Map.Entry<String, String> entry : tasks.entrySet()) {
                String url = entry.getKey();
                String note = entry.getValue();

                //DokuWikiDumperPlugin.startJob(channel, url, note, event.getUser().getAsMention(), event.getUser().getName(), DokuWikiDumperPlugin.parseDiscordOptions(event));
                String id = UUID.randomUUID().toString();
                Job job = new DokuWikiDumperJob(event.getUser().getName(), id, url, DokuWikiDumperPlugin.parseDiscordOptions(event), note);
                JobManager.submit(job);
            }
            event.getHook().editOriginal("Launched " + tasks.size() + " jobs!").queue();
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
    }*/
}
