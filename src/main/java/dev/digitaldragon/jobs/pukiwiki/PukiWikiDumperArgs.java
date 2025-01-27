package dev.digitaldragon.jobs.pukiwiki;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.jobs.JobMeta;
import lombok.Getter;
import lombok.Setter;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PukiWikiDumperArgs {
    @Parameter(names = {"--url"})
    private String url;
    @Parameter(names = {"--resume"})
    private String resume;
    @Parameter(names = {"--current-only"})
    private boolean currentOnly;
    @Parameter(names = {"--threads"})
    private int threads;
    @Parameter(names = {"--i-love-retro"})
    private boolean iLoveRetro;
    @Parameter(names = {"--insecure"})
    private boolean insecure;
    @Parameter(names = {"--ignore-action-disabled-edit"})
    private boolean ignoreActionDisabledEdit;
    @Parameter(names = {"--trim-php-warnings"})
    private boolean trimPhpWarnings;
    @Parameter(names = {"--delay"})
    private Double delay;
    @Parameter(names = {"--retry"})
    private int retry;
    @Parameter(names = {"--hard-retry"})
    private int hardRetry;
    @Parameter(names = {"--verbose"})
    private boolean verbose;
    @Parameter(names = {"--auto"})
    private boolean auto;
    @Parameter(names = {"--force"})
    private boolean force;

    public PukiWikiDumperArgs() {}

    public PukiWikiDumperArgs(String[] args, JobMeta meta) throws ParameterException, ParseException {
        JCommander commander = JCommander.newBuilder()
                .addObject(meta)
                .addObject(this)
                .build();

        commander.parse(args);
    }

    public List<String> get() {
        List<String> args = new ArrayList<>();
        args.add("pukiWikiDumper");
        parseBooleanOption(args, currentOnly, "--current-only");
        parseIntOption(args, threads, "--threads");
        parseBooleanOption(args, iLoveRetro, "--i-love-retro");
        parseBooleanOption(args, insecure, "--insecure");
        parseBooleanOption(args, ignoreActionDisabledEdit, "--ignore-action-disabled-edit");
        parseBooleanOption(args, trimPhpWarnings, "--trim-php-warnings");
        parseDoubleOption(args, delay, "--delay");
        parseIntOption(args, retry, "--retry");
        parseIntOption(args, hardRetry, "--hard-retry");
        //parseUrlOption(args, parser, "--parser");
        parseBooleanOption(args, verbose, "--verbose");
        //parseUrlOption(args, cookies, "--cookies");
        parseBooleanOption(args, auto, "--auto");
        parseBooleanOption(args, force, "--force");
        parseUrlOption(args, url, "");


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
