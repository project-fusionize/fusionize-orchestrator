package dev.fusionize.web.services;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class HttpInboundConnectorService {
    public record HttpConnectorKey(String workflowKey, String workflowNodeKey) {
    }

    private final Map<HttpConnectorKey, Consumer<Map<String, Object>>> listeners = new ConcurrentHashMap<>();

    public void addListener(HttpConnectorKey key, Consumer<Map<String, Object>> listener) {
        listeners.put(key, listener);
    }

    public void removeListener(HttpConnectorKey key) {
        listeners.remove(key);
    }

    public boolean hasListener(HttpConnectorKey key) {
        return listeners.containsKey(key);
    }

    public void invoke(HttpConnectorKey key, Map<String, Object> body) {
        Consumer<Map<String, Object>> listener = listeners.get(key);
        if (listener != null) {
            listener.accept(body);
        }
    }
}
