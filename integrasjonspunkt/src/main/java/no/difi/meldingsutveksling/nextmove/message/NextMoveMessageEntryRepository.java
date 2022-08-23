package no.difi.meldingsutveksling.nextmove.message;

import no.difi.meldingsutveksling.nextmove.NextMoveMessageEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.stream.Stream;

public interface NextMoveMessageEntryRepository extends CrudRepository<NextMoveMessageEntry, String>,
        JpaRepository<NextMoveMessageEntry, String> {

    Stream<NextMoveMessageEntry> findByMessageIdAndFilename(String messageId, String filename);

    void deleteByMessageId(String messageId);
}
