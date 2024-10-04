package dev.digitaldragon.wikibot;

import dev.digitaldragon.interfaces.generic.Command;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ShellSplitTest {

    @Test
    void testIndividualItems() {
        // Test individual items
        assert Command.shellSplit("test").equals(List.of("test"));
        assert Command.shellSplit("test test").equals(List.of("test", "test"));
        assert Command.shellSplit("test test test").equals(List.of("test", "test", "test"));
        assert Command.shellSplit("test  test test").equals(List.of("test", "test", "test"));
    }

    @Test
    void testQuotes() {
        // Test quotes
        assert Command.shellSplit("\"test\"").equals(List.of("test"));
        assert Command.shellSplit("\"test test\"").equals(List.of("test test"));
        assert Command.shellSplit("\"test test test\"").equals(List.of("test test test"));
        assert Command.shellSplit("\"test test test\" test").equals(List.of("test test test", "test"));
        assert Command.shellSplit("'test test test' test").equals(List.of("test test test", "test"));
    }

    @Test
    void testMixedQuotes() {
        // Test mixed quotes
        assert Command.shellSplit("\"one two\" 'three four'").equals(List.of("one two", "three four"));
        assert Command.shellSplit("'one two' \"three four\"").equals(List.of("one two", "three four"));
        assert Command.shellSplit("\"one 'two three' four\" five").equals(List.of("one 'two three' four", "five"));
        assert Command.shellSplit("\"one 'two three' four\" five 'six \"seven eight\" nine'").equals(List.of("one 'two three' four", "five", "six \"seven eight\" nine"));
    }

    @Test
    void testEscapes() {
        assert Command.shellSplit("one \\\"two \\\"three").equals(List.of("one", "\"two", "\"three"));
        assert Command.shellSplit("one \\\"two \"three four\" five").equals(List.of("one", "\"two", "three four", "five"));
        assert Command.shellSplit("one \"two \\\"three four\\\" five\" six").equals(List.of("one", "two \"three four\" five", "six"));
    }

    @Test
    void testHistoricallyProblematicCommands() {
        assert Command.shellSplit("!mw --url https://vsrecommendedgames.miraheze.org/wiki/Main_Page --xml --xmlrevisions --images --delay 10 -e \"dumps from 2022 and 2023 exist, update for 2024\"  --resume f9382988-d45d-43c0-a08f-859b6c56e2e4 --bypass-cdn-image-compression")
                .equals(List.of("!mw", "--url", "https://vsrecommendedgames.miraheze.org/wiki/Main_Page", "--xml", "--xmlrevisions", "--images", "--delay", "10", "-e", "dumps from 2022 and 2023 exist, update for 2024", "--resume",  "f9382988-d45d-43c0-a08f-859b6c56e2e4", "--bypass-cdn-image-compression"));

        assert Command.shellSplit("!mw --url https://www.avid.wiki/Main_Page --xml --xmlrevisions --images --force --explain \"migration announced \\\"over the next few days\\\" on 2024-09-05: https://www.avid.wiki/Forum:IMPORTANT:_AVID_will_be_migrating_to_MyWikis\"")
                .equals(List.of("!mw", "--url", "https://www.avid.wiki/Main_Page", "--xml", "--xmlrevisions", "--images", "--force", "--explain", "migration announced \"over the next few days\" on 2024-09-05: https://www.avid.wiki/Forum:IMPORTANT:_AVID_will_be_migrating_to_MyWikis"));
    }
}
