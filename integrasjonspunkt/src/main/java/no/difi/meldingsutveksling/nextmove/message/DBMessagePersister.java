package no.difi.meldingsutveksling.nextmove.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.nextmove.NextMoveMessageEntry;
import no.difi.move.common.io.ResourceUtils;
import no.difi.move.common.io.WritableByteArrayResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
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
    public byte[] readBytes(String messageId, String filename) throws IOException {
        WritableByteArrayResource writableByteArrayResource = new WritableByteArrayResource();
        this.read(messageId, filename, writableByteArrayResource);
        return writableByteArrayResource.toByteArray();
    }

    @Override
    @Transactional(readOnly = true)
    public Resource read(String messageId, String filename) throws IOException {
        NextMoveMessageEntry entry = repo.findByMessageIdAndFilename(messageId, filename)
                .orElseThrow(() -> new PersistenceException(String.format("Entry for conversationId=%s, filename=%s not found in database", messageId, filename)));
        return new BlobResource(entry.getContent(), String.format("BLOB for messageId=%s, filename=%s", messageId, filename));
    }

    @Override
    @Transactional(readOnly = true)
    public void read(String messageId, String filename, WritableResource writableResource) throws IOException {
        NextMoveMessageEntry entry = repo.findByMessageIdAndFilename(messageId, filename)
                .orElseThrow(() -> new PersistenceException(String.format("Entry for conversationId=%s, filename=%s not found in database", messageId, filename)));

        try (InputStream inputStream = entry.getContent().getBinaryStream()) {
            ResourceUtils.copy(inputStream, writableResource);
        } catch (SQLException e) {
            throw new PersistenceException("Error reading data stream from database", e);
        }
    }

    @Override
    @Transactional
    public void delete(String messageId) throws IOException {
        repo.deleteByMessageId(messageId);
    }
}
