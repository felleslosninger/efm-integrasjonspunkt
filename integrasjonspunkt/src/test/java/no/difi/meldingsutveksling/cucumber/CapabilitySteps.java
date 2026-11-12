package no.difi.meldingsutveksling.cucumber;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
@SpringJUnitConfig
@TestPropertySource("classpath:CucumberStepsConfiguration.properties")
public class CapabilitySteps {

    private final TestRestTemplate testRestTemplate;
    private ResponseEntity<String> response;

    @Autowired
    private Environment env;

    @Before
    public void before() {

    }

    @After
    public void after() {
        this.response = null;
    }

    @Given("^I request all capabilities for \"([^\"]*)\"$")
    public void iRequestAllCapabilitiesFor(String receiverId) {
        String username = env.getProperty("spring.security.user.name");
        String password = env.getProperty("spring.security.user.password");

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);

        this.response = testRestTemplate.exchange(
                "/api/capabilities/{receiverId}",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class,
                receiverId);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Given("^I request all capabilities for \"([^\"]*)\" and get a \"([^\"]+)\" response$")
    public void iRequestAllCapabilitiesForAndGetResponse(String receiverId, String expectedStatusName) {
        this.response = get("/api/capabilities/{receiverId}", receiverId);
        assertStatus(expectedStatusName);
    }

    @Given("^I request the service record for \"([^\"]*)\" and get a \"([^\"]+)\" response$")
    public void iRequestTheServiceRecordForAndGetResponse(String identifier, String expectedStatusName) {
        this.response = get("/servicerecord/{identifier}", identifier);
        assertStatus(expectedStatusName);
    }

    @Then("^the lookup error message is \"([^\"]*)\"$")
    public void theLookupErrorMessageIs(String expectedMessage) {
        assertThat(JsonPath.<String>read(response.getBody(), "$.message"))
                .isEqualTo(expectedMessage);
    }

    private ResponseEntity<String> get(String url, String identifier) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(env.getProperty("spring.security.user.name"), env.getProperty("spring.security.user.password"));
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        return testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class, identifier);
    }

    private void assertStatus(String expectedStatusName) {
        assertThat(response.getStatusCode())
                .withFailMessage(response.toString())
                .isEqualTo(HttpStatus.valueOf(expectedStatusName));
    }

    @Then("^the returned capabilities are:$")
    public void theReturnedCapabilitiesAre(String expectedJson) throws JSONException {
        JSONAssert.assertEquals(expectedJson, response.getBody(), true);
    }

}
