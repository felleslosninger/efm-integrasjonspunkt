package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import org.xmlunit.assertj.XmlAssert;

import java.util.List;


@RequiredArgsConstructor
public class DigipostOutSteps {

    private final Holder<List<String>> webServicePayloadHolder;

    @Before
    public void before() {
    }

    @After
    public void after() {
    }

    @Then("^an upload to Digipost is initiated with:$")
    public void anUploadToDigipostIsInitiatedWith(String expectedPayload) {
        List<String> payloads = webServicePayloadHolder.get();
        String actualPayload = payloads.get(0);

        XmlAssert.assertThat(hideVolatiles(actualPayload))
                .and(hideVolatiles(expectedPayload))
                .ignoreWhitespace()
                .areSimilar();
    }

    private String hideVolatiles(String s) {
        return s.replaceAll("InstanceIdentifier>[^<]+", "InstanceIdentifier>")
                .replaceAll("CreationDateAndTime>[^<]+", "CreationDateAndTime>")
                .replaceAll("virkningstidspunkt>[^<]+", "virkningstidspunkt>")
                .replaceAll("DigestValue>[^<]+", "DigestValue>")
                .replaceAll("SignatureValue>[^<]+", "SignatureValue>")
                .replaceAll("X509Certificate>[^<]+", "X509Certificate>");
    }
}
