package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ScriptComponentTest {

    private ScriptComponent component;
    private TestEmitter emitter;
    private Context context;

    @BeforeEach
    void setUp() {
        component = new ScriptComponent();
        emitter = new TestEmitter();
        context = new Context();
        context.setData(new ConcurrentHashMap<>());
    }

    @Test
    void testJsContextUpdate() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        // JS: Return a new object with updates
        config.set(ScriptComponent.CONF_SCRIPT, "context['foo']='js'; logger.info('asd')");
        config.set(ScriptComponent.CONF_PARSER, "js");
        component.configure(config);

        component.run(context, emitter);

        assertTrue(emitter.successCalled, "Script should succeed");
        assertNotNull(emitter.capturedContext, "Context should be captured");
        assertEquals("js", emitter.capturedContext.getData().get("foo"));
    }

    @Test
    void testJsContextUpdate_ReturnObject() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        // JS: Return a new object with updates
        config.set(ScriptComponent.CONF_SCRIPT, "var updates = ['foo', 'bar']; context['updates']=updates;");
        config.set(ScriptComponent.CONF_PARSER, "js");
        component.configure(config);

        component.run(context, emitter);

        assertTrue(emitter.successCalled, "Script should succeed");
        assertNotNull(emitter.capturedContext, "Context should be captured");
        assertEquals("foo", ((List)emitter.capturedContext.getData().get("updates")).getFirst());
    }

    @Test
    void testGroovyContextUpdate_DirectPut() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        // Groovy: Can update context directly (if supported) or return map
        // Based on previous debugging, Groovy might support direct put if context is
        // exposed as Map
        config.set(ScriptComponent.CONF_SCRIPT, "context.foo = 'groovy'; logger.info('asd');");
        config.set(ScriptComponent.CONF_PARSER, "groovy");
        component.configure(config);

        component.run(context, emitter);

        assertTrue(emitter.successCalled, "Script should succeed");
        assertEquals("groovy", emitter.capturedContext.getData().get("foo"));
    }

    @Test
    void testKotlinContextUpdate_DirectPut() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        // Groovy: Can update context directly (if supported) or return map
        // Based on previous debugging, Groovy might support direct put if context is
        // exposed as Map
        config.set(ScriptComponent.CONF_SCRIPT, "context[\"foo\"]= \"kotlin\"; logger.info(\"kotlin\");");
        config.set(ScriptComponent.CONF_PARSER, "kts");
        component.configure(config);

        component.run(context, emitter);

        assertTrue(emitter.successCalled, "Script should succeed");
        assertEquals("kotlin", emitter.capturedContext.getData().get("foo"));
    }

    @Test
    void testScriptFailure_InvalidSyntax() {
        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ScriptComponent.CONF_SCRIPT, "this is not valid code");
        config.set(ScriptComponent.CONF_PARSER, "js");
        component.configure(config);

        component.run(context, emitter);

        assertFalse(emitter.successCalled, "Script should fail");
        assertNotNull(emitter.capturedException, "Exception should be captured");
    }

    @Test
    void testJsContextUpdate_MergeExisting() {
        context.set("existing", "value");

        ComponentRuntimeConfig config = new ComponentRuntimeConfig();
        config.set(ScriptComponent.CONF_SCRIPT, "context.new = `new ${existing}`;");
        config.set(ScriptComponent.CONF_PARSER, "js");
        component.configure(config);

        component.run(context, emitter);

        assertTrue(emitter.successCalled);
        assertEquals("value", emitter.capturedContext.getData().get("existing"), "Existing data should be preserved");
        assertEquals("new value", emitter.capturedContext.getData().get("new"), "New data should be added");
    }

    static class TestEmitter implements ComponentUpdateEmitter {
        boolean successCalled = false;
        Context capturedContext;
        Exception capturedException;

        @Override
        public void success(Context updatedContext) {
            successCalled = true;
            capturedContext = updatedContext;
        }

        @Override
        public void failure(Exception ex) {
            capturedException = ex;
        }

        @Override
        public Logger logger() {
            return (message, level, throwable) -> {
                System.out.println(message);
            };
        }

        @Override
        public InteractionLogger interactionLogger() {
            return null;
        }
    }
}
