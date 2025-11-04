package dev.fusionize.orchestrator.components;

import dev.fusionize.worker.component.annotations.RuntimeComponent;

@RuntimeComponent
public class MyCustomComponent {

    public void doSomething() {
        System.out.println("RuntimeComponent is working!");
    }
}
