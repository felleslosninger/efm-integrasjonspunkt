package no.difi.meldingsutveksling.webhooks.subscription;

import no.difi.meldingsutveksling.domain.webhooks.QSubscription;
import no.difi.meldingsutveksling.domain.webhooks.Subscription;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface SubscriptionRepository extends PagingAndSortingRepository<Subscription, Long>,
        QuerydslPredicateExecutor<Subscription>,
        QuerydslBinderCustomizer<QSubscription> {

    Optional<Subscription> getDistinctByNameAndPushEndpoint(String name, String pushEndpoint);

    /**
     * Using this to avoid Exception if the entity is already deleted at the time of deletion
     *
     * @param id subscription id
     */
    @Modifying
    @Query("DELETE FROM Subscription where id = ?1")
    void deleteSubscriptionById(Long id);

    @Override
    default void customize(QuerydslBindings bindings, QSubscription root) {
        // NOOP
    }
}
