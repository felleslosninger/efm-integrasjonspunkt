package no.difi.meldingsutveksling.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.v2.ContentDisposition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.json.JsonContentAssert;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RequiredArgsConstructor
@SpringJUnitConfig
@TestPropertySource("classpath:CucumberStepsConfiguration.properties")
public class NextMoveMessageOutSteps {

    private final TestRestTemplate testRestTemplate;
    private final Holder<Message> messageOutHolder;
    private final ObjectMapper objectMapper;

    private MultiValueMap<String, HttpEntity<?>> multipart;
    private ResponseEntity<String> response;

    @Autowired
    private Environment env;

    @Before
    public void before() {
    }

    @After
    public void after() {
        messageOutHolder.reset();
        multipart = null;
        response = null;
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
    public void iPostTheMultipartRequest() throws IOException {
        String username = env.getProperty("spring.security.user.name");
        String password = env.getProperty("spring.security.user.password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBasicAuth(username, password);

        this.response = testRestTemplate.exchange(
                "/api/messages/out/multipart",
                HttpMethod.POST,
                new HttpEntity<>(multipart, headers),
                String.class);

        assertThat(response.getStatusCode())
                .withFailMessage(response.toString())
                .isEqualTo(HttpStatus.OK);

        messageOutHolder.getOrCalculate(Message::new)
                .setSbd(objectMapper.readValue(response.getBody(), StandardBusinessDocument.class));
    }

    @Then("^I post the multipart request and get a \"([^\"]+)\" response$")
    public void iPostTheMultipartRequestAndGetStatusResponse(String expectedStatusName) throws IOException {
        String username = env.getProperty("spring.security.user.name");
        String password = env.getProperty("spring.security.user.password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBasicAuth(username, password);

        this.response = testRestTemplate.exchange(
                "/api/messages/out/multipart",
                HttpMethod.POST,
                new HttpEntity<>(multipart, headers),
                String.class);

        assertThat(response.getStatusCode())
                .withFailMessage(response.toString())
                .isEqualTo(HttpStatus.valueOf(expectedStatusName));
    }

    @Given("^I POST the following message:$")
    public void iPostTheFollowingMessage(String body) throws IOException {
        String username = env.getProperty("spring.security.user.name");
        String password = env.getProperty("spring.security.user.password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(username, password);

        this.response = testRestTemplate.exchange(
                "/api/messages/out",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            StandardBusinessDocument sbd = objectMapper.readValue(response.getBody(), StandardBusinessDocument.class);

            messageOutHolder.getOrCalculate(Message::new)
                    .setSbd(sbd);
        }
    }

    @Then("^the response status is \"([^\"]+)\"")
    public void thenTheResponseStatusIs(String expectedStatusName) {
        assertThat(response.getStatusCode())
                .withFailMessage(response.getBody())
                .isEqualTo(HttpStatus.valueOf(expectedStatusName));
    }

    @Given("^I upload a file named \"([^\"]+)\" with mimetype \"([^\"]+)\" and title \"([^\"]+)\" with the following body:$")
    public void iUploadAFileToTheMessage(String filename, String mimetype, String title, String body) {
        String username = env.getProperty("spring.security.user.name");
        String password = env.getProperty("spring.security.user.password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(mimetype));
        headers.setBasicAuth(username, password);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder()
                .type("inline")
                .name(title)
                .filename(filename)
                .build()
                .toString()
        );

        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("messageId", messageOutHolder.get().getSbd().getMessageId());
        uriVariables.put("title", title);

        this.response = testRestTemplate.exchange(
                "/api/messages/out/{messageId}?title={title}",
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                String.class,
                uriVariables);

        assertThat(response.getStatusCode())
                .withFailMessage(response.toString())
                .isEqualTo(HttpStatus.OK);
    }

    @Given("^I send the message$")
    public void iSendTheMessage() {
        String username = env.getProperty("spring.security.user.name");
        String password = env.getProperty("spring.security.user.password");

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);

        this.response = testRestTemplate.exchange(
                "/api/messages/out/{messageId}",
                HttpMethod.POST, new HttpEntity<>(headers),
                String.class,
                messageOutHolder.get().getSbd().getMessageId());
        assertThat(response.getStatusCode())
                .withFailMessage(response.toString())
                .isEqualTo(HttpStatus.OK);
    }

    @Given("^I send the message and get the following error response:$")
    public void iSendTheMessageAndGetTheFollowingErrorResponse(String body) {
        String username = env.getProperty("spring.security.user.name");
        String password = env.getProperty("spring.security.user.password");

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);

        this.response = testRestTemplate.exchange(
                "/api/messages/out/{messageId}",
                HttpMethod.POST, new HttpEntity<>(headers),
                String.class,
                messageOutHolder.get().getSbd().getMessageId());

        try {
            new JsonContentAssert(String.class, response.getBody())
                    .isStrictlyEqualToJson(body);
        } catch (AssertionError e) {
            log.info(response.getBody());
            throw e;
        }
    }
}
