package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.move.common.io.ResourceUtils;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.xmlunit.matchers.CompareMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                .map(p -> Arrays.asList(p.getFilename(), p.getMimeType() != null ? p.getMimeType().toString() : null))
                .toList()
        );

        DataTable actualTable = DataTable.create(actualList);
        expectedTable.diff(actualTable);
    }

    @Then("^the sent message contains no files$")
    @SneakyThrows
    public void theSentMessageContainsNoFiles() {
        Message message = messageSentHolder.get();
        assertTrue(message.getAttachments().isEmpty());
    }

    @Then("^the content of the file named \"([^\"]*)\" is:$")
    public void theContentOfTheFileNamedIs(String filename, String expectedContent) {
        Message message = messageSentHolder.get();

        String actualContent = new String(ResourceUtils.toByteArray(message.getAttachment(filename).getResource()));
        assertThat(actualContent, equalToCompressingWhiteSpace(expectedContent));
    }

    @Then("^the XML content of the file named \"([^\"]*)\" is:$")
    public void theXmlContentOfTheFileNamedIs(String filename, String expectedContent) {
        Message message = messageSentHolder.get();
        String actualContent = new String(ResourceUtils.toByteArray(message.getAttachment(filename).getResource()));
        assertThat(actualContent, CompareMatcher.isIdenticalTo(expectedContent).ignoreWhitespace());
    }

    @Then("^the XML payload of the message is:$")
    public void theXmlPayloadOfTheMessageIs(String expectedPayload) {
        Message message = messageSentHolder.get();
        assertThat(message.getBody(), CompareMatcher.isIdenticalTo(expectedPayload).ignoreWhitespace());
    }

    @SneakyThrows
    @Then("^the sent message's SBD is:$")
    public void theSentMessagesSbdIs(String expectedSBD) {
        JsonContent<StandardBusinessDocument> actual = json.write(messageSentHolder.get().getSbd());
        assertThat(actual.getJson(), jsonEquals(expectedSBD));
    }

}
