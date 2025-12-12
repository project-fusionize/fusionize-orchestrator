package dev.fusionize.orchestrator.components;

import dev.fusionize.orchestrator.EmailBoxService;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyCustomComponentRecEmail implements ComponentRuntime {
    private static final Logger logger = LoggerFactory.getLogger(MyCustomComponentRecEmail.class);
    private final EmailBoxService emailBoxService;
    private String address;

    MyCustomComponentRecEmail(EmailBoxService emailBoxService) {
        this.emailBoxService = emailBoxService;
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        this.address = config.getConfig().get("address").toString();
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        try {
            Thread.sleep(100);
            logger.info("MyCustomComponentRecEmail activated");
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
                if(!emailBoxService.getInbox().isEmpty()){
                    logger.info("inbox size: {}", emailBoxService.getInbox().size());
                }

                if (!emailBoxService.getInbox().isEmpty()) {
                    // Remove the first email and process it
                    String email = emailBoxService.getInbox().remove(0);

                    String worklog = "MockRecEmailComponentRuntime handle email: " + email;
                    logger.info(worklog);

                    context.getData().put("email_message", email);

                    // Trigger downstream workflow components
                    emitter.success(context);
                }

            } catch (InterruptedException e) {
                logger.error(e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error processing email", e);
            }
        }
    }

}
