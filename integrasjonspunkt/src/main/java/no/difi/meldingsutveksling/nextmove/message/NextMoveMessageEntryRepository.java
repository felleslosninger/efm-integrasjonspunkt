package no.difi.meldingsutveksling.nextmove.message;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface NextMoveMessageEntryRepository extends CrudRepository<NextMoveMessageEntry, String> {

    Optional<NextMoveMessageEntry> findByConversationIdAndFilename(String conversationId, String filename);
    List<NextMoveMessageEntry> findByConversationId(String conversationId);
}
