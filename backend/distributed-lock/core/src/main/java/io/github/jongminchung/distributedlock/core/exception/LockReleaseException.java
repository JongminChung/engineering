package io.github.jongminchung.distributedlock.core.exception;

public class LockReleaseException extends RuntimeException {
    public LockReleaseException(String message) {
        super(message);
    }

    public LockReleaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
