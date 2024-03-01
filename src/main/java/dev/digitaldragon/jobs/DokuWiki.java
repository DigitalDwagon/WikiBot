package dev.digitaldragon.jobs;

import dev.digitaldragon.interfaces.UserErrorException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Optional;

public class DokuWiki extends Wiki {
    @Override
    public Optional<Job> getJob() {
        return Optional.empty();
    }

    @Override
    public boolean isSafe() {
        return true; //TODO: Implement dokuwiki safety check
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
        throw new UserErrorException("DokuWiki!");
    }
}
