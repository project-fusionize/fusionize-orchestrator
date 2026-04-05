package dev.fusionize.web.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HttpOutboundConnectorService {
    private static final Logger log = LoggerFactory.getLogger(HttpOutboundConnectorService.class);

    private final RestClient restClient;

    public HttpOutboundConnectorService() {
        this.restClient = RestClient.create();
    }

    public HttpOutboundConnectorService(RestClient restClient) {
        this.restClient = restClient;
    }

    public record Request(
            String url,
            HttpMethod method,
            Map<String, String> headers,
            Object body) {
    }

    public record Response(
            int statusCode,
            Map<String, List<String>> headers,
            Object body,
            boolean success) implements Serializable {

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("statusCode", statusCode);
            map.put("headers", headers);
            map.put("body", body);
            map.put("success", success);
            return map;
        }
    }

    public Response execute(Request request) {
        try {
            RestClient.RequestBodySpec spec = restClient
                    .method(request.method())
                    .uri(request.url());

            if (request.headers() != null) {
                request.headers().forEach(spec::header);
            }

            if (request.body() != null && isBodyAllowed(request.method())) {
                spec.contentType(MediaType.APPLICATION_JSON);
                spec.body(request.body());
            }

            ResponseEntity<Object> responseEntity = spec
                    .retrieve()
                    .toEntity(Object.class);

            Map<String, List<String>> responseHeaders = new HashMap<>();
            HttpHeaders httpHeaders = responseEntity.getHeaders();
            httpHeaders.forEach(responseHeaders::put);

            return new Response(
                    responseEntity.getStatusCode().value(),
                    responseHeaders,
                    responseEntity.getBody(),
                    responseEntity.getStatusCode().is2xxSuccessful());

        } catch (RestClientResponseException e) {
            Map<String, List<String>> errorHeaders = new HashMap<>();
            HttpHeaders httpHeaders = e.getResponseHeaders();
            if (httpHeaders != null) {
                httpHeaders.forEach(errorHeaders::put);
            }
            return new Response(
                    e.getStatusCode().value(),
                    errorHeaders,
                    e.getResponseBodyAsString(),
                    false);
        }
    }

    private boolean isBodyAllowed(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
    }
}
