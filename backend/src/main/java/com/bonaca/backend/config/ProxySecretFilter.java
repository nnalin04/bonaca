package com.bonaca.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class ProxySecretFilter extends OncePerRequestFilter {

    static final String HEADER_NAME = "X-Backend-Secret";

    private final ProxySecurityProperties properties;

    public ProxySecretFilter(ProxySecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || "/health".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {
        String supplied = request.getHeader(HEADER_NAME);
        if (!matches(supplied, properties.secret())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Forbidden\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean matches(String supplied, String expected) {
        if (supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
