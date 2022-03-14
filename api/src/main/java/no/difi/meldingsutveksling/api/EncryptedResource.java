package no.difi.meldingsutveksling.api;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor
public class EncryptedResource extends AbstractResource {

    private final Resource resource;
    private final String description;

    public EncryptedResource() {
        this(null);
    }

    public EncryptedResource(@Nullable String description) {
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
e                .keystoreHelper(keystoreHelper)
                .resource(encrypted)
                .build();
    }
}
