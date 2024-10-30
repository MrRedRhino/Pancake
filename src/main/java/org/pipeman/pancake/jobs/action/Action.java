package org.pipeman.pancake.jobs.action;

import org.json.JSONObject;
import org.pipeman.pancake.MinecraftServer;

import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;

public interface Action {
    CompletableFuture<Boolean> run(JSONObject config, MinecraftServer server, PrintWriter logPrintWriter);
}
