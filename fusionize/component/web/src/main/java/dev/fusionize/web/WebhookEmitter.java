package dev.fusionize.web;

import dev.fusionize.web.services.WebhookEmitterService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

import java.util.HashMap;
import java.util.Map;

public class WebhookEmitter implements ComponentRuntime {
    public static final String CONF_URL = "url";
    public static final String CONF_INPUT_VAR = "input";
    public static final String CONF_OUTPUT_VAR = "output";
    public static final String CONF_SECRET = "secret";
    public static final String CONF_HEADERS = "headers";

    private final WebhookEmitterService webhookEmitterService;

    private String url;
    private String inputVar = "data";
    private String outputVar = "webhookResult";
    private String secret;
    private Map<String, String> headers = new HashMap<>();

    public WebhookEmitter(WebhookEmitterService webhookEmitterService) {
        this.webhookEmitterService = webhookEmitterService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString(CONF_URL).ifPresent(s -> this.url = s);
        config.varString(CONF_INPUT_VAR).ifPresent(s -> this.inputVar = s);
        config.varString(CONF_OUTPUT_VAR).ifPresent(s -> this.outputVar = s);
        config.varString(CONF_SECRET).ifPresent(s -> this.secret = s);
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
        if (!context.contains(inputVar)) {
            emitter.failure(new IllegalArgumentException("Input '" + inputVar + "' not found in context"));
            return;
        }
        emitter.logger().info("Webhook emitter activated, target: {}", url);
        emitter.success(context);
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            Object payload = context.getData().get(inputVar);
            String resolvedUrl = resolveUrl(url, context);

            emitter.logger().info("Sending webhook to {}", resolvedUrl);

            WebhookEmitterService.WebhookRequest request = new WebhookEmitterService.WebhookRequest(
                    resolvedUrl, payload, secret, headers);

            WebhookEmitterService.WebhookResponse response = webhookEmitterService.send(request);

            emitter.logger().info("Webhook result: delivered={}, status={}",
                    response.delivered(), response.statusCode());

            context.getData().put(outputVar, response.toMap());
            emitter.success(context);

        } catch (Exception e) {
            emitter.logger().error("Error sending webhook: {}", e.getMessage(), e);
            emitter.failure(e);
        }
    }

    private String resolveUrl(String template, Context context) {
        String resolved = template;
        for (Map.Entry<String, Object> entry : context.getData().entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (resolved.contains(placeholder) && entry.getValue() != null) {
                resolved = resolved.replace(placeholder, entry.getValue().toString());
            }
        }
        return resolved;
    }
}
