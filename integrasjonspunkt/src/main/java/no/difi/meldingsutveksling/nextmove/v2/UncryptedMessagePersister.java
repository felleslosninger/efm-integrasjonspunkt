package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.message.BugFix610;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;

@Slf4j
@Primary
@Component
@ConditionalOnProperty(name = "difi.move.feature.cryptoMessagePersister", havingValue = "false")
@RequiredArgsConstructor
public class UncryptedMessagePersister implements OptionalCryptoMessagePersister {

    private final MessagePersister delegate;
    private final IntegrasjonspunktProperties props;

    public void write(String messageId, String filename, Resource input) throws IOException {
        delegate.write(messageId, filename, possiblyApplyZipHeaderPatch(messageId, filename, input));
    }

    @Override
    public byte[] readBytes(String messageId, String filename) throws IOException {
        return delegate.readBytes(messageId, filename);
    }

    private Resource possiblyApplyZipHeaderPatch(String messageId, String filename, Resource stream) throws IOException {
        if (props.getNextmove().getApplyZipHeaderPatch() && ASIC_FILE.equals(filename)) {
            return BugFix610.applyPatch(stream, messageId);
        }

        return stream;
    }

    public InMemoryWithTempFileFallbackResource read(String messageId, String filename) throws IOException {
        return delegate.read(messageId, filename);
    }

    @Override
    public InputStreamResource stream(String messageId, String filename, Reject reject) throws IOException {
        return delegate.stream(messageId, filename, reject);
    }

    @Override
    public void read(String messageId, String filename, WritableResource writableResource) throws IOException {
        delegate.read(messageId, filename, writableResource);
    }

    public void delete(String messageId) throws IOException {
        delegate.delete(messageId);
    }
}
