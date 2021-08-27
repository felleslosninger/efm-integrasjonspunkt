package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import cucumber.api.DataTable;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.StringUtil;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class MessageOutSteps {

    private final Holder<Message> messageOutHolder;
    private final Holder<Message> messageSentHolder;
    private final ObjectMapper objectMapper;

    private JacksonTester<StandardBusinessDocument> json;

    @Before
    public void before() {
        JacksonTester.initFields(this, objectMapper);
        messageOutHolder.reset();
        messageSentHolder.reset();
    }

    @After
    public void beforeAndAfter() {
        messageOutHolder.reset();
        messageSentHolder.reset();
    }

    @Given("^the sender is \"([^\"]*)\"")
    public void givenTheSenderIs(String orgnr) {
        messageOutHolder.getOrCalculate(Message::new).setSender(orgnr);
    }

    @Given("^the receiver is \"([^\"]*)\"")
    public void givenTheRecieverIs(String orgnr) {
        messageOutHolder.getOrCalculate(Message::new).setReceiver(orgnr);
    }

    @Given("^the conversationId is \"([^\"]*)\"")
    public void givenTheConversationId(String conversationId) {
        messageOutHolder.getOrCalculate(Message::new).setConversationId(conversationId);
    }

    @Given("^the messageId is \"([^\"]*)\"")
    public void givenTheMessageId(String messageId) {
        messageOutHolder.getOrCalculate(Message::new).setMessageId(messageId);
    }

    @Given("^the payload is:$")
    public void thePayloadIs(String payload) {
        messageOutHolder.get().setBody(payload);
    }

    @Then("^the sent message contains the following files:$")
    @SneakyThrows
    public void theSentMessageContainsTheFollowingFiles(DataTable expectedTable) {
        Message message = messageSentHolder.get();

        List<List<String>> actualList = new ArrayList<>();
        actualList.add(Arrays.asList("filename", "content type"));
        actualList.addAll(message.getAttachments().stream()
                .map(p -> Arrays.asList(p.getFileName(), StringUtil.nonNull(p.getMimeType())))
                .collect(Collectors.toList())
        );

        DataTable actualTable = DataTable.create(actualList);
        expectedTable.diff(actualTable);
    }

    @Then("^the sent message contains no files$")
    @SneakyThrows
    public void theSentMessageContainsNoFiles() {
        Message message = messageSentHolder.get();
        assertThat(message.getAttachments()).isEmpty();
    }

    @Then("^the content of the file named \"([^\"]*)\" is:$")
    public void theContentOfTheFileNamedIs(String filename, String expectedContent) throws IOException {
        Message message = messageSentHolder.get();

        String actualContent = new String(IOUtils.toByteArray(message.getAttachment(filename).getInputStream()));
        assertThat(actualContent).isEqualToIgnoringWhitespace(expectedContent);
    }

    @Then("^the XML content of the file named \"([^\"]*)\" is:$")
    public void theXmlContentOfTheFileNamedIs(String filename, String expectedContent) throws IOException {
        Message message = messageSentHolder.get();
        String actualContent = new String(IOUtils.toByteArray(message.getAttachment(filename).getInputStream()));
        assertThat(actualContent).isXmlEqualTo(expectedContent);
    }

    @Then("^the XML payload of the message is:$")
    public void theXmlPayloadOfTheMessageIs(String expectedPayload) {
        Message message = messageSentHolder.get();
        assertThat(message.getBody()).isXmlEqualTo(expectedPayload);
    }

    @SneakyThrows
    @Then("^the sent message's SBD is:$")
    public void theSentMessagesSbdIs(String expectedSBD) {
        JsonContent<StandardBusinessDocument> actual = json.write(messageSentHolder.get().getSbd());
        assertThat(actual)
                .isStrictlyEqualToJson(expectedSBD);
    }

}
