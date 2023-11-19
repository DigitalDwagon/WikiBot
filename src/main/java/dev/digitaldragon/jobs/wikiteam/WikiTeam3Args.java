package dev.digitaldragon.jobs.wikiteam;

import com.beust.jcommander.Parameter;
import dev.digitaldragon.interfaces.UserErrorException;
import lombok.Getter;
import lombok.Setter;

import java.net.MalformedURLException;
import java.net.URL;

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
    private int indexCheckThreshold;
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
    public String get() {
        //parse the args into a string compatible with wikiteam3. Wikiteam3 only uses the long version of the args, so we have to convert the short versions to the long versions.
        StringBuilder sb = new StringBuilder();

        parseDoubleOption(sb, delay, "--delay");

        parseIntOption(sb, retries, "--retries");
        parseIntOption(sb, apiChunkSize, "--api-chunksize");
        parseIntOption(sb, indexCheckThreshold, "--index-check-threshold");

        parseBooleanOption(sb, xml, "--xml");
        parseBooleanOption(sb, xmlApiExport, "--xmlapiexport");
        parseBooleanOption(sb, xmlRevisions, "--xmlrevisions");
        parseBooleanOption(sb, images, "--images");
        parseBooleanOption(sb, bypassCdnImageCompression, "--bypass-cdn-image-compression");
        parseBooleanOption(sb, disableImageVerify, "--disable-image-verify");
        parseBooleanOption(sb, currentOnly, "--curonly");
        parseBooleanOption(sb, force, "--force");
        parseBooleanOption(sb, warcImages, "--warc-images");
        parseBooleanOption(sb, warcPages, "--warc-pages");
        parseBooleanOption(sb, warcPagesHistory, "--warc-pages-history");

        parseUrlOption(sb, api, "--api");
        parseUrlOption(sb, index, "--index");
        parseUrlOption(sb, url, "");

        return sb.toString();
    }

    private void parseBooleanOption(StringBuilder sb, boolean option, String longOption) {
        if (option) {
            sb.append(longOption).append(" ");
        }
    }

    private void parseIntOption(StringBuilder sb, int option, String longOption) {
        if (option != 0) {
            sb.append(longOption).append(" ").append(option).append(" ");
        }
    }

    private void parseDoubleOption(StringBuilder sb, Double option, String longOption) {
        if (option == null) {
            return;
        }

        if (option != 0) {
            sb.append(longOption).append(" ").append(option).append(" ");
        }
    }

    private void parseUrlOption(StringBuilder sb, String option, String longOption) {
        if (option == null || option.isEmpty()) {
            return;
        }

        try {
            new URL(option);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL in options at get time: " + option + ". Did you run the check() method after the user handed it in?");
        }

        sb.append(longOption).append(!longOption.isEmpty() ? " " : "" ).append(option).append(" ");
    }
}
