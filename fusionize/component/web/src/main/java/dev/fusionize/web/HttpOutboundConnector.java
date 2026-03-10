package dev.fusionize.web;

import dev.fusionize.web.services.HttpOutboundConnectorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

public class HttpOutboundConnector implements ComponentRuntime {
    public static final String CONF_URL = "url";
    public static final String CONF_METHOD = "method";
    public static final String CONF_HEADERS = "headers";
    public static final String CONF_INPUT_VAR = "input";
    public static final String CONF_OUTPUT_VAR = "output";

    private final HttpOutboundConnectorService httpOutboundConnectorService;

    private String url;
    private HttpMethod method = HttpMethod.GET;
    private Map<String, String> headers = new HashMap<>();
    private String inputVar;
    private String outputVar = "httpResponse";

    public HttpOutboundConnector(HttpOutboundConnectorService httpOutboundConnectorService) {
        this.httpOutboundConnectorService = httpOutboundConnectorService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString(CONF_URL).ifPresent(s -> this.url = s);
        config.varString(CONF_METHOD).ifPresent(s -> this.method = HttpMethod.valueOf(s.toUpperCase()));
        config.varString(CONF_INPUT_VAR).ifPresent(s -> this.inputVar = s);
        config.varString(CONF_OUTPUT_VAR).ifPresent(s -> this.outputVar = s);
        config.varMap(CONF_HEADERS).ifPresent(m -> {
            this.headers = new HashMap<>();
            m.forEach((k, v) -> this.headers.put(k.toString(), v.toString()));
        });
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        if (url == null || url.isEmpty()) {
            emitter.failure(new IllegalArgumentException("URL not found in configs"));
            return;
        }
        if (inputVar != null && !context.contains(inputVar)) {
            emitter.failure(new IllegalArgumentException("Input '" + inputVar + "' not found in context"));
            return;
        }
        emitter.logger().info("HTTP outbound connector activated: {} {}", method, url);
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            String resolvedUrl = resolveUrl(context);
            Object body = inputVar != null ? context.getData().get(inputVar) : null;

            emitter.logger().info("Executing {} {}", method, resolvedUrl);

            HttpOutboundConnectorService.Request request = new HttpOutboundConnectorService.Request(
                    resolvedUrl, method, headers, body);

            HttpOutboundConnectorService.Response response = httpOutboundConnectorService.execute(request);

            emitter.logger().info("HTTP response: status={}, success={}", response.statusCode(), response.success());

            context.getData().put(outputVar, response.toMap());
            emitter.success(context);

        } catch (Exception e) {
            emitter.logger().error("Error executing HTTP request: {}", e.getMessage(), e);
            emitter.failure(e);
        }
    }

    private String resolveUrl(Context context) {
        String resolved = url;
        for (Map.Entry<String, Object> entry : context.getData().entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (resolved.contains(placeholder) && entry.getValue() != null) {
                resolved = resolved.replace(placeholder, entry.getValue().toString());
            }
        }
        return resolved;
    }
}
