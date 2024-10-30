package org.pipeman.pancake;

import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class Database {
    private static final Jdbi JDBI = Jdbi.create("jdbc:h2:./pancake");
    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    static {
        try (InputStream stream = Main.class.getResourceAsStream("/setup.sql")) {
            String setupScript = new String(Objects.requireNonNull(stream).readAllBytes());
            JDBI.useHandle(h -> h.createScript(setupScript).execute());
        } catch (IOException e) {
            LOGGER.error("Failed to load setup script", e);
            throw new RuntimeException(e);
        }
    }

    public static Jdbi getJdbi() {
        return JDBI;
    }
}
