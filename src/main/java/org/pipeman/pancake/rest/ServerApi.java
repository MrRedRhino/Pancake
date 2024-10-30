package org.pipeman.pancake.rest;

import io.javalin.http.Context;
import org.pipeman.pancake.MinecraftServer;
import org.pipeman.pancake.ServerManager;

import java.util.ArrayList;
import java.util.List;

public class ServerApi {
    public static void getServers(Context ctx) {
        List<Server> servers = new ArrayList<>();
        for (MinecraftServer server : ServerManager.getServers()) {
            servers.add(new Server(server.id(), "Server", server.state()));
        }
        ctx.json(servers);
    }

    public static void start(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();

        ServerManager.getServerById(serverId).start();
    }

    public static void stop(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();

        ServerManager.getServerById(serverId).stop();
    }

    public static void terminate(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();

        ServerManager.getServerById(serverId).terminate();
    }

    public static void sendCommand(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();
        SendCommandPayload payload = ctx.bodyAsClass(SendCommandPayload.class);

        ServerManager.getServerById(serverId).sendCommand(payload.command());
    }

    public static void getLogHistory(Context ctx) {
        long serverId = ctx.pathParamAsClass("server-id", Long.class).get();
        int startLine = ctx.queryParamAsClass("before-line", Integer.class).get();

        ServerManager.getServerById(serverId);
    }

    private record SendCommandPayload(String command) {
    }

    private record Server(long id, String name, MinecraftServer.State state) {
    }
}
