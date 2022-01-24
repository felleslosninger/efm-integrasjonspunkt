package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.xmlunit.builder.DiffBuilder;

import java.util.List;

@RequiredArgsConstructor
public class SvarUtSteps {

    private final Holder<List<String>> webServicePayloadHolder;
    private final Holder<Message> messageSentHolder;

    @After
    public void after() {
        messageSentHolder.reset();
    }

    @Then("^an upload to Fiks is initiated with:$")
    @SneakyThrows
    public void anUploadToFiksInitiatedWith(String expectedPayload) {
        List<String> payloads = webServicePayloadHolder.get();

        MatcherAssert.assertThat(
                payloads.stream()
                        .map(this::hideData)
                        .anyMatch(p -> !DiffBuilder.compare(p)
                                .withTest(expectedPayload).ignoreWhitespace()
                                .build()
                                .hasDifferences()),
                Matchers.is(true));
    }

    private String hideData(String s) {
        return s.replaceAll("<data>.*</data>", "<data><!--encrypted content--></data>")
                .replaceAll(":ns2|ns2:", "")
                .replaceAll(":ns3|ns3:", "");
    }
}
