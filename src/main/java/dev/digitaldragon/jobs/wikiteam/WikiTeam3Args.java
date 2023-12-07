package dev.digitaldragon.jobs.wikiteam;

import com.beust.jcommander.Parameter;
import dev.digitaldragon.interfaces.UserErrorException;
import lombok.Getter;
import lombok.Setter;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class WikiTeam3Args {
    //Description field omitted here because the JCommander help is never shown anywhere.
    @Parameter(names = {"--delay", "-d"})
    private Double delay;
    @Parameter(names = {"--retries", "-R"})
    private int retries;
    @Parameter(names = {"--api_chunksize", "-C"})
    private int apiChunkSize;
    @Parameter(names = {"--index-check-threshold", "-T"})
    private double indexCheckThreshold;
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
    @Parameter(names = {"--api", "-A"})
    private String api;
    @Parameter(names = {"--index", "-N"})
    private String index;
    @Parameter(names = {"--url", "-u"}, variableArity = true)
    private String url;

    /**
     * This method checks the validity of three URL options - api, index, and url.
     * If any of these options are invalid, a UserErrorException is thrown.
     *
     * @throws UserErrorException if any of the URL options are invalid
     */
    public void check() throws UserErrorException {
        checkUrlOption(api);
        checkUrlOption(index);
        checkUrlOption(url);
    }

    private void checkUrlOption(String option) throws UserErrorException {
        if (option == null || option.isEmpty()) {
            return;
        }

        try {
            new URL(option);
        } catch (MalformedURLException e) {
            throw new UserErrorException("Invalid URL in options: " + option);
        }
    }

    /**
     * Returns a string representation of the parsed arguments in a format compatible with WikiTeam3.
     * <p>
     * The method parses the arguments into a string and converts the short versions of the arguments
     * to their long versions, as WikiTeam3 only uses the long versions of the arguments.
     * <p>
     * Make sure you call {@link #check()} before calling this method to verify the validity of all
     * options and gracefully hand errors back to the user. If this is not done, it may result in a {@link RuntimeException}.
     *
     * @return A string representation of the parsed arguments compatible with wikiteam3.
     */
    public String[] get() {
        //parse the args into a string compatible with wikiteam3. Wikiteam3 only uses the long version of the args, so we have to convert the short versions to the long versions.
        List<String> args = new ArrayList<>();
        args.add("wikiteam3dumpgenerator");

        parseDoubleOption(args, delay, "--delay");
        parseDoubleOption(args, indexCheckThreshold, "--index-check-threshold");

        parseIntOption(args, retries, "--retries");
        parseIntOption(args, apiChunkSize, "--api_chunksize");

        parseBooleanOption(args, xml, "--xml");
        parseBooleanOption(args, xmlApiExport, "--xmlapiexport");
        parseBooleanOption(args, xmlRevisions, "--xmlrevisions");
        parseBooleanOption(args, images, "--images");
        parseBooleanOption(args, bypassCdnImageCompression, "--bypass-cdn-image-compression");
        parseBooleanOption(args, disableImageVerify, "--disable-image-verify");
        parseBooleanOption(args, currentOnly, "--curonly");
        parseBooleanOption(args, force, "--force");
        parseBooleanOption(args, warcImages, "--warc-images");
        parseBooleanOption(args, warcPages, "--warc-pages");
        parseBooleanOption(args, warcPagesHistory, "--warc-pages-history");

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
