package org.pipeman.pancake.jobs;

import org.json.JSONArray;
import org.json.JSONObject;
import org.pipeman.pancake.MinecraftServer;
import org.pipeman.pancake.jobs.action.Action;
import org.pipeman.pancake.jobs.action.ChatMessageAction;
import org.pipeman.pancake.jobs.action.ServerControlAction;
import org.pipeman.pancake.jobs.action.SleepAction;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public class JobRuntime {
    private static final Map<String, Action> ACTION_TYPES = Map.of(
            "chat_message", new ChatMessageAction(),
            "server_control", new ServerControlAction(),
            "sleep", new SleepAction()
    );

    private final JSONArray actions;
    private final MinecraftServer server;
    private final Executor executor;
    private final BiConsumer<String, Boolean> onComplete;
    private final StringWriter logWriter = new StringWriter();
    private final PrintWriter logPrintWriter = new PrintWriter(logWriter);
    private int actionIndex = 0;

    public JobRuntime(JSONObject config, MinecraftServer server, Executor executor, BiConsumer<String, Boolean> onComplete) {
        this.actions = config.getJSONArray("tasks");
        this.server = server;
        this.executor = executor;
        this.onComplete = onComplete;
    }

    public void start() {
        scheduleNextTask();
    }

    private void scheduleNextTask() {
        int index = actionIndex++;
        if (index >= actions.length()) {
            logPrintWriter.flush();
            onComplete.accept(logWriter.toString(), true);
            return;
        }

        JSONObject actionConfig = actions.getJSONObject(index);
        Action action = ACTION_TYPES.get(actionConfig.getString("type"));

        executor.execute(() -> action.run(actionConfig.getJSONObject("config"), server, logPrintWriter)
                .thenAccept(success -> {
                    if (!success) {
                        logPrintWriter.flush();
                        onComplete.accept(logWriter.toString(), false);
                    } else {
                        scheduleNextTask();
                    }
                }));
    }
}
