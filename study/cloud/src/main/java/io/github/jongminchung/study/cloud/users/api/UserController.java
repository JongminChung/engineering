package io.github.jongminchung.study.cloud.users.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.jongminchung.study.cloud.auth.AuthenticationService;
import io.github.jongminchung.study.cloud.auth.AuthorizationService;
import io.github.jongminchung.study.cloud.iam.AccessPrincipal;
import io.github.jongminchung.study.cloud.iam.Action;
import io.github.jongminchung.study.cloud.iam.Permission;
import io.github.jongminchung.study.cloud.users.domain.User;
import io.github.jongminchung.study.cloud.users.service.UserService;

@RestController
@RequestMapping("/users")
public class UserController {
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final UserService userService;

    public UserController(
            AuthenticationService authenticationService,
            AuthorizationService authorizationService,
            UserService userService) {
        this.authenticationService = authenticationService;
        this.authorizationService = authorizationService;
        this.userService = userService;
    }

    @PostMapping
    public UserResponse create(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @RequestBody UserCreateRequest request) {
        AccessPrincipal principal = authenticationService.authenticate(apiKey);
        authorizationService.require(principal, Permission.of("user", Action.CREATE));

        User user = userService.create(request.email(), request.displayName());
        return UserResponse.from(user);
    }

    @GetMapping("/{id}")
    public UserResponse find(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey, @PathVariable String id) {
        AccessPrincipal principal = authenticationService.authenticate(apiKey);
        authorizationService.require(principal, Permission.of("user", Action.READ));

        return UserResponse.from(userService.find(id));
    }
}
