package dev.digitaldragon.util;

import lombok.Getter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The EnvConfig class provides functionality to read environment configurations from the .env file in
 * the directory the jar is located in.
 */
public class EnvConfig {
    private static final Map<String, String> configs = new ConcurrentHashMap<>();
    @Getter
    private Runnable runnable;

    public static void updateConfigs() {
        for (String s : readFileLines(".env")) {
            if (s.startsWith("#") || s.equalsIgnoreCase("") || s.equalsIgnoreCase("\n")) {
                continue;
            }
            String[] sarray = s.split("=");
            if (sarray.length == 1)
                continue;
            StringBuilder after = new StringBuilder();
            configs.put(sarray[0], sarray[1]);
        }
    }

    public static Map<String, String> getConfigs(){
        updateConfigs();
        return configs;
    }

    private static String[] readFileLines(String file) {
        List<String> str = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                str.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str.toArray(new String[0]);
    }

    public static int getInt(String key) {
        return Integer.parseInt(getConfigs().get(key));
    }

    public void ready(Runnable callback) {
        this.runnable = callback;
    }
}