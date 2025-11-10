package dev.fusionize.orchestrator;
import dev.fusionize.user.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EchoController {
    private final EmailBoxService emailBoxService;

    public EchoController(EmailBoxService emailBoxService) {
        this.emailBoxService = emailBoxService;
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
}
