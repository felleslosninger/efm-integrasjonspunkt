package no.difi.meldingsutveksling.nextmove.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.apache.commons.io.IOUtils;
import org.hibernate.LobHelper;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.nextmove.useDbPersistence", havingValue = "true")
@RequiredArgsConstructor
public class DBMessagePersister implements MessagePersister {

    private final NextMoveMessageEntryRepository repo;
    private final IntegrasjonspunktProperties props;
    private final EntityManager em;

    @Override
    @Transactional
    public void write(String conversationId, String filename, byte[] message) throws IOException {
        LobHelper lobHelper = em.unwrap(Session.class).getLobHelper();
        Blob contentBlob = lobHelper.createBlob(message);
        if (props.getNextmove().getApplyZipHeaderPatch() && ASIC_FILE.equals(filename)) {
            BugFix610.applyPatch(message, conversationId);
        }

        NextMoveMessageEntry entry = NextMoveMessageEntry.of(conversationId, filename, contentBlob, (long) message.length);
        repo.save(entry);
    }

    @Override
    @Transactional
    public void writeStream(String conversationId, String filename, InputStream stream, long size) throws IOException {
        LobHelper lobHelper = em.unwrap(Session.class).getLobHelper();
        Blob contentBlob = lobHelper.createBlob(stream, size);

        NextMoveMessageEntry entry = NextMoveMessageEntry.of(conversationId, filename, contentBlob, size);
        repo.save(entry);
    }

    @Override
    public byte[] read(String conversationId, String filename) throws IOException {
        NextMoveMessageEntry entry = repo.findByConversationIdAndFilename(conversationId, filename)
                .orElseThrow(() -> new IOException(String.format("File \'%s\' for conversation with id=%s not found in repository", filename, conversationId)));

        try {
            return IOUtils.toByteArray(entry.getContent().getBinaryStream());
        } catch (SQLException e) {
            throw new IOException("Error reading data stream from database", e);
        }
    }

    @Override
    public FileEntryStream readStream(String conversationId, String filename) {
        NextMoveMessageEntry entry = repo.findByConversationIdAndFilename(conversationId, filename)
                .orElseThrow(() -> new PersistenceException(String.format("Entry for conversationId=%s, filename=%s not found in database", conversationId, filename)));

        try {
            return FileEntryStream.of(entry.getContent().getBinaryStream(), entry.getSize());
        } catch (SQLException e) {
            throw new PersistenceException("Error reading data stream from database", e);
        }
    }

    @Override
    @Transactional
    public void delete(String conversationId) {
        repo.deleteByConversationId(conversationId);
    }
}
