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

    public record TelegramConfig(boolean isEnabled, String token, String channelId, Long creatorId, String username) {}
    public record DiscordConfig(boolean isEnabled, String token, String channelId, Optional<String> successChannel, Optional<String> failureChannel) {}
    public record DashboardConfig(boolean isEnabled, int port) {}
    public record IRCConfig(boolean isEnabled, String server, int port, String channel, String nick, String realName, IRCAuthOptions authOptions) {}
    public record IRCAuthOptions(boolean isEnabled, String password) {}
    public record WikiTeam3Config(boolean isEnabled, String userAgent, String binZstd) {}
    public record UploadConfig(String collection, boolean offloadEnabled, String offloadServer, String transferProvider) {}
}
