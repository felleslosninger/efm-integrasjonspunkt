package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;

import java.io.IOException;

import static no.difi.meldingsutveksling.NextMoveConsts.SBD_FILE;

@RequiredArgsConstructor
@Slf4j
public class MessageInSteps {

    private final Holder<Message> messageInHolder;
    private final ObjectMapper objectMapper;

    private JacksonTester<StandardBusinessDocument> json;

    @Before
    public void before() {
        JacksonTester.initFields(this, objectMapper);
    }

    @After
    public void after() {
        messageInHolder.reset();
    }

    @And("^(\\w+) prepares a message with the following SBD:$")
    public void altinnPreparesAMessageWithTheFollowingSBD(String who, String body) throws IOException {
        StandardBusinessDocument sbd = objectMapper.readValue(body, StandardBusinessDocument.class);
        Message message = new Message()
                .setServiceIdentifier(ServiceIdentifier.DPO)
                .setSbd(sbd);

        if ("Altinn".equals(who)) {
            message.attachment(new Attachment(body.getBytes())
                    .setFileName(SBD_FILE)
                    .setMimeType(MediaType.APPLICATION_JSON_VALUE));
        }

        messageInHolder.set(message);
    }

    @And("^appends a file named \"([^\"]*)\" with mimetype=\"([^\"]*)\":$")
    public void appendsAFileNamedWithMimetype(String filename, String mimeType, String body) {
        Attachment attachment = new Attachment(body.getBytes())
                .setFileName(filename)
                .setMimeType(mimeType);

        messageInHolder.get().attachment(attachment);
    }
}
