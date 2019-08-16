package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.junit.Before;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RequiredArgsConstructor
public class MessageStatusSteps {

    private final TestRestTemplate testRestTemplate;
    private ResponseEntity<String> response;

    @Before
    public void before() {

    }

    @After
    public void after() {
        this.response = null;
    }

    @Given("^the message statuses for the conversation with id = \"([^\"]*)\" are:$")
    public void theMessageStatusesForTheConversationIdAre(String messageId, String expectedJson) throws JSONException {
        this.response = testRestTemplate.exchange(
                "/api/statuses?messageId={messageId}",
                HttpMethod.GET,
                null,
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
