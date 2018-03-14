package no.difi.meldingsutveksling.nextmove.message;

import no.difi.meldingsutveksling.nextmove.ConversationResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "difi.move.nextmove.useDbPersistence", havingValue = "true")
public class DBMessagePersister implements MessagePersister {

    private NextMoveMessageEntryRepository repo;

    @Autowired
    public DBMessagePersister(NextMoveMessageEntryRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void write(ConversationResource cr, String filename, byte[] message) throws IOException {
        NextMoveMessageEntry entry = NextMoveMessageEntry.of(cr.getConversationId(), filename, message);
        repo.save(entry);
    }

    @Override
    public byte[] read(ConversationResource cr, String filename) throws IOException {
        Optional<NextMoveMessageEntry> entry = repo.findByConversationIdAndFilename(cr.getConversationId(), filename);
        if (entry.isPresent()) {
            return entry.get().getContent();
        }
        throw new IOException(String.format("File \'%s\' for conversation with id=%s not found in repository", filename, cr.getConversationId()));
    }

    @Override
    @Transactional
    public void delete(ConversationResource cr) throws IOException {
        repo.deleteByConversationId(cr.getConversationId());
    }
}
