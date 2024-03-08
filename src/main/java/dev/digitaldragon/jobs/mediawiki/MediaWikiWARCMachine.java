package dev.digitaldragon.jobs.mediawiki;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.interfaces.api.Dashboard;
import dev.digitaldragon.jobs.CommonTasks;
import dev.digitaldragon.jobs.RunCommand;
import dev.digitaldragon.jobs.StringLogHandler;
import dev.digitaldragon.util.Config;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import java.util.Scanner;

public class MediaWikiWARCMachine {
    private String apiUrl;
    private StringLogHandler handler;
    private File jobDir;
    private File warcFile;
    private File urlsFile;

    public MediaWikiWARCMachine(String apiUrl, StringLogHandler handler, File jobDir, File warcFile, File urlsFile) {
        this.apiUrl = apiUrl;
        this.handler = handler;
        this.jobDir = jobDir;
        this.warcFile = warcFile;
        this.urlsFile = urlsFile;
    }

    /**
     * Writes a list of all page URLs on the MediaWiki instance to a file, then downloads them to a WARC with wget-at.
     */
    public String run() {
        try {
            for (String namespace : getNamespaces()) {
                fetchPagesInNamespace(namespace);
            }
        } catch (IOException e) {
            e.printStackTrace();
           return "WARC grab aborted due to an error listing pages: " + e.getMessage();
        }

        File wgetLuaHook = new File(jobDir, "mediawiki.lua");
        try (InputStream stream = MediaWikiWARCMachine.class.getClassLoader().getResourceAsStream("mediawiki.lua")) {
               //write stream to file
            if (stream == null) {
                return "Couldn't find Lua hook file";
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(wgetLuaHook))) {
                writer.write(new String(stream.readAllBytes()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Couldn't write Lua hook file: ";
        }

        //get domain name from api url
        String domain = apiUrl.substring(apiUrl.indexOf("://") + 3, apiUrl.indexOf("/", apiUrl.indexOf("://") + 3));


        String[] wgetArgs = new String[]{
                "wget-at",
                "-w", "1", //wait 0.5 seconds between requests
                //"--random-wait", // vary the wait time 0.5-1.5x to the wait time
                "-U", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 (compatible; Wikibot/1.2.1; wikibot@digitaldragon.dev; +https://wikibot.digitaldragon.dev/)",
                "--host-lookups", "dns",
                "--hosts-file", "/dev/null",
                "--resolvconf-file", "/dev/null",
                "--dns-servers", "9.9.9.9,9.9.9.10,2620:fe::10,2620:fe::fe:10",
                "--reject-reserved-subnets",
                "--prefer-family", "IPv6",
                "--content-on-error",
                "--max-redirect", "20",
                "--lua-script", wgetLuaHook.getAbsolutePath(),
                "-o", "wget.log", // Instead of logging to a file, we capture stderror for logs.
                //"--no-check-certificate",
                "--output-document", "wget.tmp",
                //"--truncate-output",
                "-e", "robots=off",
                //"--recursive", "--level=inf","
                "--page-requisites",
                "--timeout", "60",
                "--tries", "2",
                "--waitretry", "0",
                "--warc-file", domain,
                "--warc-header", "operator: DigitalDragon",
                "--warc-header", "x-wikibot-version: " + WikiBot.getVersion(),
                "--warc-dedup-url-agnostic",
                "--warc-compression-use-zstd",
                //"--warc-zstd-dict-no-include",
                "--header", "Connection: keep-alive",
                "--header", "Accept-Language: en-US;q=0.9, en;q=0.8",
                "--header", "Accept: text/html",
                //"https://archiveteam.invalid/wikibot-input-file/" + urlsFile.getAbsolutePath(),
                "-i", urlsFile.getAbsolutePath(),

        };


        RunCommand wgetAtCommand = new RunCommand(null, wgetArgs, jobDir, handler);
        handler.onMessage("----- Bot: Task Wget-AT started -----");

        CommonTasks.runAndVerify(wgetAtCommand, handler, "Wget-AT");



        return null;
    }

    /**
     * Fetches the namespace IDs from the MediaWiki instance.
     */
    public List<String> getNamespaces() throws IOException{
        List<String> namespaces = new ArrayList<>();
        String response = readStringFromURL(apiUrl + "?action=query&meta=siteinfo&siprop=namespaces&format=json");
        JSONObject obj = new JSONObject(response);

        JSONObject query = obj.getJSONObject("query");
        JSONObject namespacesObj = query.getJSONObject("namespaces");
        namespaces.addAll(namespacesObj.keySet());
        return namespaces;

    }

    /**
     * Fetches all pages in a given namespace from the MediaWiki instance, and calls fetchUrlsForPage for each page.
     */
    public void fetchPagesInNamespace(String namespace) {
        boolean finished = false;
        String gapContinue = "";
        String apiQuery = "?action=query&generator=allpages&format=json&gaplimit=50&prop=info&inprop=url&gapnamespace=" + namespace;
        while (!finished) {
            String url = apiUrl + apiQuery + gapContinue;
            try {
                String json = readStringFromURL(url);
                JSONObject obj = new JSONObject(json);
                System.out.println(obj);
                JSONObject query = obj.getJSONObject("query");
                JSONObject pages = query.getJSONObject("pages");
                for (String key : pages.keySet()) {
                    JSONObject page = pages.getJSONObject(key);
                    String fullUrl = page.getString("fullurl");
                    String editUrl = page.getString("editurl");
                    fetchUrlsForPage(fullUrl, editUrl);
                }
                if (obj.has("query-continue")) {
                    JSONObject cont = obj.getJSONObject("query-continue").getJSONObject("allpages");
                    gapContinue = "&gapcontinue=" + cont.getString("gapcontinue");

                    Thread.sleep(1000);
                } else {
                    finished = true;
                }
                System.out.println("new request: " + url);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                //assume the namespace is empty or invalid
                finished = true;
            }
        }
    }

    public void fetchUrlsForPage(String pageUrl, String editUrl) {
        writeLineToFile(urlsFile, pageUrl);
        String url = editUrl.replace("action=edit", "action=history");
        while (true) {
            writeLineToFile(urlsFile, url);
            try {
                Document document = Jsoup.connect(url).get();
                String finalUrl = url;
                document.getElementsByClass("mw-changeslist-date").forEach((element) -> {
                    String href = element.attr("href");
                    if (href != null && !href.isEmpty()) {
                        try {
                            writeLineToFile(urlsFile, resolveRelativeUrl(finalUrl, href));
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                    }
                });
                Element next = document.getElementsByClass("mw-nextlink").first();
                if (next == null) {
                    break;
                }
                String nextUrl = resolveRelativeUrl(url, next.attr("href"));
                if (nextUrl.equals(url)) {
                    break;
                }
                url = nextUrl;
            } catch (Exception e) {
                System.out.println("No revisions found for " + url);
                return;
            }
        }
    }

    private static String readStringFromURL(String requestURL) throws IOException {
        try (Scanner scanner = new Scanner(
                new URL(requestURL).openStream(), StandardCharsets.UTF_8
        )) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private static void writeLineToFile(File file, String line) {
        System.out.println(line);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.append(line);
            writer.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to write to file");
        }
    }

    private static String resolveRelativeUrl(String base, String url) throws MalformedURLException {
        return new URL(new URL(base), url).toString();
    }
}
