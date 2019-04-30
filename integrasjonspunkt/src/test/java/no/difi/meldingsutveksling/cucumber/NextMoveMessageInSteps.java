package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

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
    @SneakyThrows
    public void iPopTheLockedMessage() {
        RequestCallback requestCallback = request -> request.getHeaders()
                .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

        ResponseExtractor<Void> responseExtractor = response -> {
            messageReceivedHolder.get().attachments(asicParser.parse(response.getBody()));
            return null;
        };

        testRestTemplate.execute(
                "/api/messages/in/pop/{conversationId}",
                HttpMethod.GET,
                requestCallback, responseExtractor,
                Collections.singletonMap("conversationId", messageReceivedHolder.get().getSbd().getConversationId())
        );
    }

    @And("^I remove the message$")
    public void iRemoveTheMessage() {
        ResponseEntity<StandardBusinessDocument> response = testRestTemplate.exchange(
                "/api/messages/in/{conversationId}",
                HttpMethod.DELETE,
                new HttpEntity<>(null),
                StandardBusinessDocument.class,
                Collections.singletonMap("conversationId", messageReceivedHolder.get().getSbd().getConversationId())
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Then("^the converted SBD is:$")
    public void theReceivedSBDMatchesTheIncomingSBD(String expectedSBD) throws IOException {
        JsonContent<StandardBusinessDocument> actual = json.write(messageReceivedHolder.get().getSbd());
        assertThat(actual)
                .withFailMessage(actual.getJson())
                .isEqualToJson(expectedSBD);
    }

    @Then("^the received SBD matches the incoming SBD:$")
    public void theReceivedSBDMatchesTheIncomingSBD() throws IOException {
        JsonContent<StandardBusinessDocument> actual = json.write(messageInHolder.get().getSbd());
        assertThat(actual)
                .withFailMessage(actual.getJson())
                .isEqualToJson(json.write(messageReceivedHolder.get().getSbd()).getJson());
    }

    @And("^I have an ASIC that contains a file named \"([^\"]*)\" with mimetype=\"([^\"]*)\":$")
    public void iHaveAnASICThatContainsAFileNamedWithMimetype(String filename, String mimetype, String body) throws Throwable {
        Attachment attachement = messageReceivedHolder.get().getAttachment(filename);
        assertThat(attachement.getMimeType()).isEqualTo(mimetype);

        String actual = new String(IOUtils.toByteArray(attachement.getInputStream()));

        if (MediaType.APPLICATION_XML_VALUE.equals(mimetype)) {
            assertThat(actual).isXmlEqualTo(body);
        } else {
            assertThat(actual).isEqualTo(body);
        }
    }
}
