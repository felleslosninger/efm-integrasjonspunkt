package no.difi.meldingsutveksling.nextmove.v2;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import org.springframework.core.io.AbstractResource;

import java.io.IOException;
import java.io.InputStream;

@ToString
@EqualsAndHashCode(callSuper = false)
public class CryptoMessageResource extends AbstractResource {

    private final String messageId;
    private final String filename;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;

    public CryptoMessageResource(String messageId, String filename, OptionalCryptoMessagePersister optionalCryptoMessagePersister) {
        this.messageId = messageId;
        this.filename = filename;
        this.optionalCryptoMessagePersister = optionalCryptoMessagePersister;
    }

    public String getMessageId() {
        return messageId;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @NonNull
    @Override
    public String getDescription() {
        return this.toString();
    }

    @NonNull
    @Override
    public InputStream getInputStream() throws IOException {
        return optionalCryptoMessagePersister.read(messageId, filename).getInputStream();
    }
}
