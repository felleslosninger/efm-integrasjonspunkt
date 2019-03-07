package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.nextmove.NextMoveInMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface NextMoveMessageInRepository extends CrudRepository<NextMoveInMessage, Long>, QueryDslPredicateExecutor<NextMoveInMessage> {
    List<NextMoveInMessage> findAll();
    Optional<NextMoveInMessage> findByConversationId(String conversationId);
    void save(NextMoveMessage message);
}
