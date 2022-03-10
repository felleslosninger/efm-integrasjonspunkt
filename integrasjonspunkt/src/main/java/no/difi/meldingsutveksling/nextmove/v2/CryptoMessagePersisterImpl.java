package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.CryptoMessagePersister;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsAlgorithm;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateCMSDocument;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.message.BugFix610;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

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

    public void write(String messageId, String filename, Resource input) {
        promiseMaker.promise(reject -> {
            try {
                InputStreamResource encrypted = createCMSDocument.createCMS(CreateCMSDocument.Input.builder()
                        .resource(possiblyApplyZipHeaderPatch(messageId, filename, input))
                        .certificate(keystoreHelper.getX509Certificate())
                        .keyEncryptionScheme(getKeyEncryptionScheme())
                        .build(), reject);
                delegate.write(messageId, filename, encrypted);
                return null;
            } catch (IOException e) {
                throw new NextMoveRuntimeException(String.format("Writing of file %s failed for messageId: %s", filename, messageId));
            }
        }).await();
    }

    private Resource possiblyApplyZipHeaderPatch(String messageId, String filename, Resource stream) throws IOException {
        if (props.getNextmove().getApplyZipHeaderPatch() && ASIC_FILE.equals(filename)) {
            return BugFix610.applyPatch(stream, messageId);
        }

        return stream;
    }

    public InMemoryWithTempFileFallbackResource read(String messageId, String filename) {
        return decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                .keystoreHelper(keystoreHelper)
                .resource(delegate.read(messageId, filename))
                .build());
    }

    @Override
    public InputStreamResource read(String messageId, String filename, Reject reject) {
        return decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                .keystoreHelper(keystoreHelper)
                .resource(delegate.read(messageId, filename))
                .build(), reject);
    }

    public void delete(String messageId) throws IOException {
        delegate.delete(messageId);
    }

    private AlgorithmIdentifier getKeyEncryptionScheme() {
        if (props.getOrg().getKeystore().getType().toLowerCase().startsWith("windows") ||
                Boolean.TRUE.equals(props.getOrg().getKeystore().getLockProvider())) {
            return null;
        }
        return CmsAlgorithm.RSAES_OAEP;
    }
}
