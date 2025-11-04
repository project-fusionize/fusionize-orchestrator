package dev.fusionize.orchestrator.oidc;

import dev.fusionize.user.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class OidcController {
    private final OidcTokenService tokenService;
    private final BasicAuthenticationConverter basicAuthConverter;

    public OidcController(OidcTokenService tokenService) {
        this.tokenService = tokenService;
        this.basicAuthConverter = new BasicAuthenticationConverter();
    }

    @GetMapping("/token/client")
    public ResponseEntity<Map<String, Object>> clientToken(
            HttpServletRequest request,
            @RequestHeader(value = "X-Token-Uri", required = false) String tokenUri,
            @RequestHeader(value = "X-Scope", required = false) String scope) {

        try {
            // Extract credentials using Spring Security's BasicAuthenticationConverter
            UsernamePasswordAuthenticationToken auth = basicAuthConverter.convert(request);

            if (auth == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authorization header is required"));
            }

            String clientId = auth.getName();
            String clientSecret = (String) auth.getCredentials();

            // Get token from service
            String token = tokenService.getClientCredentialsToken(
                    clientId,
                    clientSecret,
                    tokenUri,
                    scope
            );

            Map<String, Object> response = new HashMap<>();
            response.put("access_token", token);
            response.put("token_uri", tokenUri);
            response.put("scope", scope);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid authorization");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/token/password")
    public ResponseEntity<Map<String, Object>> passwordToken(HttpServletRequest request) {

        try {
            // Extract credentials using Spring Security's BasicAuthenticationConverter
            UsernamePasswordAuthenticationToken auth = basicAuthConverter.convert(request);

            if (auth == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Authorization header is required"));
            }

            String username = auth.getName();
            String password = (String) auth.getCredentials();

            // Get token from service
            String token = tokenService.getPasswordToken(
                    username,
                    password
            );

            Map<String, Object> response = new HashMap<>();
            response.put("access_token", token);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Invalid authorization");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
}
