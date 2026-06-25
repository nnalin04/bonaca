package com.bonaca.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT auth: no sessions, no cookies, so CSRF protection (relevant only to
 * cookie-riding attacks) is disabled — Spring Security 7 enables it by default even for APIs,
 * but a Bearer-token-only API has nothing for CSRF to protect.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ProxySecurityProperties.class)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public ProxySecretFilter proxySecretFilter(ProxySecurityProperties properties) {
        return new ProxySecretFilter(properties);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, ProxySecretFilter proxySecretFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/health",
                                "/api/v1/auth/otp/request",
                                "/api/v1/auth/otp/verify",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout",
                                "/api/v1/webhooks/spike",
                                "/api/v1/webhooks/razorpay")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(proxySecretFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
