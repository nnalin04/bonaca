package com.bonaca.backend.auth.service.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Contract: LoggingOtpSender is always present; Msg91OtpSender is @Primary and shadows it
 * when auth-key is configured. Must never throw — OTP requests must work without real SMS.
 */
class LoggingOtpSenderTest {

    private final LoggingOtpSender sender = new LoggingOtpSender();
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(LoggingOtpSender.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void sendDoesNotThrow() {
        assertThatCode(() -> sender.send("+919876543210", "1234")).doesNotThrowAnyException();
    }

    @Test
    void sendLogsThePhoneNumberAndCodeInsteadOfDeliveringAnSms() {
        sender.send("+919876543210", "4321");

        assertThat(appender.list)
                .anySatisfy(event -> assertThat(event.getFormattedMessage())
                        .contains("+919876543210")
                        .contains("4321"));
    }

    @Test
    void isAnUnconditionalComponentSoItIsAlwaysAvailableAsFallback() {
        assertThat(LoggingOtpSender.class.getAnnotation(Component.class)).isNotNull();
    }
}
