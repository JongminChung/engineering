package io.github.jongminchung.study.apicommunication.context;

import java.util.Optional;

/** ThreadLocal holder for propagating the request context across layers in the REST stack. */
public final class ApiRequestContextHolder {

    private static final ThreadLocal<ApiRequestContext> CONTEXT = new ThreadLocal<>();

    private ApiRequestContextHolder() {}

    public static void set(ApiRequestContext context) {
        CONTEXT.set(context);
    }

    public static Optional<ApiRequestContext> current() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static ApiRequestContext requireContext() {
        return current().orElseThrow(() -> new IllegalStateException("Request context is not bound to the thread"));
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
