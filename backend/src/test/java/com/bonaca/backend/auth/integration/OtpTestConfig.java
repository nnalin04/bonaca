package com.bonaca.backend.auth.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class OtpTestConfig {

    @Bean
    @Primary
    public FakeOtpSender fakeOtpSender() {
        return new FakeOtpSender();
    }
}
