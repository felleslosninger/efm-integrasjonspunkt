package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RequiredArgsConstructor
public class NextMoveMessageOutSteps {

    private final TestRestTemplate testRestTemplate;
    private final Holder<StandardBusinessDocument> standardBusinessDocumentHolder;

    @Given("^I POST the following message:$")
    public void iPostTheFollowingMessage(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<StandardBusinessDocument> response = testRestTemplate.exchange(
                "/api/message/out",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                StandardBusinessDocument.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        standardBusinessDocumentHolder.set(response.getBody());
    }

    @Given("^I upload a file named \"([^\"]+)\" with mimetype \"([^\"]+)\" and title \"([^\"]+)\" with the following body:$")
    public void iUploadAFileToTheMessage(String filename, String mimetype, String title, String body) {
        uploadFile(filename, mimetype, title, body, false);
    }

    @Given("^I upload a primary document named \"([^\"]+)\" with mimetype \"([^\"]+)\" with the following body:$")
    public void iUploadAPrimaryDocument(String filename, String mimetype, String body) {
        uploadFile(filename, mimetype, null, body, true);
    }

    private void uploadFile(String filename, String mimetype, String title, String body, boolean primaryDocument) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("conversationId", standardBusinessDocumentHolder.get().getConversationId());
        uriVariables.put("filename", filename);
        uriVariables.put("mimetype", mimetype);
        uriVariables.put("title", title);
        uriVariables.put("primaryDocument", Boolean.toString(primaryDocument));

        ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api/message/out/{conversationId}/upload?filename={filename}&mimetype={mimetype}&title={title}",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Void.class,
                uriVariables);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Given("^I send the message$")
    public void iSendTheMessage() {
        ResponseEntity<Void> response = testRestTemplate.exchange(
                "/api/message/out/{conversationId}",
                HttpMethod.POST, new HttpEntity(null),
                Void.class,
                standardBusinessDocumentHolder.get().getConversationId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
