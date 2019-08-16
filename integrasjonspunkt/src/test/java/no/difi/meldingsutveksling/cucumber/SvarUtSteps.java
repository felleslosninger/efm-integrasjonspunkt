package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ks.svarut.SendForsendelseMedId;
import no.difi.meldingsutveksling.ks.svarut.SvarUtRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(actualPayload.replaceAll("<data>[^<]*</data>", "<data><!--encrypted content--></data>"))
                .isXmlEqualTo(expectedPayload);

        SendForsendelseMedId sendForsendelseMedId = xmlMarshaller.unmarshall(actualPayload, SendForsendelseMedId.class);
        messageSentHolder.set(svarUtDataParser.parse(new SvarUtRequest(null, sendForsendelseMedId)));
    }
}
