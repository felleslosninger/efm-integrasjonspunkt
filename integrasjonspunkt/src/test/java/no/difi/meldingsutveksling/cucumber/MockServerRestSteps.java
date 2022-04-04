package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnClient;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusRestClient;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusRestTemplate;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.UnorderedRequestExpectationManager;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RequiredArgsConstructor
public class MockServerRestSteps {

    private final RestClient restClient;
    private final ServiceBusRestTemplate serviceBusRestTemplate;
    private final ServiceBusRestClient serviceBusRestClient;
    private final SvarInnClient svarInnClient;

    private MockServerRestTemplateCustomizer mockServerRestTemplateCustomizer;
    private DefaultResponseCreator responseCreator;
    private ResponseActions responseActions;

    @Before
    @SneakyThrows
    public void before() {
        mockServerRestTemplateCustomizer = new MockServerRestTemplateCustomizer(UnorderedRequestExpectationManager.class);
        mockServerRestTemplateCustomizer.setDetectRootUri(false);
        mockServerRestTemplateCustomizer.customize((RestTemplate) restClient.getRestTemplate());
        mockServerRestTemplateCustomizer.customize(serviceBusRestTemplate);
        mockServerRestTemplateCustomizer.customize(svarInnClient.getRestTemplate());
    }

    @After
    public void after() {
        mockServerRestTemplateCustomizer.getServers().values().forEach(MockRestServiceServer::reset);
        mockServerRestTemplateCustomizer = null;
        responseActions = null;
    }

    private MockRestServiceServer getServer(String url) {
        return mockServerRestTemplateCustomizer.getServer(getRestTemplate(url));
    }

    @SuppressWarnings("squid:S1172")
    private RestTemplate getRestTemplate(String url) {
        if (url.startsWith(serviceBusRestClient.getBase())) {
            return serviceBusRestTemplate;
        } else if (url.startsWith(svarInnClient.getRootUri())) {
            return svarInnClient.getRestTemplate();
        }

        return (RestTemplate) restClient.getRestTemplate();
    }

    @Given("^a \"([^\"]*)\" request to \"([^\"]*)\" will respond with status \"(\\d+)\" and the following \"([^\"]*)\"$")
    public void aRequestToWillRespondWithStatusAndTheFollowing(String method, String url, int statusCode, String contentType, String body) {
        this.responseActions = getServer(url)
                .expect(manyTimes(), requestTo(url))
                .andExpect(method(HttpMethod.valueOf(method)));

        responseActions
                .andRespond(withStatus(HttpStatus.valueOf(statusCode))
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(body));
    }

    @Given("^a \"([^\"]*)\" request to \"([^\"]*)\" will respond with status \"(\\d+)\"$")
    public void aRequestToWillRespondWithStatus(String method, String url, int statusCode) {
        this.responseActions = getServer(url)
                .expect(manyTimes(), requestTo(url))
                .andExpect(method(HttpMethod.valueOf(method)));

        this.responseCreator = withStatus(HttpStatus.valueOf(statusCode));
        responseActions.andRespond(responseCreator);
    }

    @And("^the header \"([^\"]*)\"=\"([^\"]*)\"$")
    public void andTheHeader(String name, String value) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(name, value);

        responseActions
                .andRespond(responseCreator
                        .headers(httpHeaders));
    }

    @And("^the following \"([^\"]*)\"$")
    public void andTheFollowing(String contentType, String body) {
        responseActions
                .andRespond(responseCreator
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(body));
    }

    void andTheFollowing(String contentType, byte[] body) {
        responseActions
                .andRespond(responseCreator
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(body));
    }

    @And("^a \"([^\"]*)\" request to \"([^\"]*)\" will respond with status \"([^\"]*)\" and the following \"([^\"]*)\" in \"([^\"]*)\"$")
    public void aRequestToWillRespondWithStatusAndTheFollowingIn(String method, String url, int statusCode, String contentType, String path) {
        this.responseActions = getServer(url)
                .expect(manyTimes(), requestTo(url))
                .andExpect(method(HttpMethod.valueOf(method)));

        responseActions
                .andRespond(withStatus(HttpStatus.valueOf(statusCode))
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(new ClassPathResource(path)));
    }
}