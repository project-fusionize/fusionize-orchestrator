package dev.fusionize.workflow.component.local;

public record LocalComponentBundle<T extends LocalComponentRuntime>(
        LocalComponentRuntimeFactory<T> factory, String name) {

    public boolean matches(String name) {
        return this.name.equalsIgnoreCase(name);
    }

    public T newInstance() {
        return factory.create();
    }
}
