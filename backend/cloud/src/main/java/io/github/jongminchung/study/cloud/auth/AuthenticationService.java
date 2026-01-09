package io.github.jongminchung.study.cloud.auth;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import io.github.jongminchung.study.cloud.iam.AccessPrincipal;

@Component
public class AuthenticationService {
    private final Map<String, AccessPrincipal> principals = Map.of(
            "admin-key", new AccessPrincipal("admin", Set.of("ADMIN")),
            "user-key", new AccessPrincipal("user-1", Set.of("USER")));

    public AccessPrincipal authenticate(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AuthenticationFailedException("Missing API key");
        }

        AccessPrincipal principal = principals.get(apiKey);
        if (principal == null) {
            throw new AuthenticationFailedException("Invalid API key");
        }

        return principal;
    }
}
