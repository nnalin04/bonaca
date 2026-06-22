package com.bonaca.backend.auth.service.otp;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Not implemented — MSG91 DLT template registration hasn't happened yet (multi-day lead time,
 * tracked separately; see docs/TECHNICAL_REQUIREMENTS.md §4). Wire the actual MSG91 API call
 * here once a template is approved. Only active in the "prod" profile so it can never be
 * accidentally selected in dev/test.
 */
@Component
@Profile("prod")
public class Msg91OtpSender implements OtpSender {

    @Override
    public void send(String phoneNumber, String code) {
        throw new UnsupportedOperationException(
                "MSG91 integration not implemented — DLT template registration pending");
    }
}
