package com.bonaca.backend.auth.service.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Active whenever the real MSG91 sender isn't (default, dev, test). Logs the code instead of
 * sending an SMS — MSG91 needs a DLT-approved template registered before any real send is
 * possible (multi-day lead time, tracked separately; see docs/TECHNICAL_REQUIREMENTS.md).
 */
@Component
@Profile("!prod")
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void send(String phoneNumber, String code) {
        log.info("[dev-otp] OTP for {} is {}", phoneNumber, code);
    }
}
