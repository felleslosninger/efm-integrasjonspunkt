package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RequiredArgsConstructor
public class NextMoveMessageInSteps {

    private final TestRestTemplate testRestTemplate;

    private StandardBusinessDocument sbd;

    @Given("^I peek and lock a message$")
    public void iPeekAndLockAMessage() {
        ResponseEntity<StandardBusinessDocument> response = testRestTemplate.exchange(
                "/api/message/in/peek",
                HttpMethod.GET,
                null,
                StandardBusinessDocument.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        this.sbd = response.getBody();
    }
}
