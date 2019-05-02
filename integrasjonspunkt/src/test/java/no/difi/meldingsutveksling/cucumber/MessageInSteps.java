package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.boot.test.json.JacksonTester;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RequiredArgsConstructor
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

    @And("^\\w+ prepares a message with the following SBD:$")
    public void altinnPreparesAMessageWithTheFollowingSBD(String body) throws IOException {
        StandardBusinessDocument sbd = objectMapper.readValue(body, StandardBusinessDocument.class);
        messageInHolder.set(new Message()
                .setSbd(sbd));
    }

    @And("^appends a file named \"([^\"]*)\" with mimetype=\"([^\"]*)\":$")
    public void appendsAFileNamedWithMimetype(String filename, String mimeType, String body) {
        Attachment attachment = new Attachment(new ByteArrayInputStream(body.getBytes()))
                .setFileName(filename)
                .setMimeType(mimeType);

        messageInHolder.get().attachment(attachment);
    }
}
