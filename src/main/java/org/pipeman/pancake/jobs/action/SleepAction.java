package org.pipeman.pancake.jobs.action;

import org.json.JSONObject;
import org.pipeman.pancake.MinecraftServer;

import java.io.PrintWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SleepAction implements Action {
    @Override
    public CompletableFuture<Boolean> run(JSONObject config, MinecraftServer server, PrintWriter logPrintWriter) {
        int delay = config.getInt("delay");

        return new CompletableFuture<Boolean>().completeOnTimeout(true, delay, TimeUnit.SECONDS);
    }
}
