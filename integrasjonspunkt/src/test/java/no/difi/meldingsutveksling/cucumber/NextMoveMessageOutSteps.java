package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.v2.ContentDisposition;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RequiredArgsConstructor
public class NextMoveMessageOutSteps {

    private final TestRestTemplate testRestTemplate;
    private final Holder<Message> messageOutHolder;

    private MultiValueMap<String, HttpEntity<?>> multipart;

    @Before
    public void before() {
    }

    @After
    public void after() {
        messageOutHolder.reset();
        multipart = null;
    }

    @Given("^I prepare a multipart request$")
    public void iPrepareAMultipartRequest() {
        multipart = new LinkedMultiValueMap<>();
    }

    @Given("^I add a part named \"([^\"]+)\" with content type \"([^\"]+)\" and body:$")
    @SneakyThrows
    public void iAddAMultipart(String name, String contentType, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(contentType));
        headers.setContentDispositionFormData(name, null);
        multipart.add(name, new HttpEntity<>(body, headers));
    }

    @Given("^I add a part named \"([^\"]+)\" and filename \"([^\"]+)\" with content type \"([^\"]+)\" and body:$")
    @SneakyThrows
    public void iAddAMultipartFile(String name, String filename, String contentType, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(contentType));
        headers.setContentDispositionFormData(name, filename);
        multipart.add(name, new HttpEntity<>(body, headers));
    }

    @Given("^I post the multipart request$")
    public void iPostTheMultipartRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<StandardBusinessDocument> response = testRestTemplate.exchange(
                "/api/message/out",
                HttpMethod.POST,
                new HttpEntity<>(multipart, headers),
                StandardBusinessDocument.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        messageOutHolder.getOrCalculate(Message::new)
                .setSbd(response.getBody());
    }

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

        messageOutHolder.getOrCalculate(Message::new)
                .setSbd(response.getBody());
    }

    @Given("^I upload a file named \"([^\"]+)\" with mimetype \"([^\"]+)\" and title \"([^\"]+)\" with the following body:$")
    public void iUploadAFileToTheMessage(String filename, String mimetype, String title, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(mimetype));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder()
                .type("inline")
                .name(title)
                .filename(filename)
                .build()
                .toString()
        );

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("conversationId", messageOutHolder.get().getSbd().getConversationId());
        uriVariables.put("title", title);

        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/message/out/{conversationId}/upload?title={title}",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class,
                uriVariables);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Given("^I send the message$")
    public void iSendTheMessage() {
        ResponseEntity<String> response = testRestTemplate.exchange(
                "/api/message/out/{conversationId}",
                HttpMethod.POST, new HttpEntity(null),
                String.class,
                messageOutHolder.get().getSbd().getConversationId());
        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
