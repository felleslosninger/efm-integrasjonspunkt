package no.difi.meldingsutveksling.nextmove.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.nextmove.NextMoveMessageEntry;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.nextmove.useDbPersistence", havingValue = "true")
@RequiredArgsConstructor
public class DBMessagePersister implements MessagePersister {

    private final NextMoveMessageEntryRepository repo;
    private final BlobFactory blobFactory;

    @Override
    @Transactional
    public void write(String messageId, String filename, byte[] message) throws IOException {
        Blob contentBlob = blobFactory.createBlob(message);
        NextMoveMessageEntry entry = NextMoveMessageEntry.of(messageId, filename, contentBlob, (long) message.length);
        repo.save(entry);
    }

    @Override
    @Transactional
    public void writeStream(String messageId, String filename, InputStream stream, long size) throws IOException {
        Blob blob = blobFactory.createBlob(stream, size);
        NextMoveMessageEntry entry = NextMoveMessageEntry.of(messageId, filename, blob, size);
        repo.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] read(String messageId, String filename) throws IOException {
        NextMoveMessageEntry entry = repo.findByMessageIdAndFilename(messageId, filename).findFirst()
                .orElseThrow(() -> new IOException(String.format("File \'%s\' for message with id=%s not found in repository", filename, messageId)));

        try {
            return IOUtils.toByteArray(entry.getContent().getBinaryStream());
        } catch (SQLException e) {
            throw new IOException("Error reading data stream from database", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FileEntryStream readStream(String messageId, String filename) {
        NextMoveMessageEntry entry = repo.findByMessageIdAndFilename(messageId, filename).findFirst()
                .orElseThrow(() -> new PersistenceException(String.format("Entry for conversationId=%s, filename=%s not found in database", messageId, filename)));

        try {
            return FileEntryStream.of(entry.getContent().getBinaryStream(), entry.getSize());
        } catch (SQLException e) {
            throw new PersistenceException("Error reading data stream from database", e);
        }
    }

    @Override
    @Transactional
    public void delete(String messageId) {
        repo.deleteByMessageId(messageId);
    }
}
