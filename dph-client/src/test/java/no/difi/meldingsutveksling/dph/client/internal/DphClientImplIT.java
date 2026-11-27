package no.difi.meldingsutveksling.dph.client.internal;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.dph.client.DphException;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.Notification;
import no.difi.meldingsutveksling.nhn.adapter.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Checks the requests to, and the responses from, the notification and incoming message endpoints of the NHN adapter.
 */
class DphClientImplIT {

    @RegisterExtension
    static WireMockExtension wireMockExtension = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private final Iso6523 onBehalfOf = Iso6523.parse("0192:987654321");

    private DphClientImpl dphClient;

    @BeforeEach
    void setUp() {
        CreateMaskinportenToken createMaskinportenToken = mock(CreateMaskinportenToken.class);
        given(createMaskinportenToken.createMaskinportenToken(onBehalfOf)).willReturn("dummy-token");

        dphClient = new DphClientImpl(
            WebClient.builder().baseUrl(wireMockExtension.baseUrl()).build(),
            new CreateMultipart(),
            mock(DphParcelService.class),
            new DphClientErrorHandlerImpl(),
            createMaskinportenToken
        );
    }

    @Test
    void getUnreadNotifications_decodes_notifications() {
        wireMockExtension.stubFor(get(urlEqualTo("/notifications/unread?herId=1111"))
            .withHeader("Authorization", equalTo("Bearer dummy-token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    [{"notificationId": "n-1", "relatedMessageId": "m-1", "type": "NEW_MESSAGE",
                      "notificationReceiverHerId": 1111, "notificationTriggeredByHerId": 2222,
                      "description": "New message", "createdAt": "2026-11-23T10:00:00+01:00"},
                     {"notificationId": "n-2", "relatedMessageId": null, "type": "MESSAGE_DELIVERY_STATE_UPDATED",
                      "notificationReceiverHerId": 1111, "notificationTriggeredByHerId": null,
                      "description": null, "createdAt": null}]""")));

        List<Notification> notifications = dphClient.getUnreadNotifications(onBehalfOf, 1111, null);

        assertThat(notifications).containsExactly(
            new Notification("n-1", "m-1", NotificationType.NEW_MESSAGE, 1111, 2222, "New message",
                OffsetDateTime.parse("2026-11-23T10:00:00+01:00")),
            new Notification("n-2", null, NotificationType.MESSAGE_DELIVERY_STATE_UPDATED, 1111, null, null, null));
    }

    @Test
    void getUnreadNotifications_asks_for_the_given_number_of_notifications() {
        wireMockExtension.stubFor(get(urlEqualTo("/notifications/unread?herId=1111&notificationsToFetch=10"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));

        assertThat(dphClient.getUnreadNotifications(onBehalfOf, 1111, 10)).isEmpty();
    }

    @Test
    void deleteNotifications_posts_the_notification_ids() {
        wireMockExtension.stubFor(post(urlEqualTo("/notifications/delete"))
            .willReturn(aResponse().withStatus(200)));

        dphClient.deleteNotifications(onBehalfOf, List.of("n-1", "n-2"));

        wireMockExtension.verify(1, postRequestedFor(urlEqualTo("/notifications/delete"))
            .withHeader("Authorization", equalTo("Bearer dummy-token"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson("{\"notificationIds\": [\"n-1\", \"n-2\"]}")));
    }

    @Test
    void getMessage_decodes_the_message() {
        wireMockExtension.stubFor(get(urlEqualTo("/messages/in/m-1?receiverHerId=1111"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"id": "m-1", "receiverHerId": 1111, "senderHerId": 2222, "businessDocumentId": "bd-1",
                     "businessDocumentDate": "2026-11-23T10:00:00+01:00", "isAppRec": true}""")));

        IncomingMessage message = dphClient.getMessage(onBehalfOf, 1111, "m-1");

        assertThat(message).isEqualTo(new IncomingMessage("m-1", 1111, 2222, "bd-1",
            OffsetDateTime.parse("2026-11-23T10:00:00+01:00"), true));
    }

    @Test
    void getMessage_reports_not_found() {
        wireMockExtension.stubFor(get(urlEqualTo("/messages/in/m-1?receiverHerId=1111"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"status": 404, "reason": "Not Found", "errorCode": "E21", "message": "Annen feil",
                     "path": "/api/messages/in/m-1", "requestId": "req-1", "timestamp": "2026-11-23T10:00:00Z"}""")));

        assertThatExceptionOfType(DphException.class)
            .isThrownBy(() -> dphClient.getMessage(onBehalfOf, 1111, "m-1"))
            .satisfies(e -> assertThat(e.isNotFound()).isTrue());
        wireMockExtension.verify(1, getRequestedFor(urlEqualTo("/messages/in/m-1?receiverHerId=1111")));
    }

    @Test
    void receiveBusinessDocument_reports_invalid_message_with_the_technical_code() {
        wireMockExtension.stubFor(post(urlEqualTo("/messages/in"))
            .willReturn(aResponse()
                .withStatus(422)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"status": 422, "reason": "Unprocessable Content", "errorCode": "invalid-xml-code", "message": "Ugyldig XML",
                     "technicalCode": "E7782", "path": "/api/messages/in", "requestId": "req-1",
                     "timestamp": "2026-11-23T10:00:00Z"}""")));

        assertThatExceptionOfType(DphException.class)
            .isThrownBy(() -> dphClient.receiveBusinessDocument(onBehalfOf, "jwe-token"))
            .satisfies(e -> {
                assertThat(e.getStatusCode()).isEqualTo(422);
                assertThat(e.getErrorCode()).isEqualTo("invalid-xml-code");
                assertThat(e.getTechnicalCode()).isEqualTo(DphException.INVALID_MESSAGE);
                assertThat(e.isInvalidMessage()).isTrue();
                assertThat(e.isInvalidApplicationReceipt()).isFalse();
            });
        // A client error is not retried
        wireMockExtension.verify(1, postRequestedFor(urlEqualTo("/messages/in")));
    }

    @Test
    void markAsDownloaded_posts_to_downloaded() {
        wireMockExtension.stubFor(post(urlEqualTo("/messages/in/m-1/downloaded?receiverHerId=1111"))
            .willReturn(aResponse().withStatus(200)));

        dphClient.markAsDownloaded(onBehalfOf, 1111, "m-1");

        wireMockExtension.verify(1, postRequestedFor(urlEqualTo("/messages/in/m-1/downloaded?receiverHerId=1111"))
            .withHeader("Authorization", equalTo("Bearer dummy-token")));
    }
}
