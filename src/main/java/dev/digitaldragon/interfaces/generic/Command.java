package dev.digitaldragon.interfaces.generic;

import java.util.ArrayList;
import java.util.List;

public class Command {
    public static List<String> shellSplit(String command) {
        List<String> args = new ArrayList<>();
        StringBuilder argument = new StringBuilder();
        String endQuote = null;
        boolean escape = false;
        for (String character : command.split("")) {
            boolean consumed = false;
            if (escape) {
                argument.append(character);
                escape = false;
                continue;
            }

            if (character.equals("\\")) {
                escape = true;
                consumed = true;
            }

            if (endQuote != null && endQuote.equals(character)) {
                endQuote = null;
                consumed = true;
            } else if (endQuote == null && List.of("\"", "'").contains(character)) {
                endQuote = character;
                consumed = true;
            }

            if (character.isBlank() && endQuote == null) {
                if (!argument.isEmpty()) args.add(argument.toString());
                argument = new StringBuilder();
            } else {
                if (!consumed) argument.append(character);
            }
        }
        if (!argument.isEmpty()) args.add(argument.toString());
        return args;
    }
}
