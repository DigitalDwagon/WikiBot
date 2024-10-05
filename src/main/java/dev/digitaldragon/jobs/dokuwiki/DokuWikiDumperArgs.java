package dev.digitaldragon.jobs.dokuwiki;

import com.beust.jcommander.Parameter;
import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.jobs.JobMeta;
import lombok.Getter;
import lombok.Setter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Getter
@Setter
public class DokuWikiDumperArgs {
    //Description field omitted here because the JCommander help is never shown anywhere.
    @Parameter(names = {"--url"})
    private String url;
    @Parameter(names = {"--explain", "-e"})
    private String explain;
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
    @Parameter(names = {"--silent-mode"})
    private String silentMode;
    @Parameter(names = {"--resume"})
    private String resume;


    /**
     * This method checks the validity of three URL options - api, index, and url.
     * If any of these options are invalid, a UserErrorException is thrown.
     *
     * @throws UserErrorException if any of the URL options are invalid
     */
    public void check() throws UserErrorException {
        if (url == null) {
            throw new UserErrorException("You need to specify a URL!");
        }

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new UserErrorException("Invalid URL! Hint: make sure you include the protocol (http:// or https://)");
        }

        try {
            if (silentMode != null) {
                silentMode = silentMode.toUpperCase(Locale.ENGLISH);
                JobMeta.SilentMode.valueOf(silentMode);
            }
        } catch (IllegalArgumentException e) {
            throw new UserErrorException("Invalid --silent-mode - it must be one of: " + Arrays.toString(JobMeta.SilentMode.values()));
        }
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

}
