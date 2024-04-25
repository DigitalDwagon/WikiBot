package dev.digitaldragon.jobs;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
public class JobMeta {
    private String userName;
    private Optional<String> explain;
    private JobPlatform platform;
    private Optional<String> targetUrl;
    private Optional<String> discordUserId;

    public JobMeta(String userName) {
        this.userName = userName;
    }

    public enum JobPlatform {
        DISCORD,
        IRC,
        TELEGRAM,
        API
    }

    public void setExplain(String explain) {
        this.explain = Optional.of(explain);
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = Optional.of(targetUrl);
    }

    public void setDiscordUserId(String discordUserId) {
        this.discordUserId = Optional.of(discordUserId);
    }




}
