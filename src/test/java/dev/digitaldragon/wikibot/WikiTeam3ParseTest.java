package dev.digitaldragon.wikibot;

import com.beust.jcommander.ParameterException;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Args;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Job;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.UUID;

public class WikiTeam3ParseTest {

    @Test
    void testJunk() {
        Assertions.assertThrows(ParameterException.class, () -> {
            new WikiTeam3Args(new String[]{"asasd", "asdasd"}, new JobMeta("Test"));
        });
    }

    @Test
    void testUrl() throws ParseException {
        WikiTeam3Args args = new WikiTeam3Args(new String[]{"--url", "https://example.com"}, new JobMeta("Test"));
        assert args.getUrl().equals("https://example.com");
    }

    @Test
    void testInvalidUrl() {
        Assertions.assertThrows(ParameterException.class, () -> {
            new WikiTeam3Args(new String[]{"--url", "abcdefg"}, new JobMeta("Test"));
        });
    }

    @Test
    void testInvalidProtocol() {
        Assertions.assertThrows(ParameterException.class, () -> {
            new WikiTeam3Args(new String[]{"--url", "ftp://example.com"}, new JobMeta("Test"));
        });
    }

    @Test
    void testSilentMode() throws ParseException {
        JobMeta meta = new JobMeta("Test");
        Job job = new WikiTeam3Job(
                new WikiTeam3Args(new String[]{"--silent-mode", JobMeta.SilentMode.SILENT.toString(), "--url", "https://invalid.localhost"}, meta),
                meta,
                UUID.randomUUID().toString()
        );
        assert job.getMeta().getSilentMode().equals(JobMeta.SilentMode.SILENT);
    }

    @Test
    void testInvalidSilentMode() {
        Assertions.assertThrows(ParameterException.class, () -> {
            new WikiTeam3Args(new String[]{"--silent-mode", "efefsavrgvrsvrvsrdzx"}, new JobMeta("Test"));
        });
    }

    @Test
    void testInOut() throws ParseException {
        WikiTeam3Args args = new WikiTeam3Args(new String[]{"--xml", "--images"}, new JobMeta("Test"));
        assert args.isXml();
        assert args.isImages();
    }

}
