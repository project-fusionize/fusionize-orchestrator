package dev.fusionize.web.services;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@Component
public class WebhookService {
    public record WebhookKey(String workflowKey, String workflowNodeKey) {
        @Override
        public int hashCode() {
            return Objects.hash(workflowKey, workflowNodeKey);
        }
    }

    Map<WebhookKey, Consumer<Map<String, Object>>> listeners = new java.util.concurrent.ConcurrentHashMap<>();

    public void addListener(WebhookKey key, Consumer<Map<String, Object>> listener) {
        listeners.put(key, listener);
    }

    public void removeListener(WebhookKey key) {
        listeners.remove(key);
    }

    public void invoke(WebhookKey key, Map<String, Object> body) {
        Consumer<Map<String, Object>> listener = listeners.get(key);
        if (listener != null) {
            listener.accept(body);
        }
    }
}
