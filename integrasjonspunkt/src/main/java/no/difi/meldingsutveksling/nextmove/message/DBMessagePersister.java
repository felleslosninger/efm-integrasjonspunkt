package no.difi.meldingsutveksling.nextmove.message;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
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

    private IntegrasjonspunktProperties props;

    @Autowired
    public DBMessagePersister(NextMoveMessageEntryRepository repo, IntegrasjonspunktProperties props) {
        this.repo = repo;
        this.props = props;
    }

    @Override
    @Transactional
    public void write(String conversationId, String filename, byte[] message) throws IOException {

        if (props.getNextmove().getApplyZipHeaderPatch() && props.getNextmove().getAsicfile().equals(filename)){
            BugFix610.applyPatch(message, conversationId);
        }

        NextMoveMessageEntry entry = NextMoveMessageEntry.of(conversationId, filename, message);
        repo.save(entry);
    }

    @Override
    public byte[] read(String conversationId, String filename) throws IOException {
        Optional<NextMoveMessageEntry> entry = repo.findByConversationIdAndFilename(conversationId, filename);
        if (entry.isPresent()) {
            return entry.get().getContent();
        }
        throw new IOException(String.format("File \'%s\' for conversation with id=%s not found in repository", filename, conversationId));
    }

    @Override
    @Transactional
    public void delete(String conversationId) throws IOException {
        repo.deleteByConversationId(conversationId);
    }
}
