package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

public interface NextMoveMessageInRepository extends PagingAndSortingRepository<NextMoveInMessage, Long>, QueryDslPredicateExecutor<NextMoveInMessage> {
    List<NextMoveInMessage> findAll();

    Optional<NextMoveInMessage> findByConversationId(String conversationId);
}
