package no.difi.meldingsutveksling.webhooks.subscription;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.webhooks.Subscription;
import no.difi.meldingsutveksling.exceptions.SubscriptionNotFoundException;
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
        webhookPusher.ping(subscription.getPushEndpoint());
        return subscriptionRepository.save(subscription);
    }

    @Override
    public void updateSubscription(Long id, Subscription subscription) {
        Subscription existingSubscription = getSubscription(id);

        Optional.ofNullable(subscription.getPushEndpoint()).ifPresent(pushEndpoint -> {
            if (!existingSubscription.getPushEndpoint().equals(pushEndpoint)) {
                webhookPusher.ping(subscription.getPushEndpoint());
            }
            existingSubscription.setPushEndpoint(pushEndpoint);
        });

        Optional.ofNullable(subscription.getName()).ifPresent(existingSubscription::setName);
        Optional.ofNullable(subscription.getResource()).ifPresent(existingSubscription::setResource);
        Optional.ofNullable(subscription.getEvent()).ifPresent(existingSubscription::setEvent);
        Optional.ofNullable(subscription.getFilter()).ifPresent(existingSubscription::setFilter);

        subscriptionRepository.save(existingSubscription);
    }

    @Override
    public void deleteSubscription(Long id) {
        subscriptionRepository.delete(getSubscription(id).getId());
    }
}
