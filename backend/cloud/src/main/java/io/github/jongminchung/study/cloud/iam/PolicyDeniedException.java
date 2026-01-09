package io.github.jongminchung.study.cloud.iam;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PolicyDeniedException extends RuntimeException {
    public PolicyDeniedException(String message) {
        super(message);
    }
}
