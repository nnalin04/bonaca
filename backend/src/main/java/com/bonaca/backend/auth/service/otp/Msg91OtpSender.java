package com.bonaca.backend.auth.service.otp;

import com.bonaca.backend.auth.config.Msg91Properties;
import com.bonaca.backend.auth.exception.OtpDeliveryException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Sends OTPs via MSG91's v5 API. Active only when bonaca.msg91.auth-key is configured;
 * LoggingOtpSender takes over when it is absent (local/dev). Requires a DLT-approved
 * template — see docs/TECHNICAL_REQUIREMENTS.md §4 for registration steps.
 */
@Component
@EnableConfigurationProperties(Msg91Properties.class)
@ConditionalOnProperty(name = "bonaca.msg91.auth-key")
public class Msg91OtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(Msg91OtpSender.class);

    private final Msg91Properties properties;
    private final HttpClient httpClient;

    public Msg91OtpSender(Msg91Properties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void send(String phoneNumber, String code) {
        // MSG91 expects the number without the leading '+' (e.g. 919876543210)
        String mobile = phoneNumber.startsWith("+") ? phoneNumber.substring(1) : phoneNumber;

        String body = """
                {"template_id":"%s","mobile":"%s","otp":"%s"}"""
                .formatted(properties.templateId(), mobile, code);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.baseUrl() + "/api/v5/otp/send"))
                .header("authkey", properties.authKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("MSG91 returned HTTP {} for {}: {}", response.statusCode(), phoneNumber, response.body());
                throw new OtpDeliveryException("OTP could not be sent — please try again");
            }
            if (response.body().contains("\"type\":\"error\"")) {
                log.error("MSG91 error for {}: {}", phoneNumber, response.body());
                throw new OtpDeliveryException("OTP could not be sent — please try again");
            }
            log.debug("OTP dispatched to {} via MSG91", phoneNumber);
        } catch (IOException e) {
            log.error("MSG91 network error for {}: {}", phoneNumber, e.getMessage());
            throw new OtpDeliveryException("OTP could not be sent — please try again", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OtpDeliveryException("OTP could not be sent — please try again", e);
        }
    }
}
