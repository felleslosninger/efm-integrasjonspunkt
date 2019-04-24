package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoMessagePersisterImpl implements CryptoMessagePersister {

    private final MessagePersister delegate;
    private final ObjectProvider<CmsUtil> cmsUtilProvider;
    private final IntegrasjonspunktNokkel keyInfo;

    public void write(String conversationId, String filename, byte[] message) throws IOException {
        byte[] encryptedMessage = getCmsUtil().createCMS(message, keyInfo.getX509Certificate());
        delegate.write(conversationId, filename, encryptedMessage);
    }

    public void writeStream(String conversationId, String filename, InputStream stream, long size) throws IOException {
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);

        CompletableFuture.runAsync(() -> {
            log.trace("Starting thread: writeStream encrypted");
            cmsUtilProvider.getIfAvailable().createCMSStreamed(stream, pos, keyInfo.getX509Certificate());
            try {
                pos.flush();
                pos.close();
            } catch (IOException e) {
                throw new NextMoveRuntimeException("Error closing writeStream output stream", e);
            }
            log.trace("Thread finished: writeStream encrypted");
        });

        delegate.writeStream(conversationId, filename, pis, size);
    }

    public byte[] read(String conversationId, String filename) throws IOException {
        return getCmsUtil().decryptCMS(delegate.read(conversationId, filename), keyInfo.loadPrivateKey());
    }

    public FileEntryStream readStream(String conversationId, String filename) throws IOException {

        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);

        CompletableFuture.runAsync(() -> {
            log.trace("Starting thread: readStream encrypted");
            try (FileEntryStream fileEntryStream = delegate.readStream(conversationId, filename)) {
                InputStream decryptedInputStream = getCmsUtil().decryptCMSStreamed(fileEntryStream.getInputStream(), keyInfo.loadPrivateKey());
                IOUtils.copy(decryptedInputStream, pos);
                pos.flush();
                pos.close();
            } catch (IOException e) {
                throw new NextMoveRuntimeException("Error closing attachment readStream output stream", e);
            }

            log.trace("Thread finished: readStream encrypted");
        });

        return FileEntryStream.of(pis, -1);
    }

    public void delete(String conversationId) throws IOException {
        delegate.delete(conversationId);
    }

    private CmsUtil getCmsUtil() {
        return cmsUtilProvider.getIfAvailable();
    }
}
