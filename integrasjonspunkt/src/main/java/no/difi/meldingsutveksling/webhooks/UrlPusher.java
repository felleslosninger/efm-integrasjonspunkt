package no.difi.meldingsutveksling.webhooks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
        restTemplate.postForEntity(uri, new HttpEntity<>(jsonPayload, headers), String.class);
    }
}
