package dev.digitaldragon.jobs;

import dev.digitaldragon.interfaces.UserErrorException;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Args;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Job;
import lombok.Getter;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

@Getter
public class MediaWiki extends Wiki {
    private String givenUrl;
    private String apiUrl = null;
    private String indexUrl = null;
    private Long totalMediaSize = null;
    private String version = null;

    public MediaWiki(String givenUrl) {
        this.givenUrl = givenUrl;
    }


    @Override
    public Optional<Job> getJob() {
        return Optional.empty();
    }

    @Override
    public String getUnsafeReason() {
        if (getTotalMediaSize() > /* 300GiB */ 300L * 1024 * 1024 * 1024) {
            return "Wiki media size is more than 300GiB. If you're sure, please start this job manually or with --suppress-safety-check.";
        }

        if (getTotalRevisions() > 300_000) {
            return "Wiki has more than 300,000 revisions. If you're sure, please start this job manually or with --suppress-safety-check.";
        }

        return null;
    }

    @Override
    public Optional<Job> run(String url, String username, String explain) throws UserErrorException {
        try {
            return run(Jsoup.connect(url).get(), username, explain);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Job> run(Document document, String username, String explain) throws UserErrorException {
        throw new UserErrorException("MediaWiki!");
    }

    public String run(String username, String explain) throws UserErrorException {
        if (!findInfo()) {
            throw new UserErrorException("Couldn't find a compatible wiki at the given URL.");
        }
        WikiTeam3Args args = new WikiTeam3Args();
        args.setImages(true);
        args.setXml(true);
        args.setApi(apiUrl);
        args.setIndex(indexUrl);
        JobMeta meta = new JobMeta(username);
        meta.setExplain(explain);

        String version = getVersion();
        if (version == null) {
            throw new UserErrorException("Couldn't find the MediaWiki version.");
        }// check if mw version > 1.27 (1.27 good, 1.27.1 good, 1.23.1 bad)
        String[] versionParts = version.split("\\.");
        if (versionParts.length < 2) {
            throw new UserErrorException("Couldn't find the MediaWiki version.");
        }
        if (Integer.parseInt(versionParts[1]) >= 27) {
            args.setXmlRevisions(true);
        } else {
            args.setXmlApiExport(true);
        }
        Job job = new WikiTeam3Job(args, meta, UUID.randomUUID().toString());
        JobManager.submit(job);
        return job.getId();
    }

    public int getMajorVersion() {
        String version = getVersion();
        if (version == null) {
            return -1;
        }
        String[] versionParts = version.split("\\.");
        if (versionParts.length < 2) {
            return -1;
        }
        return Integer.parseInt(versionParts[0]);
    }

    private WikiTeam3Args getArgs() throws UserErrorException {
        WikiTeam3Args args = new WikiTeam3Args();
        args.setImages(true);
        args.setXml(true);
        args.setApi(apiUrl);
        args.setIndex(indexUrl);

        String version = getVersion();
        if (version == null) {
            throw new UserErrorException("Couldn't find the MediaWiki version.");
        }
        String[] versionParts = version.split("\\.");
        if (versionParts.length < 2) {
            throw new UserErrorException("Couldn't find the MediaWiki version.");
        }
        if (Integer.parseInt(versionParts[1]) >= 27) { // Cutoff for xmlrevisions support
            args.setXmlRevisions(true);
        } else {
            args.setXmlApiExport(true);
        }

        return args;
    }

    public String getVersion() {
        if (version != null) {
            return version;
        }
        //get the version from the api
        try {
            String response = readStringFromURL(apiUrl + "?action=query&meta=siteinfo&siprop=general&format=json");
            JSONObject obj = new JSONObject(response);
            JSONObject query = obj.getJSONObject("query");
            JSONObject pages = query.getJSONObject("general");
            version = pages.getString("generator");
            return version;
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    public int getTotalRevisions() {
        //get the total number of revisions from the api
        try {
            String response = readStringFromURL(apiUrl + "?action=query&meta=siteinfo&siprop=statistics&format=json");
            JSONObject obj = new JSONObject(response);
            JSONObject query = obj.getJSONObject("query");
            JSONObject pages = query.getJSONObject("statistics");
            return pages.getInt("edits");
        } catch (IOException | JSONException e) {
            return -1;
        }
    }

    public Long getTotalMediaSize() {
        if (totalMediaSize != null) {
            return totalMediaSize;
        }
        try {
            Document document = Jsoup.connect(indexUrl + "?title=Special:MediaStatistics&uselang=qqx").get();
            // find <p> with text (mediastatistics-allbytes: 572,239,085, (size-megabytes), 1,610)
            String text = document.select("p:contains(mediastatistics-allbytes)").text();
            String[] parts = text.split(" ");
            if (parts.length < 2) throw new Exception("No size found");
            String size = parts[1].replace(",", "");
            totalMediaSize = Long.parseLong(size);
            return totalMediaSize;
        } catch (Exception e) {
            // continue
        }
        // https://www.mediawiki.org/w/api.php?action=query&prop=imageinfo&generator=allpages&gapnamespace=6&iiprop=size&format=json
        // sum size of every media file from query.pages.PAGEID.imageinfo.size
        String nextUrl = apiUrl + "?action=query&prop=imageinfo&generator=allpages&gapnamespace=6&iiprop=size&format=json&gaplimit=500";
        long totalSize = 0;
        while (nextUrl != null) {
            System.out.println(nextUrl);
            try {
                String response = readStringFromURL(nextUrl);
                System.out.println("\t" + response);
                JSONObject obj = new JSONObject(response);
                JSONObject query = obj.getJSONObject("query");
                JSONObject pages = query.getJSONObject("pages");
                for (String key : pages.keySet()) {
                    JSONObject page = pages.getJSONObject(key);
                    if (page.has("imageinfo")) {
                        JSONObject imageinfo = page.getJSONArray("imageinfo").getJSONObject(0);
                        totalSize += imageinfo.getLong("size");
                    }
                }
                if (obj.has("continue")) {
                    Thread.sleep(1000); //ratelimit
                    nextUrl = apiUrl + "?action=query&prop=imageinfo&generator=allpages&gapnamespace=6&iiprop=size&format=json&gaplimit=500&gapcontinue=" + obj.getJSONObject("continue").getString("gapcontinue");
                } else {
                    nextUrl = null;
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return -1L;
            } catch (JSONException e) {
                return 0L;
            }
        }
        totalMediaSize = totalSize;
        return totalMediaSize;

    }

    public boolean findInfo() {
        try {
            Document document = Jsoup.connect(givenUrl).get();
            // find the api.php url

            // METHOD 1: Look for direct links to api.php
            String apiUrl = findApiDirectly(document);
            if (apiUrl != null) this.apiUrl = apiUrl;
            System.out.println("Load api: " + apiUrl);

            // METHOD 2: Look for load.php and replace with api.php
            if (this.apiUrl == null) apiUrl = findApiFromLoad(document); //todo: resolves relative with the page and "null".
            if (apiUrl != null) this.apiUrl = apiUrl;

            System.out.println("Load api: " + apiUrl);

            // METHOD 3: Fetch a random onsite page and try method 1 and 2 again.
            if(this.apiUrl == null ) apiUrl = findApiFromRandomPage(document);
            if (apiUrl != null) this.apiUrl = apiUrl;

            System.out.println("Load api: " + apiUrl);


            if (this.apiUrl == null) return false; // no api.php found
            //further possible methods: detecting api from login buttons, edit buttons, etc.

            // find the index.php url
            // method 1: replace api.php with index.php
            String indexUrl = apiUrl.replace("api.php", "index.php");
            if (!testIndex(indexUrl)) return false; //TODO: more methods

            this.indexUrl = indexUrl;

            return true;

            // find the index.php url
            //indexUrl = document.head().select("link[href*='index.php']").attr("href");
        } catch (IOException e) {
            return false;
        }
    }

    private String findApiDirectly(Document document) throws MalformedURLException {
        Element apiLink = document.head().select("link[href*='api.php']").first();
        if (apiLink == null) return null;
        String url = apiLink.attr("href");
        String cleanUrl = cleanUrl(document.location(), url);
        if (!testApi(cleanUrl)) return null;
        return cleanUrl;
    }

    // Method 2: look for load.php and replace with api.php
    private String findApiFromLoad(Document document) throws MalformedURLException {
        Element loadLink = document.head().select("link[href*='load.php']").first();
        if (loadLink == null) return null;
        String url = loadLink.attr("href");
        String cleanUrl = cleanUrl(document.location(), url);
        String apiUrl = cleanUrl.replace("load.php", "api.php");
        if (!testApi(apiUrl)) return null;
        return apiUrl;
    }

    // Method 3: Fetch a random onsite page and try method 1 and 2 again.
    private String findApiFromRandomPage(Document document) throws IOException {
        String randomPage = document.head().select("link[href*='Special:Random']").attr("href");
        Document randomDocument = Jsoup.connect(randomPage).get(); //todo this doesnt work
        String apiUrl = findApiDirectly(randomDocument);
        if (apiUrl != null) return apiUrl;
        apiUrl = findApiFromLoad(randomDocument);
        return apiUrl;
    }

    private boolean testApi(String apiUrl) {

        // test the api.php url
        // load basic info from ?action=query&meta=siteinfo

        try {
            String response = readStringFromURL(apiUrl + "?action=query&meta=siteinfo&format=json");
            JSONObject obj = new JSONObject(response);

            // find the index.php url if it exists
            JSONObject query = obj.getJSONObject("query");
            if (query.getJSONObject("general").getString("script") != null) {
                return true;
            }

        } catch (IOException | JSONException e) {
            return false;
        }
        return false;
    }

    private boolean testIndex(String indexUrl) {
        // test the index.php url
        // look for <meta name="generator" content="MediaWiki 1.37.1">

        try {
            String response = readStringFromURL(indexUrl + "?title=Special:Version");
            return response.contains("MediaWiki");
        } catch (IOException e) {
            return false;
        }
    }

    public static String readStringFromURL(String requestURL) throws IOException {
        try (Scanner scanner = new Scanner(
                new URL(requestURL).openStream(), StandardCharsets.UTF_8
        )) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private String cleanUrl(String baseUrl, String url) throws MalformedURLException {
        System.out.println(baseUrl);
        System.out.println(url);
        //resolve relative url
        URL base = new URL(baseUrl);
        URL resolved = new URL(base, url);
        // chop off the query string
        return resolved.toString().split("\\?")[0];

    }


}
