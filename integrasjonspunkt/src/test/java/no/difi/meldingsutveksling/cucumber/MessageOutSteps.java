package no.difi.meldingsutveksling.cucumber;

import cucumber.api.DataTable;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class MessageOutSteps {

    private final Holder<Message> messageSentHolder;

    @Then("^the sent message contains the following files:$")
    @SneakyThrows
    public void theSentMessageContainsTheFollowingFiles(DataTable expectedTable) {
        Message message = messageSentHolder.get();

        List<List<String>> actualList = new ArrayList<>();
        actualList.add(Collections.singletonList("filename"));
        actualList.addAll(message.getAttachments().stream()
                .map(Attachment::getFileName)
                .map(Collections::singletonList)
                .collect(Collectors.toList())
        );

        DataTable actualTable = DataTable.create(actualList);
        expectedTable.diff(actualTable);
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
}
