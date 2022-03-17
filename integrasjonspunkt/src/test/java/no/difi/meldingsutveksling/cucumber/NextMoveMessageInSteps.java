package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.dokumentpakking.service.AsicParser;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.move.common.io.ResourceUtils;
import org.hamcrest.MatcherAssert;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;
import static org.xmlunit.matchers.CompareMatcher.isIdenticalTo;

@RequiredArgsConstructor
@Slf4j
public class NextMoveMessageInSteps {

    private final TestRestTemplate testRestTemplate;
    private final ObjectMapper objectMapper;
    private final AsicParser asicParser;
    private final Holder<Message> messageInHolder;
    private final Holder<Message> messageReceivedHolder;

    private JacksonTester<StandardBusinessDocument> json;

    @Before
    public void before() {
        JacksonTester.initFields(this, objectMapper);
    }

    @After
    public void after() {
        messageReceivedHolder.reset();
    }

    @Given("^I peek and lock a message$")
    public void iPeekAndLockAMessage() {
        ResponseEntity<StandardBusinessDocument> response = testRestTemplate.exchange(
                "/api/messages/in/peek",
                HttpMethod.GET,
                null,
                StandardBusinessDocument.class);

        messageReceivedHolder.set(new Message()
                .setSbd(response.getBody()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @And("^I pop the locked message$")
    public void iPopTheLockedMessage() {
        RequestCallback requestCallback = request -> request.getHeaders()
                .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

        ResponseExtractor<Void> responseExtractor = response -> {
            try (InputStream inputStream = response.getBody()) {
                ByteArrayResource asic = new ByteArrayResource(StreamUtils.copyToByteArray(inputStream));
                messageReceivedHolder.get().attachments(asicParser.parse(asic));
                return null;
            }
        };

        testRestTemplate.execute(
                "/api/messages/in/pop/{messageId}",
                HttpMethod.GET,
                requestCallback, responseExtractor,
                Collections.singletonMap("messageId", messageReceivedHolder.get().getSbd().getMessageId())
        );
    }

    @And("^I remove the message$")
    public void iRemoveTheMessage() {
        ResponseEntity<StandardBusinessDocument> response = testRestTemplate.exchange(
                "/api/messages/in/{messageId}",
                HttpMethod.DELETE,
                new HttpEntity<>(null),
                StandardBusinessDocument.class,
                Collections.singletonMap("messageId", messageReceivedHolder.get().getSbd().getMessageId())
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Then("^the converted SBD is:$")
    public void theReceivedSBDMatchesTheIncomingSBD(String expectedSBD) throws IOException {
        JsonContent<StandardBusinessDocument> actual = json.write(messageReceivedHolder.get().getSbd());
        assertThat(actual)
                .withFailMessage(actual.getJson())
                .isStrictlyEqualToJson(expectedSBD);
    }

    @Then("^the received SBD matches the incoming SBD:$")
    public void theReceivedSBDMatchesTheIncomingSBD() throws IOException {
        JsonContent<StandardBusinessDocument> actual = json.write(messageInHolder.get().getSbd());
        assertThat(actual)
                .withFailMessage(actual.getJson())
                .isStrictlyEqualToJson(json.write(messageReceivedHolder.get().getSbd()).getJson());
    }

    @And("^I have an ASIC that contains a file named \"([^\"]*)\" with mimetype=\"([^\"]*)\":$")
    public void iHaveAnASICThatContainsAFileNamedWithMimetype(String filename, String mimetype, String body) {
        Document attachment = messageReceivedHolder.get().getAttachment(filename);
        assertThat(attachment.getMimeType()).hasToString(mimetype);

        String actual = new String(ResourceUtils.toByteArray(attachment.getResource()));

        if (MediaType.APPLICATION_XML_VALUE.equals(mimetype)) {
            MatcherAssert.assertThat(actual, isIdenticalTo(body).ignoreWhitespace());
        } else {
            MatcherAssert.assertThat(actual, equalToCompressingWhiteSpace(body));
        }
    }
}
