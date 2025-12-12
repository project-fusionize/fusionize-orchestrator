package dev.fusionize.orchestrator;

import dev.fusionize.user.AuthenticatedUser;

import dev.fusionize.web.services.FileInboundConnectorService;
import dev.fusionize.web.services.HttpInboundConnectorService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class EchoController {
    private final EmailBoxService emailBoxService;
    private final HttpInboundConnectorService httpInboundConnectorService;
    private final FileInboundConnectorService fileInboundConnectorService;

    public EchoController(EmailBoxService emailBoxService,
                          HttpInboundConnectorService httpInboundConnectorService,
                          FileInboundConnectorService fileInboundConnectorService) {
        this.emailBoxService = emailBoxService;
        this.httpInboundConnectorService = httpInboundConnectorService;
        this.fileInboundConnectorService = fileInboundConnectorService;
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

    @PostMapping("/http-inbound/{workflowKey}/{workflowNodeKey}")
    public String httpInbound(
            @RequestBody Map<String, Object> body,
            @PathVariable("workflowKey") String workflowKey,
            @PathVariable("workflowNodeKey") String workflowNodeKey) {
        httpInboundConnectorService.invoke(new HttpInboundConnectorService.HttpConnectorKey(workflowKey, workflowNodeKey), body);
        return "added";
    }

    @PostMapping("/file-inbound/{workflowKey}/{workflowNodeKey}")
    public String fileInbound(
            @RequestParam("file") MultipartFile file,
            @PathVariable("workflowKey") String workflowKey,
            @PathVariable("workflowNodeKey") String workflowNodeKey) {
        fileInboundConnectorService.invoke(new FileInboundConnectorService.IngestKey(workflowKey, workflowNodeKey), file);
        return "added";
    }
}
