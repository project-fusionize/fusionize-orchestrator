package dev.fusionize.orchestrator;
import dev.fusionize.user.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EchoController {

    @GetMapping("/echo")
    public AuthenticatedUser echo(@RequestParam String message, Authentication auth) {
        return AuthenticatedUser.getAuthenticatedUser(auth);
    }
}
