package dev.fusionize.workflow.orchestrator.helpers;

import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Start component that continuously polls an inbox list for new emails.
 * When an email arrives, it triggers the workflow chain.
 */
public final class MockRecEmailComponentRuntime implements ComponentRuntime {
    private static final Logger logger = LoggerFactory.getLogger(MockRecEmailComponentRuntime.class);
    private final List<String> inbox;
    private String address;

    public MockRecEmailComponentRuntime(List<String> inbox) {
        this.inbox = inbox;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        this.address = config.varString("address").orElse(null);
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        try {
            Thread.sleep(100);
            emitter.logger().info("MockRecEmailComponentRuntime activated");
            emitter.success(context);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        while (true) {
            try {
                Thread.sleep(100);
                logger.info("inbox size: {}", inbox.size());

                if (!inbox.isEmpty()) {
                    // Remove the first email and process it
                    String email = inbox.removeFirst();
                    emitter.logger().info(
                            "MockRecEmailComponentRuntime handle email: {} from {}",
                            email, address);
                    context.set("email_message", email);
                    emitter.success(context);
                }

            } catch (InterruptedException e) {
                logger.error(e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                emitter.logger().error("Error processing email", e);
                emitter.failure(e);
                logger.error("Error processing email", e);
            }
        }
    }

}
