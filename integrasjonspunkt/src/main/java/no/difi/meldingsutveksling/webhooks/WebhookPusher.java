package no.difi.meldingsutveksling.webhooks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.webhooks.Subscription;
import no.difi.meldingsutveksling.exceptions.WebhookPushEndpointPingFailedException;
import no.difi.meldingsutveksling.webhooks.event.WebhookContent;
import no.difi.meldingsutveksling.webhooks.event.WebhookContentFactory;
import no.difi.meldingsutveksling.webhooks.event.WebhookEvent;
import no.difi.meldingsutveksling.webhooks.filter.WebhookFilterParser;
import no.difi.meldingsutveksling.webhooks.subscription.SubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;

import javax.transaction.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookPusher {

    private final SubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;
    private final UrlPusher urlPusher;
    private final WebhookContentFactory webhookContentFactory;
    private final WebhookFilterParser webhookFilterParser;

    @Transactional
    public void push(WebhookEvent event) {
        WebhookContent content = event.getContent();
        String json = getJson(content);
        subscriptionRepository.findAll()
                .forEach(subscription -> {
                    if (shouldPush(subscription, content)) {
                        urlPusher.pushAsync(subscription.getPushEndpoint(), json);
                    }
                });
    }

    private boolean shouldPush(Subscription subscription, WebhookContent content) {
        return acceptsResource(subscription, content)
                && acceptsEvent(subscription, content)
                && inFilter(subscription.getFilter(), content);
    }

    private boolean inFilter(String filter, WebhookContent content) {
        if (!StringUtils.hasText(filter)) {
            return true;
        }

        return webhookFilterParser.parse(filter)
                .filter(p -> p.getFilter().supports(content))
                .allMatch(p -> p.getFilter().matches(content, p.getOperator(), p.getValue()));
    }

    private boolean acceptsResource(Subscription subscription, WebhookContent content) {
        String resource = subscription.getResource();
        return "all".equals(resource) || resource.equals(content.getResource());
    }

    private boolean acceptsEvent(Subscription subscription, WebhookContent content) {
        String event = subscription.getEvent();
        return "all".equals(event) || event.equals(content.getEvent());
    }

    public void ping(String url) {
        try {
            urlPusher.push(url, getJson(webhookContentFactory.pingContent()));
        } catch (HttpStatusCodeException e) {
            throw new WebhookPushEndpointPingFailedException(e);
        }
    }

    private String getJson(WebhookContent content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            log.error("Could not convert MessageStatus to JSON!", e);
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }
}
