package dev.fusionize.orchestrator;

import dev.fusionize.user.AuthenticatedUser;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class EchoController {
    private final EmailBoxService emailBoxService;
    private final WebhookService webhookService;

    public EchoController(EmailBoxService emailBoxService,
            WebhookService webhookService) {
        this.emailBoxService = emailBoxService;
        this.webhookService = webhookService;
    }

    @GetMapping("/echo")
    public AuthenticatedUser echo(Authentication auth) {
        return AuthenticatedUser.getAuthenticatedUser(auth);
    }

    @PostMapping("/email")
    public String email(@RequestParam String message) {
        emailBoxService.addInbox(message);
        return "added";
    }

    @PostMapping("/webhook/{workflowKey}/{workflowNodeKey}")
    public String webhook(
            @RequestBody Map<String, Object> webhookBody,
            @PathVariable("workflowKey") String workflowKey,
            @PathVariable("workflowNodeKey") String workflowNodeKey) {
        webhookService.invoke(new WebhookService.WebhookKey(workflowKey, workflowNodeKey), webhookBody);
        return "added";
    }
}
