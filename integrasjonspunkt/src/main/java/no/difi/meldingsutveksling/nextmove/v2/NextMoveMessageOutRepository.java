package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.QNextMoveOutMessage;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface NextMoveMessageOutRepository extends PagingAndSortingRepository<NextMoveOutMessage, Long>,
        QueryDslPredicateExecutor<NextMoveOutMessage>,
        QuerydslBinderCustomizer<QNextMoveOutMessage> {
    Optional<NextMoveOutMessage> findByConversationId(String conversationId);

    void deleteByConversationId(String conversationId);

    @Override
    default void customize(QuerydslBindings bindings, QNextMoveOutMessage root) {
        bindings.excluding(root.sbd);
    }
}
