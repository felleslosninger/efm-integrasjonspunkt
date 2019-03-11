package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.util.InMemoryResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RequiredArgsConstructor
public class NextMoveMessageOutSteps {

    private final TestRestTemplate testRestTemplate;

    @LocalServerPort
    private int localServerPort;

    private StandardBusinessDocument sbd;

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

        this.sbd = response.getBody();
    }

    @Given("^I upload a file named \"([^\"]+)\" with mimetype \"([^\"]+)\" and title \"([^\"]+)\" with the following body:$")
    public void iUploadAFileToTheMessage(String filename, String mimetype, String title, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add("file", new InMemoryResource(body));

        ResponseEntity<Void> response = testRestTemplate.exchange(
                UriComponentsBuilder.fromUriString("http://localhost:" + localServerPort)
                        .path("/api/message/out/")
                        .pathSegment(sbd.getConversationId())
                        .pathSegment("upload")
                        .queryParam("filename", filename)
                        .queryParam("mimetype", mimetype)
                        .queryParam("title", title)
                        .build()
                        .toUri(),
                HttpMethod.POST,
                new HttpEntity<>(bodyMap, headers),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
