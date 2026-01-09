package io.github.jongminchung.study.cloud.iam;

public record PolicyRule(String role, String resource, Action action) {
    public boolean matches(AccessPrincipal principal, Permission permission) {
        return principal.hasRole(role) && resource.equals(permission.resource()) && action == permission.action();
    }
}
