package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.svarut.SvarUtClientHolder;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceServer;
import org.springframework.ws.test.client.RequestMatchers;
import org.springframework.xml.transform.StringSource;

import java.util.List;

import static org.springframework.ws.test.client.RequestMatchers.connectionTo;
import static org.springframework.ws.test.client.ResponseCreators.withPayload;

@RequiredArgsConstructor
public class MockWebServiceSteps {

    private final MockWebServiceServerCustomizer mockWebServiceServerCustomizer;
    //private final CorrespondenceAgencyClient correspondenceAgencyClient;
    private final SvarUtClientHolder svarUtClientHolder;
    private final Holder<List<String>> webServicePayloadHolder;
    private final IntegrasjonspunktProperties properties;

    @Before
    @SneakyThrows
    public void before() {
        //mockWebServiceServerCustomizer.customize(correspondenceAgencyClient.getWebServiceTemplate());
        mockWebServiceServerCustomizer.customize(svarUtClientHolder.getClient(properties.getOrg().getNumber()).getWebServiceTemplate());
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
            throw new IllegalStateException("return correspondenceAgencyClient.getWebServiceTemplate();");
        }

        return svarUtClientHolder.getClient(properties.getOrg().getNumber()).getWebServiceTemplate();
    }

    @And("^a SOAP request to \"([^\"]*)\" will respond with the following payload:$")
    public void aSOAPRequestToWithActionWillRespondWith(String uri, String responsePayload) {
        getServer(uri).expect(connectionTo(uri))
                .andRespond(withPayload(new StringSource(responsePayload)));
    }

    @And("^a SOAP request to \"([^\"]*)\" with element \"([^\"]*)\" will respond with the following payload:$")
    public void aSOAPRequestToWithRootElementAndActionWillRespondWith(String uri, String root, String responsePayload) {
        getServer(uri).expect(connectionTo(uri))
                .andExpect(RequestMatchers.xpath("//*[local-name()='" + root + "']").exists())
                .andRespond(withPayload(new StringSource(responsePayload)));
    }
}
