package dev.digitaldragon.backfeed;

import com.google.common.net.InternetDomainName;

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
    /**
     * Extracts links from an input XML file and returns them as a set of strings.
     *
     * @param inputStream the input stream of the file to extract links from
     * @return a set of strings containing the extracted links
     */
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

    /**
     * Processes an XML file and extracts links from it.
     *
     * @param stream the input stream of the XML file
     * @param links a list to store the extracted links
     * @throws XMLStreamException if there is an error processing the XML
     */
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

    /**
     * Processes the start element of an XML document.
     *
     * @param elementName the name of the start element
     * @param insideSiteInfo the current state of whether we are inside the "siteinfo" element
     * @param insideBase the current state of whether we are inside the "base" element
     * @return an array representing the updated states of "insideSiteInfo", "insideBase", and "insideParsable"
     */
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

    /**
     * Processes the characters of an XML document.
     *
     * @param reader the XMLStreamReader object to read the XML document
     * @param wikiHost the current host of the wiki
     * @param links the list of links found in the XML document
     * @param insideBase the current state of whether we are inside the "base" element
     * @param insideParsable the current state of whether we are inside a parsable element
     * @return the updated "wikiHost"
     */
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

    /**
     * Processes the end element of an XML document.
     *
     * @param elementName the name of the end element
     * @param insideSiteInfo the current state of whether we are inside the "siteinfo" element
     * @param insideParsable the current state of whether we are inside a parsable element
     * @param insideBase the current state of whether we are inside the "base" element
     * @return an array of booleans representing the updated states of "insideSiteInfo", "insideParsable", and "insideBase"
     */
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

    /**
     * Checks the body of a document for links and returns a list of those links.
     *
     * @param body the body of the document to check
     * @param wikiHost the host of the wiki
     * @return a list of links found in the body
     */
    private static List<String> checkAndGetLinks(String body, String wikiHost) {
        List<String> links = new ArrayList<>();
        if (body.contains("://")) {
            links.addAll(getLinksFromBody(body, wikiHost));
        }
        return links;
    }

    /**
     * Retrieves the links from the body of a document.
     *
     * @param body the body of the document to retrieve links from
     * @param wikiHost the host of the wiki
     * @return a list of links found in the body
     */
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

    /**
     * Retrieves the host from a given URL.
     *
     * @param string the URL to retrieve the host from
     * @return the host extracted from the URL, or null if the URL is malformed
     */
    private static String getHostFromUrl(String string) {
        try {
            URL url = new URL(string);
            String host = url.getHost();
            return InternetDomainName.from(host).topPrivateDomain().toString();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Cleans a given URL string by removing characters added to it that are not intended to be in the URL.
     *
     * @param string the URL string to be cleaned
     * @return the cleaned URL string
     */
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
