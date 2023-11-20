package dev.digitaldragon.jobs.dokuwiki;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DokuWikiDumperArgs {
    //Description field omitted here because the JCommander help is never shown anywhere.
    @Parameter(names = {"--ignore-disposition-header-missing", "-D"})
    private boolean ignoreDispositionHeaderMissing;
    @Parameter(names = {"--retry", "-R"})
    private int retry;
    @Parameter(names = {"--delay", "-d"})
    private double delay;

}
