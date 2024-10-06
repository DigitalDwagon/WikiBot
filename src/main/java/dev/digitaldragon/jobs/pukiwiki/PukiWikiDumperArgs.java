package dev.digitaldragon.jobs.pukiwiki;

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
public class PukiWikiDumperArgs {
    //Description field omitted here because the JCommander help is never shown anywhere.
    @Parameter(names = {"--url"})
    private String url;
    @Parameter(names = {"--explain", "-e"})
    private String explain;
    @Parameter(names = {"--resume"})
    private String resume;
/*
PS C:\Users\Digital> pukiwikidumper --help
usage: pukiWikiDumper [-h] [--content] [--media] [--current-only] [--path PATH] [--no-resume] [--threads THREADS]
                      [--i-love-retro] [--insecure] [--ignore-errors] [--ignore-action-disabled-edit]
                      [--trim-php-warnings] [--delay DELAY] [--retry RETRY] [--hard-retry HARD_RETRY]
                      [--parser PARSER] [--verbose] [--cookies COOKIES] [--auto] [-u] [-g UPLOADER_ARGS] [--force]
                      url

pukiWikiDumper Version: 0.0.14

positional arguments:
  url                   URL of the dokuWiki (provide the doku.php URL)

options:
  -h, --help            show this help message and exit
  --current-only        Dump latest revision, no history [default: false]
  --path PATH           Specify dump directory [default: <site>-<date>]
  --no-resume           Do not resume a previous dump [default: resume]
  --threads THREADS     Number of sub threads to use [default: 1], not recommended to set > 5
  --i-love-retro        Do not check the latest version of pukiWikiDumper (from pypi.org) before running [default:
                        False]
  --insecure            Disable SSL certificate verification
  --ignore-errors       !DANGEROUS! ignore errors in the sub threads. This may cause incomplete dumps.
  --ignore-action-disabled-edit
                        Some sites disable edit action for anonymous users and some core pages. This option will
                        ignore this error and textarea not found error.But you may only get a partial dump. (only
                        works with --content)
  --trim-php-warnings   Trim PHP warnings from requests.Response.text
  --delay DELAY         Delay between requests [default: 0.0]
  --retry RETRY         Maximum number of retries [default: 5]
  --hard-retry HARD_RETRY
                        Maximum number of retries for hard errors [default: 3]
  --parser PARSER       HTML parser [default: html.parser]
  --verbose             Verbose output
  --cookies COOKIES     cookies file
  --auto                dump: content+media, threads=2, current-only. (threads is overridable)
  -u, --upload          Upload wikidump to Internet Archive after successfully dumped (only works with --auto)
  -g UPLOADER_ARGS, --uploader-arg UPLOADER_ARGS
                        Arguments for uploader.
  --force               To dump even if a recent dump exists on IA

Data to download:
  What info download from the wiki

  --content             Dump content
  --media               Dump media
 */
    @Parameter(names = {"--current-only"})
    private boolean currentOnly;
    /*@Parameter(names = {"--path"})
    private String path;
    @Parameter(names = {"--no-resume"})
    private boolean noResume;*/
    @Parameter(names = {"--threads"})
    private int threads;
    @Parameter(names = {"--i-love-retro"})
    private boolean iLoveRetro;
    @Parameter(names = {"--insecure"})
    private boolean insecure;
    /*@Parameter(names = {"--ignore-errors"})
    private boolean ignoreErrors;*/
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
    /*@Parameter(names = {"--parser"})
    private String parser;*/
    @Parameter(names = {"--verbose"})
    private boolean verbose;
    /*@Parameter(names = {"--cookies"})
    private String cookies;*/
    @Parameter(names = {"--auto"})
    private boolean auto;
    /*@Parameter(names = {"--upload", "-u"})
    private boolean upload;
    @Parameter(names = {"--uploader-arg", "-g"})
    private String uploaderArg;*/
    @Parameter(names = {"--force"})
    private boolean force;
    @Parameter(names = {"--silent-mode"})
    private String silentMode;
    @Parameter(names = {"--queue"})
    private String queue;

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
