package dev.digitaldragon.jobs;

import dev.digitaldragon.interfaces.UserErrorException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Optional;

public abstract class Wiki {

    public static Wiki detectWiki(String url) throws UserErrorException {
        try {
            Document document = Jsoup.connect(url).get();
            // check for mediawiki generator tag in head
            if (!document.head().select("meta[name=generator][content^=MediaWiki]").isEmpty()) {
                return new MediaWiki(url);
            }

            //check for DokuWiki= cookie
            if (document.connection().response().cookie("DokuWiki") != null) {
                return new DokuWiki();
            }

            throw new UserErrorException("Couldn't detect type for the wiki at " + url + ". Supported wiki software: MediaWiki, DokuWiki.");
        } catch (HttpStatusException e) {
            throw new UserErrorException("Could not connect to the wiki at " + url + ". Bad response status code: " + e.getStatusCode());
        } catch (IOException e) {
            throw new UserErrorException("Could not connect to the wiki at " + url + ". Error: " + e.getMessage());
        }
    }

    /**
     * @return The Job started for this wiki, if one exists.
     */
    public abstract Optional<Job> getJob();

    /**
     * @return If the wiki is safe to run a job on automatically.
     */
    public abstract boolean isSafe();

    /**
     *
     * @param url the URL of the wiki to run the job on.
     * @param username the user who is running the job.
     * @param explain the user's explanation for running the job.
     * @throws UserErrorException if the wiki is not safe to run a job on.
     * @return The Job started for this wiki, if one is started.
     */
    public abstract Optional<Job> run(String url, String username, String explain) throws UserErrorException;

    /**
     * Prevents needing to make multiple requests to the same wiki by reusing the Document object from a previous request.
     * @param document document of a page on the wiki to run the job on.
     * @param username the user who is running the job.
     * @param explain the user's explanation for running the job.
     * @throws UserErrorException if the wiki is not safe to run a job on.
     * @return The Job started for this wiki, if one is started.
     */
    public abstract Optional<Job> run(Document document, String username, String explain) throws UserErrorException;

}
