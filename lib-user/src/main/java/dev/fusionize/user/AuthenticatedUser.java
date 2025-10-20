package dev.fusionize.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

public class AuthenticatedUser {
    private String userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;

    public static AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return getAuthenticatedUser(authentication);
    }

    public static AuthenticatedUser getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        Map<String, Object> claims = jwt.getClaims();
        AuthenticatedUser user = new AuthenticatedUser();

        user.setUserId(getClaimValue(claims, "sub"));
        user.setUsername(getClaimValue(claims, "preferred_username"));
        user.setEmail(getClaimValue(claims, "email"));
        user.setFirstName(getClaimValue(claims, "given_name"));
        user.setLastName(getClaimValue(claims, "family_name"));
        return user;
    }

    private static String getClaimValue(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        return value != null ? value.toString() : null;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
