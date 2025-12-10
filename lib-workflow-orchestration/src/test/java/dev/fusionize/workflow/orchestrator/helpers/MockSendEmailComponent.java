package dev.fusionize.workflow.orchestrator.helpers;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

/**
 * Task component that sends emails.
 * Reads the target address from config and logs the email body.
 */
public final class MockSendEmailComponent implements ComponentRuntime {
    private String address;

    public MockSendEmailComponent() {
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        this.address = config.varString("address").orElse(null);
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {

        boolean hasMessage = context.contains("email_message");
        if (hasMessage) {
            emitter.logger().info("MockSendEmailComponent activated");
            emitter.success(context);
        } else {
            emitter.logger().info("MockSendEmailComponent not activated");
            emitter.failure(new Exception("No email to send"));
        }
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        try {
            emitter.logger().info("sending email to {}", address);
            Thread.sleep(10);
            emitter.logger().info("BODY: {}", context.varString("email_message").orElse(""));
            emitter.success(context);
        } catch (InterruptedException e) {
            emitter.failure(e);
        }
    }

}
