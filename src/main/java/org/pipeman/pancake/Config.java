package org.pipeman.pancake;

import org.simpleyaml.configuration.file.YamlFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    private static final YamlFile yamlFile;

    static {
        yamlFile = new YamlFile("config.yml");

        yamlFile.options().copyDefaults(true);

        yamlFile.createSection("webserver");
        yamlFile.addDefault("webserver.port", 8080);
        yamlFile.addDefault("webserver.host", "0.0.0.0");

        yamlFile.createSection("minecraft-server");
        yamlFile.addDefault("minecraft-server.directory", "mc-server");
        yamlFile.addDefault("minecraft-server.start-command", "java -jar paper-1.21.1-120.jar nogui");

        yamlFile.addDefault("minecraft-server.console-history.max-lines", 5);

        try {
            yamlFile.createOrLoadWithComments(); // Loads the entire file
            // If your file has comments inside you have to load it with yamlFile.loadWithComments()
        } catch (final Exception e) {
            LOGGER.error("Failed to parse config", e);
        }

                try {
            yamlFile.save();
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public static int serverPort() {
        return yamlFile.getInt("webserver.port");
    }

    public static String serverHost() {
        return yamlFile.getString("webserver.host");
    }

    public static String mcServerDirectory() {
        return yamlFile.getString("minecraft-server.directory");
    }

    public static String mcServerStartCommand() {
        return yamlFile.getString("minecraft-server.start-command");
    }

    public static int mcServerConsoleHistoryMaxLines() {
        return yamlFile.getInt("minecraft-server.console-history.max-lines");
    }
}
