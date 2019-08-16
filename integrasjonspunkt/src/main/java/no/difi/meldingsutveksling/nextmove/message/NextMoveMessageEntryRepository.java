package no.difi.meldingsutveksling.nextmove.message;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface NextMoveMessageEntryRepository extends CrudRepository<NextMoveMessageEntry, String> {

    Optional<NextMoveMessageEntry> findByMessageIdAndFilename(String messageId, String filename);

    void deleteByMessageId(String messageId);
}
