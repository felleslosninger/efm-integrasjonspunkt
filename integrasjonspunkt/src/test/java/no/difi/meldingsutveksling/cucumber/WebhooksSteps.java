package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.webhooks.WebhookPublisher;
import no.difi.meldingsutveksling.webhooks.WebhookPusher;
import no.difi.meldingsutveksling.webhooks.event.MessageStatusContent;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doAnswer;

@RequiredArgsConstructor
public class WebhooksSteps {

    private final TestRestTemplate testRestTemplate;
    private final WebhookPublisher webhookPublisher;
    private final ObjectMapper objectMapper;
    private final WireMockServer wireMockServer;
    private final WebhookPusher webhookPusher;
    private final AtomicBoolean pushed = new AtomicBoolean(false);

    @Before
    public void before() {
        doAnswer((Answer<Void>) invocation -> {
            invocation.callRealMethod();
            pushed.set(true);
            return null;
        }).when(webhookPusher).push(Mockito.any());
    }

    @After
    public void after() {
        pushed.set(false);
    }

    @Given("^the endpoint \"([^\"]*)\" accepts posts$")
    public void theEndpointAcceptsPosts(String url) {
        wireMockServer.givenThat(post(urlEqualTo(url))
                .willReturn(aResponse().withStatus(200))
        );
    }

    @Given("^I create the following webhook subscription:$")
    public void iCreateTheFollowingWebhookSubscription(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/subscriptions",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(response.getStatusCode())
                .withFailMessage(response.toString())
                .isEqualTo(HttpStatus.OK);
    }

    @Given("^the following message status is published:$")
    public void theFollowingMessageStatusIsPublished(String body) throws IOException {
        MessageStatusContent event = objectMapper.readValue(body, MessageStatusContent.class);
        Conversation conversation = new Conversation()
                .setMessageId(event.getMessageId())
                .setConversationId(event.getConversationId())
                .setDirection(event.getDirection())
                .setServiceIdentifier(event.getServiceIdentifier());
        MessageStatus messageStatus = MessageStatus.of(ReceiptStatus.valueOf(event.getStatus()), event.getCreatedTs(), event.getDescription());
        webhookPublisher.publish(conversation, messageStatus);
    }

    @Then("^the following ping message is posted to \"([^\"]*)\":$")
    public void theFollowingPingMessageIsPostedTo(String url, String body) {
        wireMockServer.verify(1, postRequestedFor(urlEqualTo(url))
                .withHeader("Content-Type", equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(equalToJson(body))
        );
    }

    @Then("^the following message is posted to \"([^\"]*)\":$")
    public void theFollowingMessageIsPostedTo(String url, String body) {
        await().atMost(3L, TimeUnit.SECONDS)
                .pollInterval(1L, TimeUnit.SECONDS)
                .untilTrue(pushed);

        wireMockServer.verify(1, postRequestedFor(urlEqualTo(url))
                .withHeader("Content-Type", equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withRequestBody(equalToJson(body))
        );
    }
}
