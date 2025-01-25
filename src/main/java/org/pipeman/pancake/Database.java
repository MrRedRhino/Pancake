package org.pipeman.pancake;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.pipeman.pancake.addons.AddonManager;
import org.pipeman.pancake.rest.JobsApi;
import org.pipeman.pancake.rest.UserApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class Database {
    private static final Jdbi JDBI = Jdbi.create("jdbc:sqlite:pancake.db");
    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    static {
        JDBI.registerRowMapper(ConstructorMapper.factory(JobsApi.Job.class));
        JDBI.registerRowMapper(ConstructorMapper.factory(ServerManager.ServerData.class));
        JDBI.registerRowMapper(ConstructorMapper.factory(UserApi.User.class));
        JDBI.registerRowMapper(ConstructorMapper.factory(UserApi.UserIdHashSnapshot.class));
        JDBI.registerRowMapper(ConstructorMapper.factory(AddonManager.CachedAddonData.class));

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

    public static void init() {
    }
}
