package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.altinn.services.serviceengine.reporteeelementlist._2010._10.BinaryAttachmentV2;
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceClientImpl;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import org.apache.commons.io.IOUtils;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.xml.transform.StringSource;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.ws.test.client.RequestMatchers.connectionTo;
import static org.springframework.ws.test.client.ResponseCreators.withPayload;

@RequiredArgsConstructor
public class MockWebServiceSteps {

    private final MockWebServiceServerCustomizer mockWebServiceServerCustomizer;
    private final CorrespondenceAgencyClient correspondenceAgencyClient;
    private final SvarUtWebServiceClientImpl svarUtWebServiceClient;
    private final CachingWebServiceTemplateFactory cachingWebServiceTemplateFactory;
    private final Holder<List<String>> webServicePayloadHolder;

    @Before
    @SneakyThrows
    public void before() {
        mockWebServiceServerCustomizer.customize(correspondenceAgencyClient.getWebServiceTemplate());
        mockWebServiceServerCustomizer.customize(svarUtWebServiceClient.getWebServiceTemplate());
        mockWebServiceServerCustomizer.customize(cachingWebServiceTemplateFactory.getWebServiceTemplate());
    }

    @After
    public void after() {
        mockWebServiceServerCustomizer.getServers().values().forEach(MockWebServiceServer::verify);
        webServicePayloadHolder.reset();
    }

    private MockWebServiceServer getServer(String url) {
        return mockWebServiceServerCustomizer.getServer(getWebServiceTemplate(url));
    }

    private WebServiceTemplate getWebServiceTemplate(String url) {
        if (url.startsWith("http://localhost:9876")) {
            return correspondenceAgencyClient.getWebServiceTemplate();
        } else if (url.startsWith("http://localhost:8088/testExchangeBinding")) {
            return cachingWebServiceTemplateFactory.getWebServiceTemplate();
        }

        return svarUtWebServiceClient.getWebServiceTemplate();
    }

    @And("^a SOAP request to \"([^\"]*)\" will respond with the following payload:$")
    public void aSOAPRequestToWithActionWillRespondWith(String uri, String responsePayload) {
        getServer(uri).expect(connectionTo(uri))
                .andRespond(withPayload(new StringSource(responsePayload)));
    }
}
