package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType;
import org.hamcrest.MatcherAssert;
import org.xmlunit.matchers.CompareMatcher;

import java.util.List;

@RequiredArgsConstructor
public class NoArkSteps {

    private final Holder<List<String>> webServicePayloadHolder;
    private final XMLMarshaller xmlMarshaller;
    private final PutMessageRequestTypeParser putMessageRequestTypeParser;
    private final Holder<Message> messageSentHolder;

    @After
    public void after() {
        messageSentHolder.reset();
    }

    @Then("^an upload to Noark P360 is initiated with:$")
    @SneakyThrows
    public void anUploadToNoarkP360IsInitiatedWith(String expectedPayload) {
        List<String> payloads = webServicePayloadHolder.get();
        String actualPayload = payloads.get(0);

        MatcherAssert.assertThat(actualPayload.replaceAll("<payload>[^<]*</payload>", "<payload><!--payload--></payload>"),
            CompareMatcher.isIdenticalTo(expectedPayload).ignoreWhitespace());

        PutMessageRequestType putMessageRequestType = xmlMarshaller.unmarshall(actualPayload, PutMessageRequestType.class);

        messageSentHolder.set(putMessageRequestTypeParser.parse(putMessageRequestType));
    }
}
