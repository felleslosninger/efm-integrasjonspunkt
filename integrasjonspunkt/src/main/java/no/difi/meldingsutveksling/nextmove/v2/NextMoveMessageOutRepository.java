package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;

public interface NextMoveMessageOutRepository extends PagingAndSortingRepository<NextMoveOutMessage, Long>, QueryDslPredicateExecutor<NextMoveOutMessage> {
    List<NextMoveOutMessage> findAll();

    Optional<NextMoveOutMessage> findByConversationId(String conversationId);

    void save(NextMoveMessage msg);
}
