package dev.digitaldragon.jobs.dokuwiki;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.util.URLValidator;
import dev.digitaldragon.util.UserAgentParser;
import lombok.Getter;
import lombok.Setter;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DokuWikiDumperArgs {
    //Description field omitted here because the JCommander help is never shown anywhere.
    @Parameter(names = {"--url"}, validateWith = URLValidator.class)
    private String url;
    @Parameter(names = {"--auto"})
    private boolean auto;
    @Parameter(names = {"--retry"})
    private int retry;
    @Parameter(names = {"--hard-retry"})
    private int hardRetry;
    @Parameter(names = {"--ignore-disposition-header-missing"})
    private boolean ignoreDispositionHeaderMissing;
    @Parameter(names = {"--delay"})
    private Double delay;
    @Parameter(names = {"--threads"})
    private int threads;
    @Parameter(names = {"--ignore-action-disabled-edit"})
    private boolean ignoreActionDisabledEdit;
    @Parameter(names = {"--insecure"})
    private boolean insecure;
    @Parameter(names = {"--current-only"})
    private boolean currentOnly;
    @Parameter(names = {"--force"})
    private boolean force;
    @Parameter(names = {"--content"})
    private boolean content;
    @Parameter(names = {"--media"})
    private boolean media;
    @Parameter(names = {"--html"})
    private boolean html;
    @Parameter(names = {"--pdf"})
    private boolean pdf;
    @Parameter(names = {"--resume"})
    private String resume;
    @Parameter(names = {"--user-agent", "-u"}, converter = UserAgentParser.class)
    private String userAgent = WikiBot.getConfig().getDokuWikiDumperConfig().userAgent();

    public DokuWikiDumperArgs() {}

    public DokuWikiDumperArgs(String[] args, JobMeta meta) throws ParameterException, ParseException {
        JCommander commander = JCommander.newBuilder()
                .addObject(meta)
                .addObject(this)
                .build();

        commander.parse(args);
    }

    public List<String> get() {
        //parse the args into a string compatible with wikiteam3. Wikiteam3 only uses the long version of the args, so we have to convert the short versions to the long versions.
        List<String> args = new ArrayList<>();
        args.add("dokuWikiDumper");

        parseUrlOption(args, url, "");

        parseIntOption(args, retry, "--retry");
        parseIntOption(args, hardRetry, "--hard-retry");
        parseIntOption(args, threads, "--threads");

        parseDoubleOption(args, delay, "--delay");

        parseBooleanOption(args, auto, "--auto");
        parseBooleanOption(args, ignoreActionDisabledEdit, "--ignore-action-disabled-edit");
        parseBooleanOption(args, ignoreDispositionHeaderMissing, "--ignore-disposition-header-missing");
        parseBooleanOption(args, insecure, "--insecure");
        parseBooleanOption(args, force, "--force");
        parseBooleanOption(args, currentOnly, "--current-only");
        parseBooleanOption(args, content, "--content");
        parseBooleanOption(args, media, "--media");
        parseBooleanOption(args, html, "--html");
        parseBooleanOption(args, pdf, "--pdf");

        parseStringOption(args, userAgent, "--user-agent");

        return args;
    }

    private void parseBooleanOption(List<String> args, boolean option, String longOption) {
        if (option) {
            args.add(longOption);
        }
    }

    private void parseIntOption(List<String> args, int option, String longOption) {
        if (option != 0) {
            args.add(longOption);
            args.add(Integer.toString(option));
        }
    }

    private void parseDoubleOption(List<String> args, Double option, String longOption) {
        if (option == null) {
            return;
        }
        args.add(longOption);
        args.add(option.toString());
    }

    private void parseUrlOption(List<String> args, String option, String longOption) {
        if (option == null || option.isEmpty()) {
            return;
        }

        try {
            new URL(option);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL in options at get time: " + option + ". Did you run the check() method after the user handed it in?");
        }

        if (!longOption.isEmpty()) {
            args.add(longOption);
        }
        args.add(option);
    }

    private void parseStringOption(List<String> args, String option, String longOption) {
        if (option == null || option.isEmpty()) {
            return;
        }

        args.add(longOption);
        args.add(option);
    }
}
