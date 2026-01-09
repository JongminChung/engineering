package io.github.jongminchung.study.cloud.users.api;

import io.github.jongminchung.study.cloud.users.domain.User;

public record UserResponse(String id, String email, String displayName) {
    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.email(), user.displayName());
    }
}
