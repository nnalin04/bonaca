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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * Contract from LoggingOtpSender's class Javadoc: fallback when no real OtpSender bean is
 * present (i.e. MSG91 auth-key not configured). Must never throw so OTP requests keep working
 * in dev/test without real SMS delivery.
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
    void isActivatedAsConditionalFallbackWhenNoOtpSenderBeanPresent() {
        ConditionalOnMissingBean conditional = LoggingOtpSender.class.getAnnotation(ConditionalOnMissingBean.class);

        assertThat(conditional).isNotNull();
        assertThat(conditional.value()).contains(OtpSender.class);
    }
}
