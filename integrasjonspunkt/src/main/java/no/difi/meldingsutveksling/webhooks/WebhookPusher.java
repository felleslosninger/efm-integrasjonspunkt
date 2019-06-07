package no.difi.meldingsutveksling.webhooks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.exceptions.WebhookPushEndpointPingFailedException;
import no.difi.meldingsutveksling.webhooks.event.WebhookEvent;
import no.difi.meldingsutveksling.webhooks.event.WebhookEventFactory;
import no.difi.meldingsutveksling.webhooks.subscription.SubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

import javax.transaction.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookPusher {

    private final SubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;
    private final UrlPusher urlPusher;
    private final WebhookEventFactory webhookEventFactory;

    @Transactional
    public void push(WebhookEvent event) {
        String json = getJson(event);
        subscriptionRepository.findAll().forEach(subscription -> urlPusher.pushAsync(subscription.getPushEndpoint(), json));
    }

    public void ping(String url) {
        try {
            urlPusher.push(url, getJson(webhookEventFactory.pingEvent()));
        } catch (HttpStatusCodeException e) {
            throw new WebhookPushEndpointPingFailedException(e);
        }
    }

    private String getJson(WebhookEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Could not convert MessageStatus to JSON!", e);
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}
