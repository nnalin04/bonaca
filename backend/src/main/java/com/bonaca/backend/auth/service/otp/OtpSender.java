package com.bonaca.backend.auth.service.otp;

/** Delivers an OTP code to a phone number. Implementations are swapped by Spring profile. */
public interface OtpSender {

    void send(String phoneNumber, String code);
}
