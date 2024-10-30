package org.pipeman.pancake;

import io.javalin.websocket.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebSocketServer {
    private static final Map<WsContext, Long> connections = new HashMap<>();
    private static final Map<WsContext, List<Long>> logSubscriptions = new HashMap<>();

    public static void onConnect(WsConnectContext ctx) {
        connections.put(ctx, 0L);
        ctx.enableAutomaticPings();
    }

    public static void onClose(WsCloseContext ctx) {
        connections.remove(ctx);
    }

    public static void broadcast(EventData message) {
        WsEvent event = new WsEvent(message.type(), message);

        for (WsContext ctx : connections.keySet()) {
            if (ctx.session.isOpen()) {
                ctx.send(event);
            }
        }
    }

    public static void onMessage(WsMessageContext ctx) {

    }

    private record WsEvent(String type, EventData data) {
    }

    public record AppendLogEventData(long serverId, String line, int lineNumber) implements EventData {
        @Override
        public String type() {
            return "APPEND_LOG";
        }
    }

    public record ServerStateChangedEventData(long serverId, MinecraftServer.State state) implements EventData {
        @Override
        public String type() {
            return "SERVER_STATE_CHANGED";
        }
    }

    public interface EventData {
        String type();
    }
}
