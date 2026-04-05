package dev.fusionize.workflow.component.local;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalComponentRegistrarTest {

    @Mock
    private ComponentRuntimeRegistry componentRuntimeRegistry;

    @Mock
    private LocalComponentRuntimeFactory<LocalComponentRuntime> factoryOne;

    @Mock
    private LocalComponentRuntimeFactory<LocalComponentRuntime> factoryTwo;

    @Test
    void shouldRegisterAllFactoriesOnConstruction() {
        // setup
        when(factoryOne.getName()).thenReturn("component-one");
        when(factoryTwo.getName()).thenReturn("component-two");

        // expectation
        new LocalComponentRegistrar(componentRuntimeRegistry, List.of(factoryOne, factoryTwo));

        // validation
        verify(componentRuntimeRegistry, times(2)).registerFactory(anyString(), any(ComponentRuntimeFactory.class));
    }

    @Test
    void shouldRegisterFactoryWithCorrectName() {
        // setup
        when(factoryOne.getName()).thenReturn("my-component");

        // expectation
        new LocalComponentRegistrar(componentRuntimeRegistry, List.of(factoryOne));

        // validation
        verify(componentRuntimeRegistry).registerFactory(eq("my-component"), any(ComponentRuntimeFactory.class));
    }

    @Test
    void shouldHandleEmptyFactoryList() {
        // setup
        List<LocalComponentRuntimeFactory<? extends LocalComponentRuntime>> emptyList = List.of();

        // expectation
        new LocalComponentRegistrar(componentRuntimeRegistry, emptyList);

        // validation
        verify(componentRuntimeRegistry, never()).registerFactory(anyString(), any(ComponentRuntimeFactory.class));
    }
}
