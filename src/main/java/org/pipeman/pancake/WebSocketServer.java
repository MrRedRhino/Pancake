package org.pipeman.pancake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

public class WebSocketServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketServer.class);
    private static final Map<WsContext, Long> connections = new HashMap<>();
    private static final Map<String, Set<Long>> logSubscriptions = new HashMap<>();

    public static void onConnect(WsConnectContext ctx) {
        connections.put(ctx, 0L);
        ctx.enableAutomaticPings();
    }

    public static void onClose(WsCloseContext ctx) {
        logSubscriptions.remove(ctx.sessionId());
        connections.remove(ctx);
    }

    public static void broadcast(EventData message) {
        broadcast(message, ctx -> true);
    }

    public static void broadcast(EventData message, Predicate<WsContext> filter) {
        WsEvent<?> event = new WsEvent<>(message.type(), message);

        for (WsContext ctx : connections.keySet()) {
            if (ctx.session.isOpen() && filter.test(ctx)) {
                ctx.send(event);
            }
        }
    }

    public static void broadcastToSubscribers(UpsertConsoleLineEventData message) {
        broadcast(message, ctx -> isSubscribed(ctx, message.serverId()));
    }

    private static boolean isSubscribed(WsContext ctx, long serverId) {
        Set<Long> subscriptions = logSubscriptions.get(ctx.sessionId());
        return subscriptions != null && subscriptions.contains(serverId);
    }

    public static void onMessage(WsMessageContext ctx) {
        try {
            String messageType = new JSONObject(ctx.message()).optString("type", "");
            switch (messageType) {
                case "SUBSCRIBE" -> SubscribeLogMessageData.handle(ctx);
                case "UNSUBSCRIBE" -> UnsubscribeLogMessageData.handle(ctx);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to handle message", e);
        }
    }

    private record WsEvent<D extends EventData>(String type, D data) {
    }

    public record SubscribeLogMessageData(long serverId) implements EventData {
        @Override
        public String type() {
            return "SUBSCRIBE";
        }

        public static void handle(WsMessageContext ctx) throws JsonProcessingException {
            WsEvent<SubscribeLogMessageData> message = Main.objectMapper().readValue(ctx.message(), new TypeReference<>() {
            });
            long serverId = message.data().serverId;

            Optional<ServerManager.ServerData> serverData = ServerManager.getServerData(serverId);
            if (serverData.isEmpty()) return;

            boolean added = logSubscriptions.computeIfAbsent(ctx.sessionId(), k -> new HashSet<>()).add(serverId);
            Optional<MinecraftServer> server = ServerManager.optServerById(serverId);
            if (added && server.isPresent()) {
                for (MinecraftServer.ConsoleLine line : server.get().getConsoleLastNLines(100)) {
                    UpsertConsoleLineEventData eventData = new UpsertConsoleLineEventData(serverId, line.content(), line.lineNumber());
                    ctx.send(new WsEvent<>(eventData.type(), eventData));
                }
            }
        }
    }

    public record UnsubscribeLogMessageData(long serverId) implements EventData {
        @Override
        public String type() {
            return "UNSUBSCRIBE";
        }

        public static void handle(WsMessageContext ctx) throws JsonProcessingException {
            WsEvent<UnsubscribeLogMessageData> message = Main.objectMapper().readValue(ctx.message(), new TypeReference<>() {
            });
            long serverId = message.data().serverId;

            Set<Long> subscriptions = logSubscriptions.get(ctx.sessionId());
            if (subscriptions != null) {
                subscriptions.remove(serverId);
                if (subscriptions.isEmpty()) logSubscriptions.remove(ctx.sessionId());
            }
        }
    }

    public record UpsertConsoleLineEventData(long serverId, String line, int lineNumber) implements EventData {
        @Override
        public String type() {
            return "UPSERT_CONSOLE_LINE";
        }
    }

    public record ServerStateChangedEventData(long serverId, MinecraftServer.State state) implements EventData {
        @Override
        public String type() {
            return "SERVER_STATE_CHANGED";
        }
    }

    public record ServerLaunchedEventData(long serverId) implements EventData {
        @Override
        public String type() {
            return "SERVER_LAUNCHED";
        }
    }

    public interface EventData {
        String type();
    }
}
