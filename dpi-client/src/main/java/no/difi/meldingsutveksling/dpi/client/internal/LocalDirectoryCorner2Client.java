package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dpi.client.DpiException;
import no.difi.meldingsutveksling.dpi.client.domain.CmsEncryptedAsice;
import no.difi.meldingsutveksling.dpi.client.domain.GetMessagesInput;
import no.difi.meldingsutveksling.dpi.client.domain.Message;
import no.difi.meldingsutveksling.dpi.client.domain.MessageStatus;
import no.difi.meldingsutveksling.dpi.client.internal.domain.SendMessageInput;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RequiredArgsConstructor
public class LocalDirectoryCorner2Client implements Corner2Client {

    private final IntegrasjonspunktProperties properties;

    @Override
    public void sendMessage(SendMessageInput input) {
        String base = UUID.randomUUID().toString();
        writeJWT(input.getJwt(), base);
        writeCmsEncryptedAsice(base, input.getCmsEncryptedAsice());
    }

    private void writeJWT(String jwt, String base) {
        try {
            Files.write(getPath(base, "jwt"), jwt.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't save file!", e);
        }
    }

    private void writeCmsEncryptedAsice(String base, CmsEncryptedAsice cmsEncryptedAsice) {
        try (InputStream is = cmsEncryptedAsice.getResource().getInputStream()) {
            Files.copy(is, getPath(base, "asic.cms"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't save file!", e);
        }
    }

    private Path getPath(String base, String postfix) {
        try {
            File targetFile = new File(new File(URI.create(properties.getDpi().getUri()).toURL().getFile()),
                    String.format("%s-%s.%s", base, properties.getDpi().getAsice().getType(), postfix));
            return targetFile.toPath();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Malformed URL", e);
        }
    }

    @Override
    public Flux<MessageStatus> getMessageStatuses(UUID messageId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Flux<Message> getMessages(GetMessagesInput input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CmsEncryptedAsice getCmsEncryptedAsice(URI downloadurl) throws DpiException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void markAsRead(UUID messageId) {
        throw new UnsupportedOperationException();
    }

}
