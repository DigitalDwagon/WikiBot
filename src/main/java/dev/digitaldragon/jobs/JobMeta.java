package dev.digitaldragon.jobs;

import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
public class JobMeta {
    private String userName;
    private Optional<String> explain = Optional.empty();
    private JobPlatform platform;
    private SilentMode silentMode = SilentMode.ALL;
    private Optional<String> targetUrl = Optional.empty();
    private Optional<String> discordUserId = Optional.empty();

    public JobMeta(String userName) {
        this.userName = userName;
    }

    public enum JobPlatform {
        DISCORD,
        IRC,
        TELEGRAM,
        API
    }

    public enum SilentMode {
        ALL,
        FAIL,
        END,
        SILENT
    }

    public void setExplain(String explain) {
        if (explain != null && !explain.isEmpty()) this.explain = Optional.of(explain);
    }

    public void setTargetUrl(String targetUrl) {
        if (targetUrl != null && !targetUrl.isEmpty()) this.targetUrl = Optional.of(targetUrl);
    }

    public void setDiscordUserId(String discordUserId) {
        if (discordUserId != null && !discordUserId.isEmpty()) this.discordUserId = Optional.of(discordUserId);
    }




}