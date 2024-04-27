package dev.digitaldragon.jobs;

import dev.digitaldragon.interfaces.UserErrorException;
import org.jsoup.nodes.Document;

import java.util.Optional;

public class DokuWiki extends Wiki {
    @Override
    public Optional<Job> getJob() {
        return Optional.empty();
    }

    @Override
    public String getUnsafeReason() {
        return null;
    }

    @Override
    public Optional<Job> run(String url, String username, String explain) throws UserErrorException {
        return Optional.empty();
    }

    @Override
    public Optional<Job> run(Document document, String username, String explain) throws UserErrorException {
        return Optional.empty();
    }
}
