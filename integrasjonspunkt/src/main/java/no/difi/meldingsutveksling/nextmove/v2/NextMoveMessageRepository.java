package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface NextMoveMessageRepository extends CrudRepository<NextMoveMessage, Long>, QueryDslPredicateExecutor<NextMoveMessage> {
    List<NextMoveMessage> findAll();
    Optional<NextMoveMessage> findByConversationId(String conversationId);
}
