package no.difi.meldingsutveksling.webhooks.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.webhooks.Subscription;
import no.difi.meldingsutveksling.exceptions.SubscriptionNotFoundException;
import no.difi.meldingsutveksling.exceptions.SubscriptionWithSameNameAndPushEndpointAlreadyExists;
import no.difi.meldingsutveksling.webhooks.WebhookPusher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
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
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new SubscriptionNotFoundException(id));
    }

    @Override
    public Subscription createSubscription(Subscription subscription) {
        subscriptionRepository.getDistinctByNameAndPushEndpoint(subscription.getName(), subscription.getPushEndpoint())
                .ifPresent(p -> {
                    throw new SubscriptionWithSameNameAndPushEndpointAlreadyExists();
                });

        webhookPusher.ping(subscription.getPushEndpoint());
        return subscriptionRepository.save(subscription);
    }

    @Override
    public void updateSubscription(Long id, Subscription subscription) {
        Subscription existingSubscription = getSubscription(id);

        subscriptionRepository.getDistinctByNameAndPushEndpoint(subscription.getName(), subscription.getPushEndpoint())
                .filter(p -> !p.getId().equals(id))
                .ifPresent(p -> {
                    throw new SubscriptionWithSameNameAndPushEndpointAlreadyExists();
                });

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
        getSubscription(id);
        subscriptionRepository.deleteSubscriptionById(id);
    }

    @Override
    public void deleteAll() {
        subscriptionRepository.deleteAll();
    }
}
