package org.pipeman.pancake;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkammerer.snowflakeid.SnowflakeIdGenerator;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.pipeman.pancake.rest.JobsApi;
import org.pipeman.pancake.rest.ServerApi;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Main {
    private static final SnowflakeIdGenerator ID_GENERATOR = SnowflakeIdGenerator.createDefault(0);

    public static void main(String[] args) {
//        MinecraftServer server = new MinecraftServer(Config.mcServerDirectory(), Config.mcServerStartCommand());
//        server.start();

        Javalin.create((c -> {
            c.showJavalinBanner = false;

            ObjectMapper mapper = new ObjectMapper().configure(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS.mappedFeature(), true);
            c.jsonMapper(new JavalinJackson(mapper, true));

            c.router.apiBuilder(() -> {
                path("api", () -> {
                    path("servers", () -> {
                        get(ServerApi::getServers);
                        path("{server-id}", () -> {
                            post("start", ServerApi::start);
                            post("stop", ServerApi::stop);
                            post("terminate", ServerApi::terminate);
                            post("command", ServerApi::sendCommand);

                            path("jobs", () -> {
                                get(JobsApi::getJobs);
                                path("{job-id}", () -> {
                                    delete(JobsApi::deleteJob);
                                    post(JobsApi::createJob);
                                    patch(JobsApi::patchJob);
                                });
                            });
                        });
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
}