package no.difi.meldingsutveksling.nextmove.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface NextMoveMessageEntryRepository extends CrudRepository<NextMoveMessageEntry, String>,
        JpaRepository<NextMoveMessageEntry, String> {

    Optional<NextMoveMessageEntry> findByMessageIdAndFilename(String messageId, String filename);

    void deleteByMessageId(String messageId);
}
