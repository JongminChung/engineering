package io.github.jongminchung.study.cloud.iam;

import java.util.Set;

public record AccessPrincipal(String userId, Set<String> roles) {
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
