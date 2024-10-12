package org.pipeman.pancake;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class MinecraftServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftServer.class);

    private final String path;
    private final String startCommand;
    private State state = State.STOPPED;
    private final Queue<String> terminalHistory = new LinkedList<>();
    private StringBuilder currentLine = new StringBuilder();
    private PtyProcess process;

    public MinecraftServer(String path, String startCommand) {
        this.path = path;
        this.startCommand = startCommand;
    }

    public void start() throws IOException {
        if (state != State.STOPPED) throw new IllegalArgumentException("Server not stopped");

        HashMap<String, String> env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm-256color");

        PtyProcess ptyProcess = new PtyProcessBuilder()
                .setEnvironment(env)
                .setDirectory(Config.mcServerDirectory())
                .setCommand(new String[]{"/bin/bash", "--login"})
//                .setCommand(Config.mcServerStartCommand().split(" "))
//                .setInitialColumns(120)
//                .setInitialRows(20)
//                .setConsole(false)
//                .setUseWinConPty(true)
                .start();

        ptyProcess.onExit().thenAccept(process -> {
            state = State.STOPPED;
            LOGGER.info("Server stopped");
        });
        new Thread(() -> handleConsoleInput(ptyProcess), "Console Reader").start();

        ptyProcess.outputWriter().write("/home/pipeman/.jdks/zulu21.32.17-ca-jre21.0.2-linux_x64/bin/java -jar paper-1.21.1-120.jar nogui\n");
        ptyProcess.outputWriter().flush();

//        ptyProcess.outputWriter().write("ls ");
//        ptyProcess.outputWriter().write(0x09);
//        ptyProcess.outputWriter().write(0x09);
//        ptyProcess.outputWriter().flush();

        state = State.STARTING;
        state = State.RUNNING;
        this.process = ptyProcess;
    }

    public void stop() {
        process.destroy();
        System.out.println("penis");
    }

    public void sendCommand(String command) {
        if (state != State.RUNNING) throw new IllegalArgumentException("Server not running");

        String trimmedCommand = command
                .trim()
                .replaceFirst("^/", "");

        try {
            process.outputWriter().write(command);
            process.outputWriter().flush();
        } catch (IOException e) {
            LOGGER.error("Failed to send command", e);
        }

        if (trimmedCommand.equalsIgnoreCase("stop")) {
            afterStopCommand();
        }
    }

    public Writer outputWriter() {
        return process.outputWriter();
    }

    private void afterStopCommand() {
        state = State.STOPPING;
        // TODO kill process after delay
    }

    private void handleConsoleInput(PtyProcess process) {
        try {
//            while (process.isAlive()) {
            while (true) {
                char read = (char) process.inputReader().read();
                System.out.print(read);
                if (read == '\n') {
                    terminalHistory.add(currentLine.toString());
                    currentLine = new StringBuilder();
                    while (terminalHistory.size() > Config.mcServerConsoleHistoryMaxLines()) {
                        terminalHistory.poll();
                    }
                } else {
                    currentLine.append(read);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public enum State {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING
    }
}
