package dev.digitaldragon.backfeed;

import dev.digitaldragon.jobs.Job;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LinkExtract {
    public static Set<String> extractLinksFromFile(InputStream inputStream) {
        List<String> links = new ArrayList<>();
        try {
            //InputStream inputStream = new FileInputStream(file);
            //InputStream inputStream = InternetArchive.getUncompressedStream("https://archive.org/download/wiki-wiki.filezilla-project.org_wiki-20230825/wiki.filezilla-project.org_wiki-20230825-history.xml.zst");
            processXml(inputStream, links);

            inputStream.close();
        } catch (IOException | XMLStreamException e) {
            e.printStackTrace();
        }

        return new HashSet<>(links);
    }

    private static void processXml(InputStream stream, List<String> links)
            throws XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(stream);

        boolean insideSiteInfo = false;
        boolean insideBase = false;
        boolean insideParsable = false;
        String baseText = null;
        String wikiHost = null;

        while (reader.hasNext()) {
            int event = reader.next();

            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    boolean[] states = processStartElement(reader.getLocalName(), insideSiteInfo, insideBase);
                    insideSiteInfo = states[0];
                    insideBase = states[1];
                    insideParsable = states[2];
                    break;
                case XMLStreamConstants.CHARACTERS:
                    wikiHost = processCharacters(reader, wikiHost, links, insideBase, insideParsable);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    states = processEndElement(reader.getLocalName(), insideSiteInfo, insideParsable, insideBase);
                    insideSiteInfo = states[0];
                    insideParsable = states[1];
                    insideBase = states[2];
                    break;
            }
        }

        reader.close();
    }

    private static boolean[] processStartElement(String elementName, boolean insideSiteInfo, boolean insideBase) {
        boolean insideParsable = false;
        if ("siteinfo".equals(elementName)) {
            insideSiteInfo = true;
        } else if ("base".equals(elementName) && insideSiteInfo) {
            insideBase = true;
        } else if ("text".equals(elementName) || "summary".equals(elementName)) {
            insideParsable = true;
        }

        return new boolean[]{insideSiteInfo, insideBase, insideParsable};
    }

    private static String processCharacters(
            XMLStreamReader reader, String wikiHost, List<String> links, boolean insideBase, boolean insideParsable) {
        if (insideBase) {
            String baseText = reader.getText();
            wikiHost = getHostFromUrl(baseText);
            System.out.println(baseText);
            System.out.println(wikiHost);
        }
        if (insideParsable) {
            links.addAll(checkAndGetLinks(reader.getText(), wikiHost));
        }

        return wikiHost;
    }

    private static boolean[] processEndElement(String elementName, boolean insideSiteInfo, boolean insideParsable, boolean insideBase) {
        if ("siteinfo".equals(elementName)) {
            insideSiteInfo = false;
        } else if ("text".equals(elementName) || "summary".equals(elementName)) {
            insideParsable = false;
        } if ("base".equals(elementName)) {
            insideBase = false;
        }

        return new boolean[]{insideSiteInfo, insideParsable, insideBase};
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
                if (host != null && !host.equals(wikiHost) && cleaned.contains("http") && cleaned.contains("://"))
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
        //remove all text before "http" eg to clean (https://example.com/) into something the parser can handle
        if (string.contains("http")) {
            string = string.substring(string.indexOf("http"));
        }


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


        return string;
    }
}
