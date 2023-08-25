package dev.digitaldragon.backfeed;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LinkExtract {
    public static List<String> extractLinksFromFile(File file) {
        List<String> links = new ArrayList<>();
        try {
            InputStream inputStream = new FileInputStream(file);
            Document document = Jsoup.parse(inputStream, "UTF-8", "");

            //extract elements
            Elements text = document.select("text");
            Elements summaries = document.select("summary");

            String wikiHost = getWikiHost(document);
            links.addAll(getLinks(text, wikiHost));
            links.addAll(getLinks(summaries, wikiHost));
        } catch (IOException exception) {
            System.out.println("Error parsing file " + file.getName());
            exception.printStackTrace();
        }
        return links;
    }

    private static String getWikiHost(Document document) {
        Element element = document.selectFirst("siteinfo > base");
        if (element == null)
            return null;
        String url = element.text();
        return getHostFromUrl(url);
    }

    private static List<String> getLinks(Elements elements, String wikiHost) {
        List<String> links = new ArrayList<>();
        for (Element element : elements) {
            String text = element.text();
            // generic text parsing for urls
            if (text.contains("://")) {
                links.addAll(getLinksFromBody(text, wikiHost));
            }
        }
        return links;
    }

    private static List<String> getLinksFromBody(String body, String wikiHost) {
        List<String> links = new ArrayList<>();
        String[] split = body.split(" ");
        for (String s : split) {
            if (s.contains("://")) {
                String cleaned = cleanUrl(s);
                String host = getHostFromUrl(cleaned);
                if (host != null && !host.equals(wikiHost))
                    links.add(cleaned);
            }
        }
        return links;
    }

    private static String getHostFromUrl(String string) {
        try {
            URL url = new URL(string);
            return url.getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private static String cleanUrl(String string) {
        // remove garbage trailing characters eg to fix "(available at https://example.com/)":

        if (string.contains("]")) {
            string = string.substring(0, string.indexOf("]"));
        }

        //if (string.matches("</?[^<>]*>")) {
        //    string = string.split("</?[^<>]*>")[0];
        //}

        if (string.contains("<")) {
            string = string.substring(0, string.indexOf("<"));
        }

        if (string.endsWith(").") || string.endsWith("),") || string.endsWith(".)")) { //yeah, this is manual, but honesly the best route to take lol
            string = string.substring(0, string.length() - 2);
        }

        if (string.endsWith(")") && !string.contains("(")) { //This seems like a good compromise between removing all trailing parentheses and not removing any parentheses
            string = string.substring(0, string.length() - 1);
        }

        if (string.endsWith(".")) {
            string = string.substring(0, string.length() - 1);
        }

        if (string.endsWith(",")) {
            string = string.substring(0, string.length() - 1);
        }

        if (string.endsWith("'''")) { //bold formatting in wikitext
            string = string.substring(0, string.length() - 3);
        }

        if (string.endsWith("}}")) { //template formatting in wikitext
            string = string.substring(0, string.length() - 3);
        }

        if (string.endsWith(";")) {
            string = string.substring(0, string.length() - 3);
        }


        return string;
    }
}
