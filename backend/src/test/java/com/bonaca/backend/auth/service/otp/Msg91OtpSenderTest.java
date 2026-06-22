package com.bonaca.backend.auth.service.otp;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.context.annotation.Profile;

/**
 * Contract from Msg91OtpSender's class Javadoc: MSG91 DLT template registration hasn't happened
 * yet (docs/TECHNICAL_REQUIREMENTS.md §4), so this sender deliberately isn't implemented and is
 * gated to the "prod" profile so it can never be accidentally selected in dev/test.
 */
class Msg91OtpSenderTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    private final Msg91OtpSender sender = new Msg91OtpSender();

    @Test
    void sendThrowsBecauseTheMsg91IntegrationIsNotYetImplemented() {
        assertThatThrownBy(() -> sender.send("+919876543210", "1234"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("MSG91");
    }

    @Test
    void isOnlyActiveInTheProdProfile() {
        Profile profile = Msg91OtpSender.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("prod");
    }

    /**
     * Not a test of Msg91OtpSender — it doesn't make an HTTP call yet, so there's nothing of
     * its to stub. This documents the pattern for whoever wires up the real MSG91 call once a
     * DLT template is approved: stub MSG91's endpoint with WireMock and assert the sender hits
     * it correctly, the same way this proves the WireMock dependency itself works end to end.
     */
    @Test
    void wireMockScaffoldIsReadyForTheFutureRealMsg91HttpCall() throws Exception {
        wireMock.stubFor(post(urlEqualTo("/api/v5/otp/send"))
                .willReturn(aResponse().withStatus(200).withBody("{\"type\":\"success\"}")));

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create(wireMock.baseUrl() + "/api/v5/otp/send"))
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
    }
}
