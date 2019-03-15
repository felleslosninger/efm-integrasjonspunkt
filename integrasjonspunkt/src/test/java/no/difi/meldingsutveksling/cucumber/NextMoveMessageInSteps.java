package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.altinn.MessagePolling;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Collections;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RequiredArgsConstructor
public class NextMoveMessageInSteps {

    private final TestRestTemplate testRestTemplate;
    private final MessagePolling messagePolling;
    private final Holder<Message> messageHolder;
    private final AltinnWsClient altinnWsClientMock;
    private final ObjectMapper objectMapper;
    private final IntegrasjonspunktNokkel keyInfo;

    private MessageBuilder messageBuilder;

    @Before
    public void before() {
        messageBuilder = new MessageBuilder(keyInfo);
    }

    @And("^Altinn prepares a message with the following SBD:$")
    public void altinnPreparesAMessageWithTheFollowingSBD(String body) throws IOException {
        messageBuilder.sbd(objectMapper.readValue(body, StandardBusinessDocument.class));
    }

    @And("^appends a file named \"([^\"]*)\" with mimetype=\"([^\"]*)\":$")
    public void appendsAFileNamedWithMimetype(String filename, String mimeType, String body) throws Throwable {
        messageBuilder.attachment(new Attachment(filename, mimeType, body.getBytes()));
    }

    @And("^Altinn sends the message$")
    public void altinnSendsTheMessage() {
        Message message = messageBuilder.build();
        messageHolder.set(message);
        given(altinnWsClientMock.availableFiles(any())).willReturn(
                Collections.singletonList(new FileReference("testMessage", 1)));
        given(altinnWsClientMock.download(any(), any())).willAnswer((Answer<StandardBusinessDocument>) invocation -> {
            StandardBusinessDocument sbd = messageHolder.get().getSbd();
            MessagePersister messagePersister = invocation.getArgument(1);
            messagePersister.write(sbd.getConversationId(), ASIC_FILE, message.getAsic());
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
