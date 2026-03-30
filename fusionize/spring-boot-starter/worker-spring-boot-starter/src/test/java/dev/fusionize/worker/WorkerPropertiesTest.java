package dev.fusionize.worker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkerPropertiesTest {

    @Test
    void shouldSetAndGetAllProperties() {
        // setup
        var props = new WorkerProperties();

        // expectation
        props.setOrchestratorUrl("http://localhost:8080");
        props.setOrchestratorMongo("mongodb://localhost:27017");
        props.setOrchestratorAmqp("amqp://localhost:5672");
        props.setOidcClientId("my-client");
        props.setOidcClientSecret("my-secret");
        props.setResourceRoot("/custom/resources");

        // validation
        assertThat(props.getOrchestratorUrl()).isEqualTo("http://localhost:8080");
        assertThat(props.getOrchestratorMongo()).isEqualTo("mongodb://localhost:27017");
        assertThat(props.getOrchestratorAmqp()).isEqualTo("amqp://localhost:5672");
        assertThat(props.getOidcClientId()).isEqualTo("my-client");
        assertThat(props.getOidcClientSecret()).isEqualTo("my-secret");
        assertThat(props.getResourceRoot()).isEqualTo("/custom/resources");
    }

    @Test
    void shouldDefaultResourceRootToResources() {
        // setup
        var props = new WorkerProperties();

        // expectation & validation
        assertThat(props.getResourceRoot()).isEqualTo("resources");
    }

    @Test
    void shouldConvertHttpToWs() {
        // setup
        var httpUrl = "http://localhost:8080/ws";

        // expectation
        var result = WorkerProperties.toWebSocketUrl(httpUrl);

        // validation
        assertThat(result).isEqualTo("ws://localhost:8080/ws");
    }

    @Test
    void shouldConvertHttpsToWss() {
        // setup
        var httpsUrl = "https://example.com/ws";

        // expectation
        var result = WorkerProperties.toWebSocketUrl(httpsUrl);

        // validation
        assertThat(result).isEqualTo("wss://example.com/ws");
    }

    @Test
    void shouldKeepWsUrl() {
        // setup
        var wsUrl = "ws://localhost:8080/ws";

        // expectation
        var result = WorkerProperties.toWebSocketUrl(wsUrl);

        // validation
        assertThat(result).isEqualTo("ws://localhost:8080/ws");
    }

    @Test
    void shouldKeepWssUrl() {
        // setup
        var wssUrl = "wss://example.com/ws";

        // expectation
        var result = WorkerProperties.toWebSocketUrl(wssUrl);

        // validation
        assertThat(result).isEqualTo("wss://example.com/ws");
    }

    @Test
    void shouldThrowForNullUrl() {
        // setup & expectation & validation
        assertThatThrownBy(() -> WorkerProperties.toWebSocketUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URL cannot be null or empty");
    }

    @Test
    void shouldThrowForEmptyUrl() {
        // setup & expectation & validation
        assertThatThrownBy(() -> WorkerProperties.toWebSocketUrl(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URL cannot be null or empty");
    }

    @Test
    void shouldThrowForUnsupportedScheme() {
        // setup & expectation & validation
        assertThatThrownBy(() -> WorkerProperties.toWebSocketUrl("ftp://example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported scheme: ftp");
    }

    @Test
    void shouldPreservePathAndPort() {
        // setup
        var httpUrl = "http://example.com:9090/api/v1/ws";

        // expectation
        var result = WorkerProperties.toWebSocketUrl(httpUrl);

        // validation
        assertThat(result).isEqualTo("ws://example.com:9090/api/v1/ws");
    }
}
