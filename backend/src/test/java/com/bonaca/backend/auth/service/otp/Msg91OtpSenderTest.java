package com.bonaca.backend.auth.service.otp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bonaca.backend.auth.config.Msg91Properties;
import com.bonaca.backend.auth.exception.OtpDeliveryException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class Msg91OtpSenderTest {

    private static final String AUTH_KEY = "test-auth-key";
    private static final String TEMPLATE_ID = "test-template-id";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    private Msg91OtpSender senderWithBaseUrl(String baseUrl) {
        return new Msg91OtpSender(new Msg91Properties(AUTH_KEY, TEMPLATE_ID, baseUrl));
    }

    @Test
    void sendsCorrectRequestToMsg91() {
        wireMock.stubFor(post(urlEqualTo("/api/v5/otp/send"))
                .willReturn(aResponse().withStatus(200).withBody("{\"type\":\"success\"}")));

        assertThatCode(() -> senderWithBaseUrl(wireMock.baseUrl()).send("+919876543210", "5678"))
                .doesNotThrowAnyException();

        wireMock.verify(postRequestedFor(urlEqualTo("/api/v5/otp/send"))
                .withHeader("authkey", equalTo(AUTH_KEY))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("""
                        {"template_id":"test-template-id","mobile":"919876543210","otp":"5678"}""")));
    }

    @Test
    void stripsLeadingPlusFromPhoneNumber() {
        wireMock.stubFor(post(urlEqualTo("/api/v5/otp/send"))
                .willReturn(aResponse().withStatus(200).withBody("{\"type\":\"success\"}")));

        senderWithBaseUrl(wireMock.baseUrl()).send("+911234567890", "1234");

        wireMock.verify(postRequestedFor(urlEqualTo("/api/v5/otp/send"))
                .withRequestBody(equalToJson("""
                        {"template_id":"test-template-id","mobile":"911234567890","otp":"1234"}""")));
    }

    @Test
    void throwsOtpDeliveryExceptionOnNon200Response() {
        wireMock.stubFor(post(urlEqualTo("/api/v5/otp/send"))
                .willReturn(aResponse().withStatus(500).withBody("{\"type\":\"error\",\"message\":\"server error\"}")));

        assertThatThrownBy(() -> senderWithBaseUrl(wireMock.baseUrl()).send("+919876543210", "1234"))
                .isInstanceOf(OtpDeliveryException.class)
                .hasMessageContaining("could not be sent");
    }

    @Test
    void throwsOtpDeliveryExceptionWhenMsg91ReturnsErrorType() {
        wireMock.stubFor(post(urlEqualTo("/api/v5/otp/send"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"type\":\"error\",\"message\":\"Invalid template\"}")));

        assertThatThrownBy(() -> senderWithBaseUrl(wireMock.baseUrl()).send("+919876543210", "1234"))
                .isInstanceOf(OtpDeliveryException.class)
                .hasMessageContaining("could not be sent");
    }

    @Test
    void throwsOtpDeliveryExceptionWhenNetworkIsUnreachable() {
        assertThatThrownBy(() ->
                senderWithBaseUrl("http://127.0.0.1:1").send("+919876543210", "1234"))
                .isInstanceOf(OtpDeliveryException.class);
    }
}
