package dev.digitaldragon.parser;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CommandLineParser {
    //write a class to parse command line arguments
    //this class should be able to parse the following:
    // -switch (booleans) eg "--ignore" sets ignore to true
    // -switch value (integers) eg "--retry 5" sets retry to 5
    // -switch value (doubles) eg "--delay 5.5" sets delay to 5.5
    // -switch value (urls) eg "--url https://example.com" sets url to https://example.com - it should use the java URL class to verify these are real urls
    // -switch value (strings) eg "--explain this is an explanation" sets explain to "this is an explanation"

    private Map<String, Object> options;
    private Map<String, String> optionTypes;

    public CommandLineParser() {
        options = new HashMap<>();
        optionTypes = new HashMap<>();
    }

    public CommandLineParser addBooleanOption(String key) {
        optionTypes.put(key, "boolean");
        return this;
    }

    public CommandLineParser addIntOption(String key) {
        optionTypes.put(key, "int");
        return this;
    }

    public CommandLineParser addDoubleOption(String key) {
        optionTypes.put(key, "double");
        return this;
    }

    public CommandLineParser addUrlOption(String key) {
        optionTypes.put(key, "url");
        return this;
    }

    public CommandLineParser addStringOption(String key) {
        optionTypes.put(key, "string");
        return this;
    }

    public void parse(String[] args) {
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                String key = arg.substring(2);
                if (i + 1 < args.length) {
                    String nextArg = args[i + 1];
                    if (!nextArg.startsWith("--")) {
                        i++; // Move to the next argument as it's a value for the current switch
                        parseValue(key, nextArg);
                    } else {
                        // Treat it as a boolean switch with no value
                        parseBooleanSwitch(key);
                    }
                } else {
                    // Treat it as a boolean switch with no value
                    parseBooleanSwitch(key);
                }
            }
            i++;
        }
    }

    private void parseValue(String key, String value) {
        String type = optionTypes.get(key);
        if (type == null) {
            System.err.println("Unknown switch: " + key);
            return;
        }

        switch (type) {
            case "boolean" -> parseBooleanSwitch(key);
            case "int" -> {
                try {
                    options.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid integer value for " + key + ": " + value);
                }
            }
            case "double" -> {
                try {
                    options.put(key, Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid double value for " + key + ": " + value);
                }
            }
            case "url" -> {
                if (isValidURL(value)) {
                    options.put(key, value);
                } else {
                    System.err.println("Invalid URL: " + value);
                }
            }
            case "string" -> options.put(key, value);
            default -> System.err.println("Unknown switch: " + key);
        }
    }

    private void parseBooleanSwitch(String key) {
        options.put(key, true);
    }

    private boolean isValidURL(String url) {
        try {
            new URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Object getOption(String key) {
        return options.get(key);
    }
}
