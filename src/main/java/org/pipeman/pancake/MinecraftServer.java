package org.pipeman.pancake;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import org.pipeman.pancake.WebSocketServer.ServerLaunchedEventData;
import org.pipeman.pancake.WebSocketServer.ServerStateChangedEventData;
import org.pipeman.pancake.ping.ServerPing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MinecraftServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftServer.class);

    private final long id;
    private final String path;
    private final String startCommand;
    private final ScheduledExecutorService executorService;
    private State state = State.STOPPED;
    private long startedAt;

    private final Queue<ConsoleLine> terminalHistory = new LinkedList<>();
    private final AtomicInteger lineNumber = new AtomicInteger();
    private StringBuilder currentLine = new StringBuilder();

    private PtyProcess process;
    private ScheduledFuture<?> pingTask;
    private CompletableFuture<Void> stopFuture;

    public MinecraftServer(long id, String path, String startCommand, ScheduledExecutorService executorService) {
        this.id = id;
        this.path = path;
        this.startCommand = startCommand;
        this.executorService = executorService;
    }

    public void start() {
        if (state != State.STOPPED) throw new IllegalArgumentException("Server not stopped");

        HashMap<String, String> env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm-256color");

        try {
            PtyProcess ptyProcess = new PtyProcessBuilder()
                    .setEnvironment(env)
                    .setDirectory(path)
                    .setCommand(startCommand.split(" "))
//                    .setCommand(new String[]{"/bin/bash", "--login"})
                    .start();

            ptyProcess.onExit().thenAccept(process -> {
                int exitValue = process.exitValue();
                setState(State.STOPPED);
                stopFuture.complete(null);
                LOGGER.info("Server stopped. Exit code: {}", exitValue);
            });
            WebSocketServer.broadcast(new ServerLaunchedEventData(id));
            new Thread(() -> handleConsoleInput(ptyProcess), "Console Reader").start();

//            ptyProcess.outputWriter().write(startCommand + "\n");
//            ptyProcess.outputWriter().flush();

            setState(State.STARTING);
            startedAt = System.currentTimeMillis();
            this.process = ptyProcess;

            currentLine = new StringBuilder();
            terminalHistory.clear();
            lineNumber.set(0);

            pingTask = executorService.scheduleWithFixedDelay(this::pingServer, 3, 3, TimeUnit.SECONDS);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start server", e);
        }
    }

    private void pingServer() {
        try {
            ServerPing.pingServer("localhost", 25565);// TODO dynamically
            setState(State.RUNNING);
        } catch (Exception ignored) {
        }
    }

    public CompletableFuture<Void> stop() {
        if (state != State.RUNNING) throw new IllegalStateException("Server not running");

        stopFuture = new CompletableFuture<>();
        setState(State.STOPPING);
        process.destroy();

        return stopFuture;
    }

    public void terminate() {
        process.destroyForcibly();
        state = State.STOPPED;
    }

    private void setState(State state) {
        if (state == State.STOPPED || state == State.STOPPING) {
            pingTask.cancel(true);
        }
        if (state != this.state) {
            WebSocketServer.broadcast(new ServerStateChangedEventData(id, state));
            this.state = state;
        }
    }

    public void sendCommand(String command) {
        if (state != State.RUNNING) throw new IllegalArgumentException("Server not running");

        String trimmedCommand = command
                .trim()
                .replaceFirst("^/", "")
                .replace("\n", "\\n");

        try {
            process.outputWriter().write(command + "\n");
            process.outputWriter().flush();

            if (trimmedCommand.equalsIgnoreCase("stop")) {
                afterStopCommand();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to send command", e);
        }
    }

    public List<ConsoleLine> getConsoleHistory(int lineNumber, int count) {
        List<ConsoleLine> history = List.copyOf(terminalHistory);
        int start = history.size() - (history.getLast().lineNumber() - lineNumber);
        int end = Math.min(start + count, terminalHistory.size());
        return history.subList(start, end);
    }

    public List<ConsoleLine> getConsoleLastNLines(int n) {
        List<ConsoleLine> history = List.copyOf(terminalHistory);
        int start = Math.max(0, history.size() - n);
        return history.subList(start, history.size());
    }

    public Writer outputWriter() {
        return process.outputWriter();
    }

    private void afterStopCommand() {
        setState(State.STOPPING);
        // TODO kill process after delay
    }

    public ScheduledExecutorService executorService() {
        return executorService;
    }

    private void handleConsoleInput(PtyProcess process) {
        try {
            while (process.isAlive()) {
                int read = process.inputReader().read();
                if (read == '\n') {
                    int thisLineNumber = lineNumber.getAndIncrement();
                    terminalHistory.add(new ConsoleLine(currentLine.toString(), thisLineNumber));
                    WebSocketServer.broadcastToSubscribers(new WebSocketServer.UpsertConsoleLineEventData(id, currentLine.toString(), thisLineNumber));

                    currentLine = new StringBuilder();
                    while (terminalHistory.size() > Config.mcServerConsoleHistoryMaxLines()) {
                        terminalHistory.poll();
                    }
                } else if (read != -1) {
                    currentLine.append((char) read);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public long startedAt() {
        return startedAt;
    }

    public record ConsoleLine(String content, int lineNumber) {
    }

    public State state() {
        return state;
    }

    public long id() {
        return id;
    }

    public enum State {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING
    }
}
