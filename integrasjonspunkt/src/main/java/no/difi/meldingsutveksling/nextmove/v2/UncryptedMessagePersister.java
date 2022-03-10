package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.message.BugFix610;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;

@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "difi.move.feature.cryptoMessagePersister", havingValue = "false")
@RequiredArgsConstructor
public class UncryptedMessagePersister implements OptionalCryptoMessagePersister {

    private final MessagePersister delegate;
    private final IntegrasjonspunktProperties props;

    public void write(String messageId, String filename, byte[] message) throws IOException {
        if (props.getNextmove().getApplyZipHeaderPatch() && ASIC_FILE.equals(filename)) {
            BugFix610.applyPatch(message, messageId);
        }
        delegate.write(messageId, filename, message);
    }

    public void writeStream(String messageId, String filename, InputStream stream) throws IOException {
        try (InputStream inputStream = possiblyApplyZipHeaderPatch(messageId, filename, stream)) {
            delegate.writeStream(messageId, filename, inputStream, -1L);
        }
    }

    private InputStream possiblyApplyZipHeaderPatch(String messageId, String filename, InputStream stream) throws IOException {
        if (props.getNextmove().getApplyZipHeaderPatch() && ASIC_FILE.equals(filename)) {
            return BugFix610.applyPatch(stream, messageId);
        }

        return stream;
    }

    public byte[] read(String messageId, String filename) throws IOException {
        return delegate.read(messageId, filename);
    }

    public FileEntryStream readStream(String messageId, String filename) throws IOException {
        InputStream inputStream = delegate.readStream(messageId, filename).getInputStream();
        return FileEntryStream.of(inputStream, -1);
    }

    public void delete(String messageId) throws IOException {
        delegate.delete(messageId);
    }
}
