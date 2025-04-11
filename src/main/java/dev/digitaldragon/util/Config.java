package dev.digitaldragon.util;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.Optional;

@Getter
public class Config {
    @SerializedName("telegram")
    public TelegramConfig telegramConfig;
    @SerializedName("discord")
    public DiscordConfig discordConfig;
    @SerializedName("dashboard")
    public DashboardConfig dashboardConfig;
    @SerializedName("irc")
    public IRCConfig ircConfig;
    @SerializedName("wikiteam3")
    public WikiTeam3Config wikiTeam3Config;
    @SerializedName("upload")
    public UploadConfig uploadConfig;
    @SerializedName("scripts")
    public ScriptConfig scriptConfig;

    public record TelegramConfig(@SerializedName("enabled") boolean isEnabled, String token, String channelId, Long creatorId, String username) {}
    public record DiscordConfig(@SerializedName("enabled") boolean isEnabled, String token, String channelId, Optional<String> successChannel, Optional<String> failureChannel) {}
    public record DashboardConfig(@SerializedName("enabled") boolean isEnabled, int port) {}
    public record IRCConfig(@SerializedName("enabled") boolean isEnabled, String server, int port, String channel, String nick, String realName, IRCAuthOptions authOptions) {}
    public record IRCAuthOptions(@SerializedName("enabled") boolean isEnabled, String password) {}
    public record WikiTeam3Config(@SerializedName("enabled") boolean isEnabled, String userAgent, String binZstd) {}
    public record UploadConfig(String collection, boolean offloadEnabled, String offloadServer, String transferProvider) {}
    public record ScriptConfig(String pythonPath) {}
}
