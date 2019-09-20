package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(actualPayload.replaceAll("<payload>[^<]*</payload>", "<payload><!--payload--></payload>"))
                .isXmlEqualTo(expectedPayload);

        PutMessageRequestType putMessageRequestType = xmlMarshaller.unmarshall(actualPayload, PutMessageRequestType.class);

        messageSentHolder.set(putMessageRequestTypeParser.parse(putMessageRequestType));
    }
}
