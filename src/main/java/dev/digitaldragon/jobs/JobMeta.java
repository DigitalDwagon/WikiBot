package dev.digitaldragon.jobs;

import com.beust.jcommander.Parameter;
import dev.digitaldragon.util.SilentModeParser;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.Optional;

@Getter
@Setter
public class JobMeta {
    private String userName;
    private JobPlatform platform = null;
    @Parameter(names = {"--explain", "-e"})
    @Nullable
    private String explain = null;
    @Parameter(names = {"--silent-mode"}, converter = SilentModeParser.class)
    private SilentMode silentMode = SilentMode.ALL;
    @Parameter(names = {"--queue"})
    private String queue = "default";
    @Nullable
    private String targetUrl = null;
    @Nullable
    private String discordUserId = null;

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
        SILENT,
        DONE
    }

    public Optional<String> getTargetUrl() {
        return Optional.ofNullable(targetUrl);
    }

    public Optional<String> getDiscordUserId() {
        return Optional.ofNullable(discordUserId);
    }

    public Optional<String> getExplain() {
        return Optional.ofNullable(explain);
    }

}