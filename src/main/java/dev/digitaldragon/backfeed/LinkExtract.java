package dev.digitaldragon.backfeed;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LinkExtract {
    public static List<String> extractLinksFromFile(File file) {
        List<String> links = new ArrayList<>();
        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            InputStream inputStream = new FileInputStream(file);
            XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(inputStream);

            boolean insideSiteInfo = false;
            boolean insideBase = false;
            boolean insideParsable = false;
            String baseText = null;
            String wikiHost = null;

            while (xmlStreamReader.hasNext()) {
                int event = xmlStreamReader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String elementName = xmlStreamReader.getLocalName();
                        if ("siteinfo".equals(elementName)) {
                            insideSiteInfo = true;
                        } else if ("base".equals(elementName) && insideSiteInfo) {
                            insideBase = true;
                        } else if ("text".equals(elementName) || "summary".equals(elementName)) {
                            insideParsable = true;
                        }
                    break;

                    case XMLStreamConstants.CHARACTERS:
                        if (insideBase) {
                            baseText = xmlStreamReader.getText();
                            wikiHost = getHostFromUrl(baseText);
                            System.out.println(baseText);
                            System.out.println(wikiHost);
                        }
                        if (insideParsable) {
                            links.addAll(checkAndGetLinks(xmlStreamReader.getText(), wikiHost));
                        }
                    break;

                    case XMLStreamConstants.END_ELEMENT:
                        elementName = xmlStreamReader.getLocalName();
                        if ("siteinfo".equals(elementName)) {
                            insideSiteInfo = false;
                        } else if ("text".equals(elementName) || "summary".equals(elementName)) {
                            insideParsable = false;
                        } if ("base".equals(elementName)) {
                            insideBase = false;
                        }
                    break;
                }
            }

            xmlStreamReader.close();
            inputStream.close();
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
        }


        return links;

    }

    private static List<String> checkAndGetLinks(String body, String wikiHost) {
        List<String> links = new ArrayList<>();
        if (body.contains("://")) {
            links.addAll(getLinksFromBody(body, wikiHost));
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


        //remove all text before the first "http" instance
        if (string.contains("http")) {
            string = string.substring(string.indexOf("http"));
        }

        return string;
    }
}
