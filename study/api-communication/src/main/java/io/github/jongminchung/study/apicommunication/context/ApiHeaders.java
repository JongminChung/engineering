package io.github.jongminchung.study.apicommunication.context;

import io.grpc.Metadata;

/**
 * Shared header/metadata names for both HTTP and gRPC entry points.
 */
public final class ApiHeaders {

    public static final String API_KEY = "X-API-KEY";
    public static final String TENANT_ID = "X-TENANT-ID";
    public static final String CLIENT_ID = "X-CLIENT-ID";
    public static final String TRACE_ID = "X-TRACE-ID";

    public static final Metadata.Key<String> GRPC_API_KEY = Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> GRPC_TENANT_ID = Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> GRPC_CLIENT_ID = Metadata.Key.of("x-client-id", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> GRPC_TRACE_ID = Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    private ApiHeaders() {
    }
}
