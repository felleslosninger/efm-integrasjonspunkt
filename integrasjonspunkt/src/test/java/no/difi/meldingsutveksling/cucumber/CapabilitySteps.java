package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

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

    @Then("^the returned capabilities are:$")
    public void theReturnedCapabilitiesAre(String expectedJson) throws JSONException {
        JSONAssert.assertEquals(expectedJson, response.getBody(), true);
    }

}
