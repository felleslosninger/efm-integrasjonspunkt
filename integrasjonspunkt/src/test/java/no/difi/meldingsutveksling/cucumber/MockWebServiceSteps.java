package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentV2;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import org.apache.commons.io.IOUtils;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.xml.transform.StringSource;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.ws.test.client.RequestMatchers.connectionTo;
import static org.springframework.ws.test.client.ResponseCreators.withPayload;

@RequiredArgsConstructor
public class MockWebServiceSteps {

    private final CorrespondenceAgencyClient correspondenceAgencyClient;
    private final Holder<List<String>> webServicePayloadHolder;
    private final XMLMarshaller xmlMarshaller;
    private final Holder<Message> messageSentHolder;

    private MockWebServiceServer mockWebServiceServer;

    @Before
    @SneakyThrows
    public void before() {
        mockWebServiceServer = MockWebServiceServer.createServer(correspondenceAgencyClient);
    }

    @After
    public void after() {
        mockWebServiceServer.verify();
    }

    @Then("^the CorrespondenceAgencyClient is called with the following payload:$")
    public void theCorrespondenceAgencyClientIsCalledWithTheFollowingPayload(String expectedPayload) {
        List<String> payloads = webServicePayloadHolder.get();
        String actualPayload = payloads.get(0);
        assertThat(actualPayload).isXmlEqualTo(expectedPayload);

        InsertCorrespondenceV2 in = xmlMarshaller.unmarshall(actualPayload, InsertCorrespondenceV2.class);

        List<Attachment> attachments = in.getCorrespondence()
                .getContent().getValue()
                .getAttachments().getValue()
                .getBinaryAttachments().getValue()
                .getBinaryAttachmentV2()
                .stream()
                .map(p -> new Attachment()
                        .setFileName(p.getFileName().getValue())
                        .setBytes(getBytes(p))
                ).collect(Collectors.toList());

        messageSentHolder.set(new Message()
                .attachments(attachments));
    }

    @SneakyThrows
    private byte[] getBytes(BinaryAttachmentV2 p) {
        return IOUtils.toByteArray(p.getData().getValue().getInputStream());
    }

    @And("^a SOAP request to \"([^\"]*)\" will respond with the following payload:$")
    public void aSOAPRequestToWithActionWillRespondWith(String uri, String responsePayload) {
        mockWebServiceServer.expect(connectionTo(uri))
                .andRespond(withPayload(new StringSource(responsePayload)));
    }
}
