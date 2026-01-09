package io.github.jongminchung.study.apicommunication.grpc;

import io.github.jongminchung.study.apicommunication.context.ApiRequestContext;
import io.grpc.Context;

public final class GrpcRequestContext {

    private static final Context.Key<ApiRequestContext> CONTEXT_KEY = Context.key("api-request-context");

    private GrpcRequestContext() {}

    public static Context withContext(ApiRequestContext context) {
        return Context.current().withValue(CONTEXT_KEY, context);
    }

    public static ApiRequestContext requireContext() {
        ApiRequestContext context = CONTEXT_KEY.get();
        if (context == null) {
            throw new IllegalStateException("gRPC request context is not available");
        }
        return context;
    }
}
