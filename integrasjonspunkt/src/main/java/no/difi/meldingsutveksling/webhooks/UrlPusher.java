package no.difi.meldingsutveksling.webhooks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
public class UrlPusher {

    private final RestTemplate restTemplate;

    @Async
    public void pushAsync(String uri, String jsonPayload) {
        push(uri, jsonPayload);
    }

    void push(String uri, String jsonPayload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        log.debug("Pushing to {}", uri);
        ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(jsonPayload, headers), String.class);
        log.debug("Response was {} {}", responseEntity.getStatusCode().value(), responseEntity.getStatusCode().getReasonPhrase());
    }
}