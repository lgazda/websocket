package pl.net.gazda.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocket
@EnableScheduling
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myHandler(), "/websocket/endpoint")
            .setAllowedOrigins("*")
            .withSockJS();
    }

    @Bean
    public WebSocketTransactionsHandler myHandler() {
        return new WebSocketTransactionsHandler();
    }

    public static class WebSocketTransactionsHandler extends TextWebSocketHandler {
        private static final Logger LOG = LoggerFactory.getLogger(WebSocketTransactionsHandler.class);
        private final WebSocketSessions sessions = new WebSocketSessions();

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            LOG.info("Connection established. Session: {}", session.getId());
            sessions.add(session);
        }

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
            LOG.info("Text message received. Sending it back.");
            sessions.sendMessage(message);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            LOG.info("Connection closed. Session: {}", session.getId());
            sessions.remove(session);
        }

        public void broadcastMessage(TextMessage message) throws IOException {
            LOG.info("Sending message to all sessions. Message: {}", message.getPayload());
            sessions.sendMessage(message);
        }
    }

    public static class WebSocketSessions {
        private static final Logger LOG = LoggerFactory.getLogger(WebSocketTransactionsHandler.class);
        Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

        public synchronized void add(WebSocketSession session) {
            sessions.put(session.getId(), session);
        }

        public synchronized void remove(WebSocketSession session) {
            sessions.remove(session.getId());
        }

        public synchronized void sendMessage(TextMessage message) throws IOException {
            sessions.values().forEach(webSocketSession -> sendMessage(message, webSocketSession));
        }

        private void sendMessage(TextMessage message, WebSocketSession webSocketSession) {
            try {
                LOG.error("Sending message: {} to Session: {}", message.getPayload(), webSocketSession.getId());
                webSocketSession.sendMessage(message);
            } catch (IOException e) {
                LOG.error("Unable to send message to Session: {}", webSocketSession.getId(), e);
            }
        }
    }
}
