package no.difi.meldingsutveksling.webhooks.subscription;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.webhooks.QSubscription;
import no.difi.meldingsutveksling.domain.webhooks.Subscription;
import no.difi.meldingsutveksling.exceptions.SubscriptionNotFoundException;
import no.difi.meldingsutveksling.exceptions.WebhookPushEndpointAlreadyRegisteredException;
import no.difi.meldingsutveksling.webhooks.WebhookPusher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final WebhookPusher webhookPusher;

    @Override
    public Page<Subscription> listSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAll(pageable);
    }

    @Override
    public Subscription getSubscription(Long id) {
        return Optional.ofNullable(subscriptionRepository.findOne(id))
                .orElseThrow(() -> new SubscriptionNotFoundException(id));
    }

    @Override
    public Subscription createSubscription(Subscription subscription) {
        if (subscriptionRepository.findOne(QSubscription.subscription.pushEndpoint.eq(subscription.getPushEndpoint())) != null) {
            throw new WebhookPushEndpointAlreadyRegisteredException(subscription.getPushEndpoint());
        }

        webhookPusher.ping(subscription.getPushEndpoint());
        return subscriptionRepository.save(subscription);
    }

    @Override
    public void deleteSubscription(Long id) {
        subscriptionRepository.delete(getSubscription(id).getId());
    }
}
