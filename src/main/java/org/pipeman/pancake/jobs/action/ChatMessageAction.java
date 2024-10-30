package org.pipeman.pancake.jobs.action;

import org.json.JSONObject;
import org.pipeman.pancake.MinecraftServer;

import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;

public class ChatMessageAction implements Action {
    @Override
    public CompletableFuture<Boolean> run(JSONObject config, MinecraftServer server, PrintWriter logPrintWriter) {
        server.sendCommand("/say " + config.getString("message"));
        return CompletableFuture.completedFuture(true);
    }
}
