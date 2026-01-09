package io.github.jongminchung.study.cloud.iam;

public record Permission(String resource, Action action) {
    public static Permission of(String resource, Action action) {
        return new Permission(resource, action);
    }
}
