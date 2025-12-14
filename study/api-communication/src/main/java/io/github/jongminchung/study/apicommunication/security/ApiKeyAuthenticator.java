package io.github.jongminchung.study.apicommunication.security;

import io.github.jongminchung.study.apicommunication.context.ApiRequestContext;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Component
public class ApiKeyAuthenticator {

    private static final String SHA_256 = "SHA-256";
    private final ApiSecurityProperties properties;

    public ApiKeyAuthenticator(ApiSecurityProperties properties) {
        this.properties = properties;
    }

    public Optional<ApiRequestContext> authenticate(String tenantId, String clientId, String apiKey, String traceId) {
        if (tenantId == null || clientId == null || apiKey == null) {
            return Optional.empty();
        }

        return properties.findClient(tenantId, clientId)
            .filter(client -> matches(client.getHashedApiKey(), apiKey))
            .map(_ -> new ApiRequestContext(tenantId, clientId, traceId));
    }

    private boolean matches(String expectedHash, String candidateApiKey) {
        if (expectedHash == null || expectedHash.isBlank()) {
            return false;
        }
        return constantTimeEquals(expectedHash.toLowerCase(Locale.US), hash(candidateApiKey));
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash API key", ex);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expectedBytes.length; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }
}
