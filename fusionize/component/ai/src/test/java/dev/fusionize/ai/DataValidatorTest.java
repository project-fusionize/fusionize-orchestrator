package dev.fusionize.ai;

import dev.fusionize.ai.service.DataValidatorService;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataValidatorTest {

    private DataValidator dataValidator;
    private DataValidatorService dataValidatorService;
    private Context context;
    private TestEmitter emitter;

    @BeforeEach
    void setUp() {
        dataValidatorService = mock(DataValidatorService.class);
        dataValidator = new DataValidator(dataValidatorService);
        context = new Context();
        emitter = new TestEmitter();
    }

    // --- configure tests ---

    @Test
    void configure_usesDefaults() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("rules", "all fields must be non-empty");
        dataValidator.configure(config);

        context.set("data", "content");
        dataValidator.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    @Test
    void configure_overridesInputAndOutputVars() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("rules", "validate it");
        config.set("input", "myInput");
        config.set("output", "myOutput");
        dataValidator.configure(config);

        context.set("myInput", "content");

        DataValidatorService.Response response = new DataValidatorService.Response(true, List.of(), List.of());
        when(dataValidatorService.validate(any())).thenReturn(response);

        dataValidator.run(context, emitter);

        assertTrue(emitter.successCalled);
        assertNotNull(context.getData().get("myOutput"));
    }

    // --- canActivate tests ---

    @Test
    void canActivate_failsWhenAgentNameMissing() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("rules", "some rules");
        dataValidator.configure(config);

        dataValidator.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Agent name"));
    }

    @Test
    void canActivate_failsWhenAgentNameEmpty() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "");
        config.set("rules", "some rules");
        dataValidator.configure(config);

        dataValidator.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
    }

    @Test
    void canActivate_failsWhenRulesMissing() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        dataValidator.configure(config);

        dataValidator.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Validation rules"));
    }

    @Test
    void canActivate_failsWhenRulesEmpty() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("rules", "");
        dataValidator.configure(config);

        dataValidator.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
    }

    @Test
    void canActivate_failsWhenInputNotInContext() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("rules", "validate fields");
        dataValidator.configure(config);

        dataValidator.canActivate(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("data"));
    }

    @Test
    void canActivate_succeedsWithAllConfig() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "testAgent");
        config.set("rules", "all fields required");
        dataValidator.configure(config);

        context.set("data", Map.of("name", "John"));
        dataValidator.canActivate(context, emitter);

        assertTrue(emitter.successCalled);
    }

    // --- run tests ---

    @Test
    void run_validatesSuccessfully_valid() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("rules", "name must not be empty, email must be valid");
        dataValidator.configure(config);

        context.set("data", Map.of("name", "John", "email", "john@example.com"));

        DataValidatorService.Response response = new DataValidatorService.Response(
                true, List.of(), List.of("Consider adding a phone number"));
        when(dataValidatorService.validate(any())).thenReturn(response);

        dataValidator.run(context, emitter);

        assertTrue(emitter.successCalled);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) context.getData().get("validation");
        assertNotNull(result);
        assertEquals(true, result.get("valid"));
    }

    @Test
    void run_validatesSuccessfully_invalid() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("rules", "email must be valid");
        dataValidator.configure(config);

        context.set("data", Map.of("email", "not-an-email"));

        DataValidatorService.Issue issue = new DataValidatorService.Issue("email", "error", "Invalid email format");
        DataValidatorService.Response response = new DataValidatorService.Response(
                false,
                List.of(issue),
                List.of("Use a valid email format like user@domain.com"));
        when(dataValidatorService.validate(any())).thenReturn(response);

        dataValidator.run(context, emitter);

        assertTrue(emitter.successCalled);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) context.getData().get("validation");
        assertNotNull(result);
        assertEquals(false, result.get("valid"));
        @SuppressWarnings("unchecked")
        List<DataValidatorService.Issue> issues = (List<DataValidatorService.Issue>) result.get("issues");
        assertEquals(1, issues.size());
    }

    @Test
    void run_handlesNullResponse() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("rules", "validate");
        dataValidator.configure(config);

        context.set("data", "content");
        when(dataValidatorService.validate(any())).thenReturn(null);

        dataValidator.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertInstanceOf(IllegalStateException.class, emitter.lastFailure);
    }

    @Test
    void run_handlesServiceException() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("rules", "validate");
        dataValidator.configure(config);

        context.set("data", "content");
        when(dataValidatorService.validate(any())).thenThrow(new RuntimeException("Connection timeout"));

        dataValidator.run(context, emitter);

        assertTrue(emitter.failureCalled);
        assertTrue(emitter.lastFailure.getMessage().contains("Connection timeout"));
    }

    @Test
    void run_handlesNullIssuesList() throws Exception {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set("agent", "mockAgent");
        config.set("rules", "validate");
        dataValidator.configure(config);

        context.set("data", "content");

        DataValidatorService.Response response = new DataValidatorService.Response(true, null, null);
        when(dataValidatorService.validate(any())).thenReturn(response);

        dataValidator.run(context, emitter);

        assertTrue(emitter.successCalled);
    }
}
