package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.serviceregistry.client.RestClient;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;

import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RequiredArgsConstructor
public class MockServerRestSteps {

    private final MockServerRestTemplateCustomizer mockServerRestTemplateCustomizer;
    private final RestClient restClient;

    private ResponseActions responseActions;

    @Before
    @SneakyThrows
    public void before() {
        mockServerRestTemplateCustomizer.getServers().values().forEach(MockRestServiceServer::reset);
        mockServerRestTemplateCustomizer.customize((RestTemplate) restClient.getRestTemplate());
    }

    private MockRestServiceServer getServer(String url) {
        return mockServerRestTemplateCustomizer.getServer(getRestTemplate(url));
    }

    @SuppressWarnings("squid:S1172")
    private RestTemplate getRestTemplate(String url) {
//        if (url.startsWith("http://localhost:9092")) {
//            return actuatorClient.getRestTemplate();
//        }

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


    @Given("^a \"([^\"]*)\" request to \"([^\"]*)\" will respond with connection refused$")
    public void aRequestToWillRespondWithNoConnectionRefused(String method, String url) {
        this.responseActions = getServer(url)
                .expect(manyTimes(), requestTo(url))
                .andExpect(method(HttpMethod.valueOf(method)));

        responseActions
                .andRespond(request -> {
                    throw new ConnectException("Connection refused: connect");
                });
    }
}