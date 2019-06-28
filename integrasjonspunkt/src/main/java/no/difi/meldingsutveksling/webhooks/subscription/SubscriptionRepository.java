package no.difi.meldingsutveksling.webhooks.subscription;

import no.difi.meldingsutveksling.domain.webhooks.QSubscription;
import no.difi.meldingsutveksling.domain.webhooks.Subscription;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface SubscriptionRepository extends PagingAndSortingRepository<Subscription, Long>,
        QuerydslPredicateExecutor<Subscription>,
        QuerydslBinderCustomizer<QSubscription> {

    @Override
    default void customize(QuerydslBindings bindings, QSubscription root) {
        // NOOP
    }
}
