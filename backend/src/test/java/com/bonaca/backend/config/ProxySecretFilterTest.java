package com.bonaca.backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ProxySecretFilterTest {

    private static final String SECRET = "remote-development-secret";

    @Test
    void allowsRequestsWhenProxySecurityIsDisabled() throws Exception {
        ProxySecretFilter filter = new ProxySecretFilter(new ProxySecurityProperties(false, ""));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void allowsHealthChecksWithoutAProxySecret() throws Exception {
        ProxySecretFilter filter = new ProxySecretFilter(new ProxySecurityProperties(true, SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsApiRequestsWithoutTheProxySecret() throws Exception {
        ProxySecretFilter filter = new ProxySecretFilter(new ProxySecurityProperties(true, SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/otp/request");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).isEqualTo("{\"message\":\"Forbidden\"}");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rejectsApiRequestsWithAnIncorrectProxySecret() throws Exception {
        ProxySecretFilter filter = new ProxySecretFilter(new ProxySecurityProperties(true, SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("X-Backend-Secret", "incorrect");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsApiRequestsWithTheCorrectProxySecret() throws Exception {
        ProxySecretFilter filter = new ProxySecretFilter(new ProxySecurityProperties(true, SECRET));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("X-Backend-Secret", SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
