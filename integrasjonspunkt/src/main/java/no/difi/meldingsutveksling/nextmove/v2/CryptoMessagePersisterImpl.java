package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.CryptoMessagePersister;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSDocument;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.message.BugFix610;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.Supplier;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;

@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoMessagePersisterImpl implements CryptoMessagePersister {

    private final MessagePersister delegate;
    private final KeystoreHelper keystoreHelper;
    private final IntegrasjonspunktProperties props;
    private final CreateCMSDocument createCMSDocument;
    private final DecryptCMSDocument decryptCMSDocument;
    private final PromiseMaker promiseMaker;
    private final Supplier<AlgorithmIdentifier> algorithmIdentifierSupplier;

    public void write(String messageId, String filename, Resource input) throws IOException {
        promiseMaker.promise(reject -> {
            try {
                Resource encrypted = createCMSDocument.encrypt(CreateCMSDocument.Input.builder()
                        .resource(possiblyApplyZipHeaderPatch(messageId, filename, input))
                        .certificate(keystoreHelper.getX509Certificate())
                        .keyEncryptionScheme(algorithmIdentifierSupplier.get())
                        .build(), reject);

                delegate.write(messageId, filename, encrypted);
                return null;
            } catch (IOException e) {
                throw new NextMoveRuntimeException("Writing of file %s failed for messageId: %s".formatted(filename, messageId));
            }
        }).await();
    }

    private Resource possiblyApplyZipHeaderPatch(String messageId, String filename, Resource stream) throws IOException {
        if (Boolean.TRUE.equals(props.getNextmove().getApplyZipHeaderPatch()) && ASIC_FILE.equals(filename)) {
            return BugFix610.applyPatch(stream, messageId);
        }

        return stream;
    }

    @Override
    public Resource read(String messageId, String filename) throws IOException {
        Resource encrypted = delegate.read(messageId, filename);
        return decryptCMSDocument.decrypt(getDecryptInput(encrypted));
    }

    private DecryptCMSDocument.Input getDecryptInput(Resource encrypted) {
        return DecryptCMSDocument.Input.builder()
                .keystoreHelper(keystoreHelper)
                .resource(encrypted)
                .build();
    }

    public void delete(String messageId) throws IOException {
        delegate.delete(messageId);
    }
}
