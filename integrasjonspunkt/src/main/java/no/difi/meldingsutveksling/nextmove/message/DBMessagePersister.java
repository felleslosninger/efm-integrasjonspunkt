package no.difi.meldingsutveksling.nextmove.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.nextmove.NextMoveMessageEntry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.nextmove.useDbPersistence", havingValue = "true")
@RequiredArgsConstructor
public class DBMessagePersister implements MessagePersister {

    private final BlobFactory blobFactory;
    private final NextMoveMessageEntryRepository repo;

    @Override
    @Transactional
    public void write(String messageId, String filename, Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            Blob blob = blobFactory.createBlob(inputStream, -1L);
            NextMoveMessageEntry entry = NextMoveMessageEntry.of(messageId, filename, blob, -1L);
            repo.save(entry);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Resource read(String messageId, String filename) throws IOException {
        NextMoveMessageEntry entry = repo.findByMessageIdAndFilename(messageId, filename).findFirst()
                .orElseThrow(() -> new PersistenceException(String.format("Entry for conversationId=%s, filename=%s not found in database", messageId, filename)));
        return new BlobResource(entry.getContent(), String.format("BLOB for messageId=%s, filename=%s", messageId, filename));
    }

    @Override
    @Transactional
    public void delete(String messageId) throws IOException {
        repo.deleteByMessageId(messageId);
    }
}
