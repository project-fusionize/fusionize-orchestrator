package dev.fusionize.workflow.component.runtime;

import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ComponentRuntimeRegistryTest {

    private ComponentRuntimeRegistry registry;
    private WorkflowComponent component;
    private ComponentRuntimeConfig config;
    private ComponentRuntime runtime;

    @BeforeEach
    void setUp() {
        registry = new ComponentRuntimeRegistry();
        component = WorkflowComponent.builder("test")
                .withDomain("example")
                .withActor(Actor.SYSTEM)
                .withActor(Actor.AI)
                .build();
        config = new ComponentRuntimeConfig();
        runtime = new TestComponentRuntime();
    }

    @Test
    void testRegisterAndGet() {
        registry.register(component, config, runtime);

        // Check SYSTEM actor registration
        List<ComponentRuntime> systemResults = registry.findByActorAndDomain(Actor.SYSTEM, "test.example");
        assertEquals(1, systemResults.size());
        assertEquals(runtime, systemResults.get(0));

        // Check AI actor registration
        List<ComponentRuntime> aiResults = registry.findByActorAndDomain(Actor.AI, "test.example");
        assertEquals(1, aiResults.size());
        assertEquals(runtime, aiResults.get(0));
    }

    @Test
    void testFindByActor() {
        registry.register(component, config, runtime);

        List<ComponentRuntime> results = registry.findByActor(Actor.SYSTEM);
        assertEquals(1, results.size());
        assertEquals(runtime, results.get(0));

        results = registry.findByActor(Actor.AI);
        assertEquals(1, results.size());
        assertEquals(runtime, results.get(0));

        results = registry.findByActor(Actor.HUMAN);
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindByActorAndDomain() {
        registry.register(component, config, runtime);

        List<ComponentRuntime> results = registry.findByActorAndDomain(Actor.SYSTEM, "test.example");
        assertEquals(1, results.size());
        assertEquals(runtime, results.get(0));

        results = registry.findByActorAndDomain(Actor.SYSTEM, "other");
        assertTrue(results.isEmpty());
    }

    @Test
    void testFactoryRegistration() {
        ComponentRuntimeFactory<TestComponentRuntime> factory = TestComponentRuntime::new;
        registry.registerFactory(component, factory);

        assertTrue(registry.hasFactory(component));

        Optional<ComponentRuntime> created = registry.get(component, config);
        assertTrue(created.isPresent());
        assertTrue(created.get() instanceof TestComponentRuntime);
    }

    static class TestComponentRuntime implements ComponentRuntime {
        @Override
        public void run(Context context, ComponentUpdateEmitter emitter) {
        }

        @Override
        public void configure(ComponentRuntimeConfig config) {
        }

        @Override
        public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        }
    }
}
