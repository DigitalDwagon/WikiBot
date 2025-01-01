package dev.digitaldragon.jobs.mediawiki;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import dev.digitaldragon.WikiBot;
import dev.digitaldragon.util.SilentModeValidator;
import dev.digitaldragon.util.URLValidator;
import lombok.Getter;
import lombok.Setter;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class WikiTeam3Args {
    //Description field omitted here because the JCommander help is never shown anywhere.
    @Parameter(names = {"--delay", "-d"})
    private Double delay = 1.5;
    @Parameter(names = {"--retries", "-R"})
    private int retries;
    @Parameter(names = {"--hard-retries", "-H"})
    private int hardRetries;
    @Parameter(names = {"--api_chunksize", "-C"})
    private int apiChunkSize;
    @Parameter(names = {"--index-check-threshold", "-T"})
    private Double indexCheckThreshold = null;
    @Parameter(names = {"--xml", "-x"})
    private boolean xml;
    @Parameter(names = {"--xmlapiexport", "-a"})
    private boolean xmlApiExport;
    @Parameter(names = {"--xmlrevisions", "-r"})
    private boolean xmlRevisions;
    @Parameter(names = {"--images", "-i"})
    private boolean images;
    @Parameter(names = {"--bypass-cdn-image-compression", "-c"})
    private boolean bypassCdnImageCompression;
    @Parameter(names = {"--disable-image-verify", "-V"})
    private boolean disableImageVerify;
    @Parameter(names = {"--curonly", "-n"})
    private boolean currentOnly;
    @Parameter(names = {"--insecure", "-k"})
    private boolean insecure;
    @Parameter(names = {"--force", "-F"})
    private boolean force;
    @Parameter(names = {"--warc-images", "-I"})
    private boolean warcImages;
    @Parameter(names = {"--warc-pages", "-P"})
    private boolean warcPages;
    @Parameter(names = {"--warc-pages-history", "-p", "-X"})
    private boolean warcPagesHistory;
    @Parameter(names = {"--explain", "-e"})
    private String explain;
    @Parameter(names = {"--resume"})
    private String resume;
    @Parameter(names = {"--api", "-A"}, validateWith = URLValidator.class)
    private String api;
    @Parameter(names = {"--index", "-N"}, validateWith = URLValidator.class)
    private String index;
    @Parameter(names = {"--url", "-u"}, variableArity = true, validateWith = URLValidator.class)
    private String url;
    @Parameter(names = {"--warc-not-for-production"})
    private boolean warc;
    @Parameter(names = {"--warconly"})
    private boolean warcOnly;
    @Parameter(names = {"--silent-mode"}, validateWith = SilentModeValidator.class)
    private String silentMode = null;
    @Parameter(names = {"--queue"})
    private String queue;
    @Parameter(names = {"--redirects"})
    private boolean redirects;

    public WikiTeam3Args() {}

    public WikiTeam3Args(String[] args) throws ParameterException, ParseException {
        JCommander commander = JCommander.newBuilder()
                .addObject(this)
                .build();

        commander.parse(args);
    }

    /**
     * Returns a string representation of the parsed arguments in a format compatible with WikiTeam3.
     * <p>
     * The method parses the arguments into a string and converts the short versions of the arguments
     * to their long versions, as WikiTeam3 only uses the long versions of the arguments.
     *
     * @return A string representation of the parsed arguments compatible with wikiteam3.
     */
    public String[] get() {
        //parse the args into a string compatible with wikiteam3. Wikiteam3 only uses the long version of the args, so we have to convert the short versions to the long versions.
        List<String> args = new ArrayList<>();
        args.add("wikiteam3dumpgenerator");
        args.add("--user-agent");
        args.add(WikiBot.getConfig().getWikiTeam3Config().userAgent());

        parseDoubleOption(args, delay, "--delay");
        parseDoubleOption(args, indexCheckThreshold, "--index-check-threshold");

        parseIntOption(args, retries, "--retries");
        parseIntOption(args, hardRetries, "--hard-retries");
        parseIntOption(args, apiChunkSize, "--api_chunksize");

        parseBooleanOption(args, xml, "--xml");
        parseBooleanOption(args, xmlApiExport, "--xmlapiexport");
        parseBooleanOption(args, xmlRevisions, "--xmlrevisions");
        parseBooleanOption(args, images, "--images");
        parseBooleanOption(args, bypassCdnImageCompression, "--bypass-cdn-image-compression");
        parseBooleanOption(args, disableImageVerify, "--disable-image-verify");
        parseBooleanOption(args, currentOnly, "--curonly");
        parseBooleanOption(args, insecure, "--insecure");
        parseBooleanOption(args, force, "--force");
        parseBooleanOption(args, warcImages, "--warc-images");
        parseBooleanOption(args, warcPages, "--warc-pages");
        parseBooleanOption(args, warcPagesHistory, "--warc-pages-history");
        parseBooleanOption(args, redirects, "--redirects");

        parseUrlOption(args, api, "--api");
        parseUrlOption(args, index, "--index");
        parseUrlOption(args, url, "");

        return args.toArray(new String[0]);
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
