package no.difi.meldingsutveksling.webhooks.subscription;

import no.difi.meldingsutveksling.domain.webhooks.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubscriptionService {

    Page<Subscription> listSubscriptions(Pageable pageable);

    Subscription getSubscription(Long id);

    Subscription createSubscription(Subscription subscription);

    void deleteSubscription(Long id);
}
