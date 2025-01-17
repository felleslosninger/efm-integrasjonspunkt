package no.difi.meldingsutveksling.webhooks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RequiredArgsConstructor
public class UrlPusher {

    private final RestTemplate restTemplate;

    @Async("threadPoolTaskScheduler")
    public void pushAsync(String uri, String jsonPayload) {
        push(uri, jsonPayload);
    }

    void push(String uri, String jsonPayload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        log.debug("Pushing to {}", uri);
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(jsonPayload, headers), String.class);
            log.debug("Response was {} {}", responseEntity.getStatusCode().value(), responseEntity.getStatusCode().getReasonPhrase());
        } catch (ResourceAccessException e) {
            log.warn("Webhook push failed for %s:".formatted(uri), e);
        }
    }
}