package io.github.jongminchung.study.apicommunication.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.jongminchung.study.apicommunication.context.ApiHeaders;
import io.github.jongminchung.study.apicommunication.context.ApiRequestContext;
import io.github.jongminchung.study.apicommunication.context.ApiRequestContextHolder;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyAuthenticator apiKeyAuthenticator;

    public ApiKeyAuthFilter(ApiKeyAuthenticator apiKeyAuthenticator) {
        this.apiKeyAuthenticator = apiKeyAuthenticator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI() != null && request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = Optional.ofNullable(request.getHeader(ApiHeaders.TRACE_ID))
                .orElse(UUID.randomUUID().toString());
        response.setHeader(ApiHeaders.TRACE_ID, traceId);
        try {
            ApiRequestContext context = apiKeyAuthenticator
                    .authenticate(
                            request.getHeader(ApiHeaders.TENANT_ID),
                            request.getHeader(ApiHeaders.CLIENT_ID),
                            request.getHeader(ApiHeaders.API_KEY),
                            traceId)
                    .orElseThrow(() -> new ApiAuthenticationException("Invalid API credentials"));

            ApiRequestContextHolder.set(context);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    context.clientId(), null, List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
            authenticationToken.setDetails(context);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (ApiAuthenticationException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorizedResponse(response, traceId, ex.getMessage());
        } finally {
            ApiRequestContextHolder.clear();
        }
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String traceId, String message)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"traceId\":\"" + traceId + "\",\"message\":\"" + message + "\"}");
    }
}
