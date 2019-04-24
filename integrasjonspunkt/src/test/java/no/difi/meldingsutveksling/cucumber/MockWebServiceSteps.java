package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceClientImpl;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.sdp.client2.SikkerDigitalPostKlient;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.xml.transform.StringSource;

import java.util.List;

import static org.springframework.ws.test.client.RequestMatchers.connectionTo;
import static org.springframework.ws.test.client.ResponseCreators.withPayload;

@RequiredArgsConstructor
public class MockWebServiceSteps {

    private final MockWebServiceServerCustomizer mockWebServiceServerCustomizer;
    private final CorrespondenceAgencyClient correspondenceAgencyClient;
    private final SvarUtWebServiceClientImpl svarUtWebServiceClient;
    private final CachingWebServiceTemplateFactory cachingWebServiceTemplateFactory;
    private final SikkerDigitalPostKlient sikkerDigitalPostKlient;
    private final Holder<List<String>> webServicePayloadHolder;

    @Before
    @SneakyThrows
    public void before() {
        mockWebServiceServerCustomizer.customize(correspondenceAgencyClient.getWebServiceTemplate());
        mockWebServiceServerCustomizer.customize(svarUtWebServiceClient.getWebServiceTemplate());
        mockWebServiceServerCustomizer.customize(cachingWebServiceTemplateFactory.getWebServiceTemplate());
        mockWebServiceServerCustomizer.customize(sikkerDigitalPostKlient.getMeldingTemplate());
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
        } else if (url.startsWith("http://localhost:3193")) {
            return sikkerDigitalPostKlient.getMeldingTemplate();
        }

        return svarUtWebServiceClient.getWebServiceTemplate();
    }

    @And("^a SOAP request to \"([^\"]*)\" will respond with the following payload:$")
    public void aSOAPRequestToWithActionWillRespondWith(String uri, String responsePayload) {
        getServer(uri).expect(connectionTo(uri))
                .andRespond(withPayload(new StringSource(responsePayload)));
    }
}
