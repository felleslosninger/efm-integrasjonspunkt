package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.domain.ByteArrayFile;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.altinn.MessagePolling;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RequiredArgsConstructor
@Slf4j
public class NextMoveMessageInSteps {

    private final TestRestTemplate testRestTemplate;
    private final MessagePolling messagePolling;
    private final AltinnWsClient altinnWsClientMock;
    private final ObjectMapper objectMapper;
    private final AsicParser asicParser;
    private final IntegrasjonspunktNokkel keyInfo;

    private JacksonTester<StandardBusinessDocument> json;

    private Message altinnMessage;
    private Message receivedMessage;

    @Before
    public void before() {
        JacksonTester.initFields(this, objectMapper);
        altinnMessage = null;
        receivedMessage = null;
    }

    @And("^Altinn prepares a message with the following SBD:$")
    public void altinnPreparesAMessageWithTheFollowingSBD(String body) throws IOException {
        altinnMessage = new Message(keyInfo)
                .setSbd(objectMapper.readValue(body, StandardBusinessDocument.class));
    }

    @And("^appends a file named \"([^\"]*)\" with mimetype=\"([^\"]*)\":$")
    public void appendsAFileNamedWithMimetype(String filename, String mimeType, String body) throws Throwable {
        altinnMessage.attachment(new Attachment()
                .setFileName(filename)
                .setMimeType(mimeType)
                .setBytes(body.getBytes()));
    }

    @And("^Altinn sends the message$")
    public void altinnSendsTheMessage() {
        given(altinnWsClientMock.availableFiles(any())).willReturn(
                Collections.singletonList(new FileReference("testMessage", 1)));
        given(altinnWsClientMock.download(any(), any())).willAnswer((Answer<StandardBusinessDocument>) invocation -> {
            StandardBusinessDocument sbd = altinnMessage.getSbd();
            MessagePersister messagePersister = invocation.getArgument(1);
            messagePersister.write(sbd.getConversationId(), ASIC_FILE, altinnMessage.getAsic());
            return sbd;
        });
    }

    @Given("^the application checks for new Next Move DPO messages$")
    public void theApplicationChecksForNewNextMoveDPOMessages() throws MessageException {
        messagePolling.checkForNewMessages();
    }

    @Given("^I peek and lock a message$")
    public void iPeekAndLockAMessage() {
        ResponseEntity<StandardBusinessDocument> response = testRestTemplate.exchange(
                "/api/message/in/peek",
                HttpMethod.GET,
                null,
                StandardBusinessDocument.class);

        receivedMessage = new Message(keyInfo)
                .setSbd(response.getBody());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @And("^I pop the locked message$")
    @SneakyThrows
    public void iPopTheLockedMessage() {
        RequestCallback requestCallback = request -> request.getHeaders()
                .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

        ResponseExtractor<Void> responseExtractor = response -> {
            receivedMessage.attachments(asicParser.parse(response.getBody()));
            return null;
        };

        testRestTemplate.execute(
                "/api/message/in/pop/{conversationId}",
                HttpMethod.GET,
                requestCallback, responseExtractor,
                Collections.singletonMap("conversationId", receivedMessage.getSbd().getConversationId())
        );
    }

    @And("^I remove the message$")
    public void iRemoveTheMessage() {
        ResponseEntity<StandardBusinessDocument> response = testRestTemplate.exchange(
                "/api/message/in/{conversationId}",
                HttpMethod.DELETE,
                new HttpEntity<>(null),
                StandardBusinessDocument.class,
                Collections.singletonMap("conversationId", receivedMessage.getSbd().getConversationId())
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Then("^the received SBD matches the incoming SBD:$")
    public void theReceivedSBDMatchesTheIncomingSBD() throws IOException {
        assertThat(json.write(altinnMessage.getSbd()))
                .isEqualToJson(json.write(receivedMessage.getSbd()).getJson());
    }

    @And("^I have an ASIC that contains a file named \"([^\"]*)\" with mimetype=\"([^\"]*)\":$")
    public void iHaveAnASICThatContainsAFileNamedWithMimetype(String filename, String mimetype, String body) throws Throwable {
        ByteArrayFile attachement = receivedMessage.getAttachement(filename);
        assertThat(attachement.getMimeType()).isEqualTo(mimetype);
        assertThat(new String(attachement.getBytes())).isEqualTo(body);
    }
}
