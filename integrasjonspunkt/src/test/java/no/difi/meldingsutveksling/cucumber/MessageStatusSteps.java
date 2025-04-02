package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RequiredArgsConstructor
@SpringJUnitConfig
@TestPropertySource("classpath:CucumberStepsConfiguration.properties")
public class MessageStatusSteps {

    private final TestRestTemplate testRestTemplate;
    private ResponseEntity<String> response;

    @Autowired
    private Environment env;

    @BeforeEach
    public void before() {

    }

    @After
    public void after() {
        this.response = null;
    }

    @Given("^the message statuses for the conversation with id = \"([^\"]*)\" are:$")
    public void theMessageStatusesForTheConversationIdAre(String messageId, String expectedJson) throws JSONException {
        String username = env.getProperty("spring.security.user.name");
        String password = env.getProperty("spring.security.user.password");

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);

        this.response = testRestTemplate.exchange(
                "/api/statuses?messageId={messageId}",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class,
                messageId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            JSONAssert.assertEquals(replaceDatabaseIds(expectedJson), replaceDatabaseIds(response.getBody()), true);
        } catch (AssertionError e) {
            log.error(response.getBody());
            throw e;
        }
    }

    private String replaceDatabaseIds(String in) {
        return in.replaceAll("\"(id|convId)\" : \\d+", "\"$1\" : 1");
    }
}
