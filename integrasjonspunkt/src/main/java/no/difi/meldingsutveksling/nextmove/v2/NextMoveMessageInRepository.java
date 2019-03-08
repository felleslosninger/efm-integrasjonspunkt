package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import no.difi.meldingsutveksling.nextmove.QNextMoveInMessage;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface NextMoveMessageInRepository extends PagingAndSortingRepository<NextMoveInMessage, Long>,
        QueryDslPredicateExecutor<NextMoveInMessage>,
        QuerydslBinderCustomizer<QNextMoveInMessage> {

    Optional<NextMoveInMessage> findByConversationId(String conversationId);

    Optional<NextMoveInMessage> findFirstByLockTimeoutIsNullOrderByLastUpdatedAsc();

    @Override
    default void customize(QuerydslBindings bindings, QNextMoveInMessage root) {
        bindings.excluding(root.sbd);
    }
}
