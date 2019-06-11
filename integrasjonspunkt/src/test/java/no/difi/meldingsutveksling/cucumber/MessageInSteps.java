package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static no.difi.meldingsutveksling.NextMoveConsts.ALTINN_SBD_FILE;

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
                .setSbd(sbd);

        if ("Altinn".equals(who)) {
            message.attachment(new Attachment(new ByteArrayInputStream(body.getBytes()))
                    .setFileName(ALTINN_SBD_FILE)
                    .setMimeType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        }

        messageInHolder.set(message);
    }

    @And("^appends a file named \"([^\"]*)\" with mimetype=\"([^\"]*)\":$")
    public void appendsAFileNamedWithMimetype(String filename, String mimeType, String body) {
        Attachment attachment = new Attachment(new ByteArrayInputStream(body.getBytes()))
                .setFileName(filename)
                .setMimeType(mimeType);

        messageInHolder.get().attachment(attachment);
    }
}
