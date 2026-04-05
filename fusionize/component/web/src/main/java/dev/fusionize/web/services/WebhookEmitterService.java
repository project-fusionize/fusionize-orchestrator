package dev.fusionize.web.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

@Component
public class WebhookEmitterService {
    private static final Logger log = LoggerFactory.getLogger(WebhookEmitterService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RestClient restClient;

    public WebhookEmitterService() {
        this.restClient = RestClient.create();
    }

    public WebhookEmitterService(RestClient restClient) {
        this.restClient = restClient;
    }

    public record WebhookRequest(
            String url,
            Object payload,
            String secret,
            Map<String, String> headers) {
    }

    public record WebhookResponse(
            boolean delivered,
            int statusCode,
            String responseBody) implements Serializable {

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("delivered", delivered);
            map.put("statusCode", statusCode);
            map.put("responseBody", responseBody);
            return map;
        }
    }

    public WebhookResponse send(WebhookRequest request) {
        try {
            RestClient.RequestBodySpec spec = restClient
                    .post()
                    .uri(request.url())
                    .contentType(MediaType.APPLICATION_JSON);

            if (request.headers() != null) {
                request.headers().forEach(spec::header);
            }

            if (request.secret() != null && !request.secret().isEmpty()) {
                String signature = computeSignature(request.payload(), request.secret());
                spec.header("X-Webhook-Signature", signature);
            }

            if (request.payload() != null) {
                spec.body(request.payload());
            }

            ResponseEntity<String> responseEntity = spec
                    .retrieve()
                    .toEntity(String.class);

            boolean delivered = responseEntity.getStatusCode().is2xxSuccessful();
            return new WebhookResponse(
                    delivered,
                    responseEntity.getStatusCode().value(),
                    responseEntity.getBody());

        } catch (RestClientResponseException e) {
            return new WebhookResponse(
                    false,
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to send webhook to {}: {}", request.url(), e.getMessage());
            return new WebhookResponse(false, 0, e.getMessage());
        }
    }

    String computeSignature(Object payload, String secret) {
        try {
            String payloadString = payload != null ? payload.toString() : "";
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payloadString.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC signature", e);
        }
    }
}
