package dev.digitaldragon.wikibot;

import com.beust.jcommander.ParameterException;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.mediawiki.WikiTeam3Args;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;

public class WikiTeam3ParseTest {

    @Test
    void testJunk() {
        Assertions.assertThrows(ParameterException.class, () -> {
            new WikiTeam3Args(new String[]{"asasd", "asdasd"});
        });
    }

    @Test
    void testUrl() throws ParseException {
        WikiTeam3Args args = new WikiTeam3Args(new String[]{"--url", "https://example.com"});
        assert args.getUrl().equals("https://example.com");
    }

    @Test
    void testInvalidUrl() {
        Assertions.assertThrows(ParameterException.class, () -> {
            new WikiTeam3Args(new String[]{"--url", "abcdefg"});
        });
    }

    @Test
    void testInvalidProtocol() {
        Assertions.assertThrows(ParameterException.class, () -> {
            new WikiTeam3Args(new String[]{"--url", "ftp://example.com"});
        });
    }

    @Test
    void testSilentMode() throws ParseException {
        WikiTeam3Args args = new WikiTeam3Args(new String[]{"--silent-mode", JobMeta.SilentMode.SILENT.toString()});
        assert args.getSilentMode().equals(JobMeta.SilentMode.SILENT.toString());
    }

    @Test
    void testInvalidSilentMode() {
        Assertions.assertThrows(ParameterException.class, () -> {
            new WikiTeam3Args(new String[]{"--silent-mode", "efefsavrgvrsvrvsrdzx"});
        });
    }

    @Test
    void testInOut() throws ParseException {
        WikiTeam3Args args = new WikiTeam3Args(new String[]{"--xml", "--images"});
        assert args.isXml();
        assert args.isImages();
    }

}
