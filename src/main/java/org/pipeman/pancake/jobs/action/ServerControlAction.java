package org.pipeman.pancake.jobs.action;

import org.json.JSONObject;
import org.pipeman.pancake.MinecraftServer;

import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;

public class ServerControlAction implements Action {
    @Override
    public CompletableFuture<Boolean> run(JSONObject config, MinecraftServer server, PrintWriter logPrintWriter) {
        if (config.getString("action").equals("start")) {
            server.start();
            return CompletableFuture.completedFuture(true);
        } else if (config.getString("action").equals("stop")) {
            return server.stop().thenApply(v -> true);
        }
        return CompletableFuture.completedFuture(false);
    }
}
