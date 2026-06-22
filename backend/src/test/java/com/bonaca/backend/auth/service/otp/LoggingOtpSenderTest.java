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
import org.springframework.context.annotation.Profile;

/**
 * Contract from LoggingOtpSender's class Javadoc: it's the stand-in for every non-prod profile
 * while MSG91's DLT template registration is pending (docs/TECHNICAL_REQUIREMENTS.md §4) — it
 * must never throw (OTP requests have to keep working in dev/test) and must log the code
 * somewhere a developer can read it, since there's no real SMS delivery.
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
    void isActiveInEveryProfileExceptProd() {
        Profile profile = LoggingOtpSender.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("!prod");
    }
}
