package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class CapabilitySteps {

    private final TestRestTemplate testRestTemplate;
    private ResponseEntity<String> response;

    @Before
    public void before() {

    }

    @After
    public void after() {
        this.response = null;
    }

    @Given("^I request all capabilities for \"([^\"]*)\"$")
    public void iRequestAllCapabilitiesFor(String receiverId) {
        this.response = testRestTemplate.exchange(
                "/api/capabilities/{receiverId}",
                HttpMethod.GET,
                null,
                String.class,
                receiverId);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Then("^the returned capabilities are:$")
    public void theReturnedCapabilitiesAre(String expectedJson) throws JSONException {
        JSONAssert.assertEquals(expectedJson, response.getBody(), true);
    }
}
