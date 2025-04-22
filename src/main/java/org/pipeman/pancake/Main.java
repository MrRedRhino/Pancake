package org.pipeman.pancake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkammerer.snowflakeid.SnowflakeIdGenerator;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.pipeman.pancake.addons.Platform;
import org.pipeman.pancake.addons.Platforms;
import org.pipeman.pancake.addons.VersionInfo;
import org.pipeman.pancake.loaders.Loader;
import org.pipeman.pancake.rest.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final SnowflakeIdGenerator ID_GENERATOR = SnowflakeIdGenerator.createDefault(0);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS.mappedFeature(), true);

    public static void main(String[] args) throws IOException {
        LOGGER.info("Starting Pancake {}", "v0.1");
        LOGGER.info("Setting up database");
        Database.init();

        Javalin.create((c -> {
            c.showJavalinBanner = false;
            c.useVirtualThreads = true;

            c.jsonMapper(new JavalinJackson(OBJECT_MAPPER, true));
            c.validation.register(Platforms.class, Platforms::fromString);
            c.validation.register(Loader.class, Loader::fromString);
            c.validation.register(Platform.ContentType.class, Platform.ContentType::fromString);
            c.validation.register(VersionInfo.class, VersionInfo::fromString);
            /*
            https://git.sakamoto.pl/domi/curseme/src/branch/meow/parsePack.sh
            https://github.com/SpigotMC/XenforoResourceManagerAPI/tree/master
            https://support.modrinth.com/en/articles/8802351-modrinth-modpack-format-mrpack
            */

            c.router.apiBuilder(() -> {
                path("api", () -> {
                    path("account", () -> {
                        get(UserApi::getAccount);
                        post("login", UserApi::login);
                        post("logout", UserApi::logout);
                    });

                    path("servers", () -> {
                        get(ServerApi::getServers);
                        post("import", ServerApi::importServer);

                        path("{server-id}", () -> {
                            post("start", ServerApi::start);
                            post("stop", ServerApi::stop);
                            post("terminate", ServerApi::terminate);
                            post("command", ServerApi::sendCommand);

                            path("jobs", () -> {
                                get(JobsApi::getJobs);
                                post(JobsApi::createJob);
                                path("{job-id}", () -> {
                                    delete(JobsApi::deleteJob);
                                    patch(JobsApi::patchJob);
                                });
                            });

                            path("logs", () -> {
                                get(LogsApi::listLogs);
                                get("{log-name}", LogsApi::getLogContent);
                            });

                            path("{type}", () -> {
                                get(FilesApi::listAddons);
                                post("install", FilesApi::installAddon);

                                path("{file}", () -> {
                                    post("enable", ctx -> FilesApi.setEnabled(ctx, true));
                                    post("disable", ctx -> FilesApi.setEnabled(ctx, false));
                                });

                                post("upload", FilesApi::uploadAddon);
                            });

                            path("files", () -> {
                                post("upload", FilesApi::upload);
                                delete(FilesApi::deleteFile);
                            });
                        });
                    });

                    path("loaders/{loader}", () -> {
                        get("game-versions", ServerApi::listSupportedGameVersionsByLoader);
                    });

                    path("mods", () -> {
                        get("search", FilesApi::searchMods);
                    });

                    path("files", () -> {
                        get("server-directories", FilesApi::listServerDirectories);
                    });

                    ws("websocket", wsConfig -> {
                        wsConfig.onConnect(WebSocketServer::onConnect);
                        wsConfig.onClose(WebSocketServer::onClose);
                        wsConfig.onMessage(WebSocketServer::onMessage);
                    });
                });
            });
        })).start(Config.serverHost(), Config.serverPort());
    }

    public static long generateNewId() {
        return ID_GENERATOR.next();
    }

    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }

    public static String serialize(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T deserialize(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}