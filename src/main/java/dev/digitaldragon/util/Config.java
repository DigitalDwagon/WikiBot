package dev.digitaldragon.util;

import lombok.Getter;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Getter
public class Config {
    public TelegramConfig telegramConfig;
    public DiscordConfig discordConfig;
    public DashboardConfig dashboardConfig;
    public IRCConfig ircConfig;
    public WikiTeam3Config wikiTeam3Config;
    public UploadConfig uploadConfig;

    public Config(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) throw new RuntimeException("Config file not found: " + filename);
        String content = Files.readString(file.toPath());
        JSONObject json = new JSONObject(content);

        if (json.has("telegram")) {
            JSONObject telegram = json.getJSONObject("telegram");
            telegramConfig = new TelegramConfig(
                    telegram.getBoolean("enabled"),
                    telegram.getString("token"),
                    telegram.getString("channelId"),
                    Long.parseLong(telegram.getString("creatorId")),
                    telegram.getString("username")
            );
        } else {
            telegramConfig = new TelegramConfig(false, null, null, null, null);
        }

        if (json.has("discord")) {
            JSONObject discord = json.getJSONObject("discord");
            discordConfig = new DiscordConfig(
                    discord.getBoolean("enabled"),
                    discord.getString("token"),
                    discord.getString("channelId"),
                    !discord.optString("successChannel").equals("disabled") ? Optional.of(discord.getString("successChannel")) : Optional.empty(),
                    !discord.optString("failureChannel").equals("disabled") ? Optional.of(discord.getString("failureChannel")) : Optional.empty()
            );
        } else {
            discordConfig = new DiscordConfig(false, null, null, Optional.empty(), Optional.empty());
        }

        if (json.has("dashboard")) {
            JSONObject dashboard = json.getJSONObject("dashboard");
            dashboardConfig = new DashboardConfig(
                    dashboard.getBoolean("enabled"),
                    dashboard.getInt("port")
            );
        } else {
            dashboardConfig = new DashboardConfig(false, 0);
        }

        if (json.has("irc")) {
            JSONObject irc = json.getJSONObject("irc");
            JSONObject auth = irc.getJSONObject("auth");
            IRCAuthOptions ircAuthOptions = new IRCAuthOptions(
                    auth.getBoolean("enabled"),
                    auth.getString("password")
            );
            ircConfig = new IRCConfig(
                    irc.getBoolean("enabled"),
                    irc.getString("server"),
                    irc.getInt("port"),
                    irc.getString("channel"),
                    irc.getString("nick"),
                    irc.getString("realName"),
                    ircAuthOptions
            );

        } else {
            ircConfig = new IRCConfig(false, null, 0, null, null, null, new IRCAuthOptions(false, null));
        }

        if (json.has("wikiteam3")) {
            JSONObject wikiteam3 = json.getJSONObject("wikiteam3");
            wikiTeam3Config = new WikiTeam3Config(
                    wikiteam3.getBoolean("enabled"),
                    wikiteam3.getString("userAgent"),
                    wikiteam3.getBoolean("warcEnabled"),
                    wikiteam3.getBoolean("autoWarc"),
                    wikiteam3.getString("binZstd")
            );
        } else {
            wikiTeam3Config = new WikiTeam3Config(false, null, false, false, null);
        }

        if (json.has("upload")) {
            JSONObject upload = json.getJSONObject("upload");
            uploadConfig = new UploadConfig(upload.getString("collection"), upload.getBoolean("offloadEnabled"), upload.getString("offloadServer"));
        } else {
            uploadConfig = new UploadConfig("opensource", false, "");
        }
    }



    public record TelegramConfig(boolean isEnabled, String token, String channelId, Long creatorId, String username) {}
    public record DiscordConfig(boolean isEnabled, String token, String channelId, Optional<String> successChannel, Optional<String> failureChannel) {}
    public record DashboardConfig(boolean isEnabled, int port) {}
    public record IRCConfig(boolean isEnabled, String server, int port, String channel, String nick, String realName, IRCAuthOptions authOptions) {}
    public record IRCAuthOptions(boolean isEnabled, String password) {}
    public record WikiTeam3Config(boolean isEnabled, String userAgent, boolean warcEnabled, boolean autoWarc, String binZstd) {}
    public record UploadConfig(String collection, boolean offloadEnabled, String offloadServer) {}
}
