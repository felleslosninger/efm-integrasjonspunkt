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
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.core.io.*;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
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
                InputStreamResource encrypted = createCMSDocument.createCMS(CreateCMSDocument.Input.builder()
                        .resource(possiblyApplyZipHeaderPatch(messageId, filename, input))
                        .certificate(keystoreHelper.getX509Certificate())
                        .keyEncryptionScheme(algorithmIdentifierSupplier.get())
                        .build(), reject);
                delegate.write(messageId, filename, encrypted);
                return null;
            } catch (IOException e) {
                throw new NextMoveRuntimeException(String.format("Writing of file %s failed for messageId: %s", filename, messageId));
            }
        }).await();
    }

    @Override
    public byte[] readBytes(String messageId, String filename) throws IOException {
        byte[] encrypted = delegate.readBytes(messageId, filename);
        return decryptCMSDocument.toByteArray(getDecryptInput(new ByteArrayResource(encrypted)));
    }

    private Resource possiblyApplyZipHeaderPatch(String messageId, String filename, Resource stream) throws IOException {
        if (props.getNextmove().getApplyZipHeaderPatch() && ASIC_FILE.equals(filename)) {
            return BugFix610.applyPatch(stream, messageId);
        }

        return stream;
    }

    public InMemoryWithTempFileFallbackResource read(String messageId, String filename) throws IOException {
        Resource encrypted = delegate.read(messageId, filename);
        return decryptCMSDocument.decrypt(getDecryptInput(encrypted));
    }

    @Override
    public InputStreamResource stream(String messageId, String filename, Reject reject) throws IOException {
        Resource encrypted = delegate.read(messageId, filename);
        return decryptCMSDocument.decrypt(getDecryptInput(encrypted), reject);
    }

    @Override
    public void read(String messageId, String filename, WritableResource writableResource) throws IOException {
        promiseMaker.promise(reject -> {
            try {
                Resource encrypted = delegate.read(messageId, filename);
                decryptCMSDocument.decrypt(getDecryptInput(encrypted), writableResource);
            } catch (IOException e) {
                reject.reject(e);
            }
            return null;
        }).await();
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

    public class EncryptedResource extends AbstractResource {

        private final Resource encrypted;
        private final String description;

        public EncryptedResource(Resource encrypted, @Nullable String description) {
            this.encrypted = encrypted;
            this.description = (description != null ? description : "");
        }

        public String getDescription() {
            return "Encrypted resource [" + this.description + "]";
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return null;
        }

        @Override
        public void read(String messageId, String filename, WritableResource writableResource) throws IOException {
            promiseMaker.promise(reject -> {
                try {
                    decryptCMSDocument.decrypt(getDecryptInput(), writableResource);
                } catch (IOException e) {
                    reject.reject(e);
                }
                return null;
            }).await();
        }

        private DecryptCMSDocument.Input getDecryptInput() {
            return DecryptCMSDocument.Input.builder()
                    .keystoreHelper(keystoreHelper)
                    .resource(encrypted)
                    .build();
        }
    }
}
