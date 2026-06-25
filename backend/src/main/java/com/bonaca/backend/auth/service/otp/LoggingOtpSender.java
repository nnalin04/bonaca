package com.bonaca.backend.auth.service.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Fallback sender: always present; Msg91OtpSender is @Primary and takes over when
 * bonaca.msg91.auth-key is set. Logs the code to the console — safe for local dev
 * and remote-dev before DLT registration is complete.
 */
@Component
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void send(String phoneNumber, String code) {
        log.info("[dev-otp] OTP for {} is {}", phoneNumber, code);
    }
}
