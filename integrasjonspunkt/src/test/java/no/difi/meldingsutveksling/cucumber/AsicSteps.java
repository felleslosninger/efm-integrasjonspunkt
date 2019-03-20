package no.difi.meldingsutveksling.cucumber;

import cucumber.api.DataTable;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.domain.ByteArrayFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class AsicSteps {

    private final Holder<Message> messageHolder;

    @Then("^the sent ASIC contains the following files:$")
    @SneakyThrows
    public void theSentASICContains(DataTable expectedTable) {
        Message message = messageHolder.get();

        List<List<String>> actualList = new ArrayList<>();
        actualList.add(Collections.singletonList("filename"));
        actualList.addAll(message.getAttachments().stream()
                .map(ByteArrayFile::getFileName)
                .map(Collections::singletonList)
                .collect(Collectors.toList())
        );

        DataTable actualTable = DataTable.create(actualList);
        expectedTable.diff(actualTable);
    }

    @Then("^the content of the ASIC file named \"([^\"]*)\" is:$")
    public void theContentOfTheASICFileNamedIs(String filename, String expectedContent) {
        Message message = messageHolder.get();
        assertThat(new String(message.getAttachement(filename).getBytes()))
                .isEqualToIgnoringWhitespace(expectedContent);
    }

}
