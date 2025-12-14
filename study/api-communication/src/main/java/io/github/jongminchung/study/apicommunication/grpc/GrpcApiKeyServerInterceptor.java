package io.github.jongminchung.study.apicommunication.grpc;

import io.github.jongminchung.study.apicommunication.context.ApiHeaders;
import io.github.jongminchung.study.apicommunication.context.ApiRequestContext;
import io.github.jongminchung.study.apicommunication.security.ApiKeyAuthenticator;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.util.Optional;
import java.util.UUID;

public class GrpcApiKeyServerInterceptor implements ServerInterceptor {

    private final ApiKeyAuthenticator apiKeyAuthenticator;

    public GrpcApiKeyServerInterceptor(ApiKeyAuthenticator apiKeyAuthenticator) {
        this.apiKeyAuthenticator = apiKeyAuthenticator;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        String apiKey = headers.get(ApiHeaders.GRPC_API_KEY);
        String tenantId = headers.get(ApiHeaders.GRPC_TENANT_ID);
        String clientId = headers.get(ApiHeaders.GRPC_CLIENT_ID);
        String traceId = Optional.ofNullable(headers.get(ApiHeaders.GRPC_TRACE_ID)).orElse(UUID.randomUUID().toString());

        return apiKeyAuthenticator.authenticate(tenantId, clientId, apiKey, traceId)
            .<ServerCall.Listener<ReqT>>map(context -> proceedWithContext(call, headers, next, context))
            .orElseGet(() -> reject(call, traceId));
    }

    private <ReqT, RespT> ServerCall.Listener<ReqT> proceedWithContext(ServerCall<ReqT, RespT> call,
                                                                       Metadata headers,
                                                                       ServerCallHandler<ReqT, RespT> next,
                                                                       ApiRequestContext context) {
        Context grpcContext = GrpcRequestContext.withContext(context);
        return Contexts.interceptCall(grpcContext, call, headers, next);
    }

    private <ReqT> ServerCall.Listener<ReqT> reject(ServerCall<ReqT, ?> call, String traceId) {
        Metadata metadata = new Metadata();
        metadata.put(ApiHeaders.GRPC_TRACE_ID, traceId);
        call.close(Status.UNAUTHENTICATED.withDescription("Invalid API credentials"), metadata);
        return new ServerCall.Listener<>() {
        };
    }
}
