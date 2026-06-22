package com.bonaca.backend.auth.integration;

import com.bonaca.backend.auth.service.otp.OtpSender;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Test double that captures sent codes instead of logging/delivering them. */
public class FakeOtpSender implements OtpSender {

    private final Map<String, String> lastCodeByPhone = new ConcurrentHashMap<>();

    @Override
    public void send(String phoneNumber, String code) {
        lastCodeByPhone.put(phoneNumber, code);
    }

    public String lastCodeFor(String phoneNumber) {
        String code = lastCodeByPhone.get(phoneNumber);
        if (code == null) {
            throw new IllegalStateException("No OTP was sent to " + phoneNumber);
        }
        return code;
    }
}
