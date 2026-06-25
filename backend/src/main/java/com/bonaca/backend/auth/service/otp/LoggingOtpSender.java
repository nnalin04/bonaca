package com.bonaca.backend.auth.service.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Fallback sender: active when MSG91 is not configured (bonaca.msg91.auth-key absent).
 * Logs the code to the console — useful for local dev and the remote-dev Oracle profile
 * before DLT registration is complete.
 */
@Component
@ConditionalOnMissingBean(OtpSender.class)
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void send(String phoneNumber, String code) {
        log.info("[dev-otp] OTP for {} is {}", phoneNumber, code);
    }
}
