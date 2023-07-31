package dev.digitaldragon.parser;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public void parse(String[] args) throws IllegalArgumentException {
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                String key = arg.substring(2);
                i++; // Move to the next argument for the potential value
                StringBuilder valueBuilder = new StringBuilder();
                while (i < args.length && !args[i].startsWith("--")) {
                    valueBuilder.append(args[i]).append(" ");
                    i++;
                }
                String value = valueBuilder.toString().trim();
                if (value.isEmpty()) {
                    // Treat it as a boolean switch with no value
                    parseBooleanSwitch(key);
                } else {
                    parseValue(key, value);
                }
            } else {
                i++; // Move to the next argument as it's not a switch
            }
        }
    }

    private void parseValue(String key, String value) throws IllegalArgumentException {
        String type = optionTypes.get(key);
        if (type == null) {
            throw new IllegalArgumentException("Unknown option: " + key);
        }

        switch (type) {
            case "boolean" -> parseBooleanSwitch(key);
            case "int" -> {
                try {
                    options.put(key, Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid integer value for " + key + ": " + value);
                }
            }
            case "double" -> {
                try {
                    options.put(key, Double.parseDouble(value));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid double value for " + key + ": " + value);
                }
            }
            case "url" -> {
                if (isValidURL(value)) {
                    options.put(key, value);
                } else {
                    throw new IllegalArgumentException("Invalid URL value for " + key + ": " + value + ". Remember: The URL parser expects a protocol (eg https://), domains don't count!");
                }
            }
            case "string" -> options.put(key, value);
            default -> throw new IllegalArgumentException("Unknown option: " + key);
        }
    }

    private void parseBooleanSwitch(String key) {
        String type = optionTypes.get(key);
        if (type == null) {
            throw new IllegalArgumentException("Unknown switch: " + key);
        }
        if (!Objects.equals(type, "boolean")) {
            throw new IllegalArgumentException("--" + key + " requires a value! (" + type + ")");
        }

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
