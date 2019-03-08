package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface NextMoveMessageRepository extends PagingAndSortingRepository<NextMoveMessage, Long>, QueryDslPredicateExecutor<NextMoveMessage> {
    Optional<NextMoveMessage> findByConversationId(String conversationId);

    void deleteByConversationId(String conversationId);
}
