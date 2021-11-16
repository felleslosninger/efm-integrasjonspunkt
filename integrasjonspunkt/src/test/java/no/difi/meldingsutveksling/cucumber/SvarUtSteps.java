package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ks.svarut.SendForsendelseMedId;
import no.difi.meldingsutveksling.ks.svarut.SvarUtRequest;
import org.hamcrest.MatcherAssert;
import org.xmlunit.matchers.CompareMatcher;

import java.util.List;

@RequiredArgsConstructor
public class SvarUtSteps {

    private final Holder<List<String>> webServicePayloadHolder;
    private final XMLMarshaller xmlMarshaller;
    private final SvarUtDataParser svarUtDataParser;
    private final Holder<Message> messageSentHolder;

    @After
    public void after() {
        messageSentHolder.reset();
    }

    @Then("^an upload to Fiks is initiated with:$")
    @SneakyThrows
    public void anUploadToFiksInitiatedWith(String expectedPayload) {
        List<String> payloads = webServicePayloadHolder.get();
        String actualPayload = payloads.get(0);

        MatcherAssert.assertThat(actualPayload.replaceAll("<data>[^<]*</data>", "<data><!--encrypted content--></data>"),
            CompareMatcher.isIdenticalTo(expectedPayload).ignoreWhitespace());

        SendForsendelseMedId sendForsendelseMedId = xmlMarshaller.unmarshall(actualPayload, SendForsendelseMedId.class);
        messageSentHolder.set(svarUtDataParser.parse(new SvarUtRequest(null, sendForsendelseMedId)));
    }
}
