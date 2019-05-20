package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.pipes.Pipe;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;

import static no.difi.meldingsutveksling.pipes.PipeOperations.close;
import static no.difi.meldingsutveksling.pipes.PipeOperations.copy;

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
        Pipe pipe = Pipe.of("CMS encrypt", inlet -> cmsUtilProvider.getIfAvailable().createCMSStreamed(stream, inlet, keyInfo.getX509Certificate()));
        delegate.writeStream(conversationId, filename, pipe.outlet(), size);
    }

    public byte[] read(String conversationId, String filename) throws IOException {
        return getCmsUtil().decryptCMS(delegate.read(conversationId, filename), keyInfo.loadPrivateKey());
    }

    public FileEntryStream readStream(String conversationId, String filename) {
        InputStream inputStream = delegate.readStream(conversationId, filename).getInputStream();
        PipedInputStream pipedInputStream = Pipe.of("Reading file", copy(inputStream).andThen(close(inputStream))).outlet();
        return FileEntryStream.of(getCmsUtil().decryptCMSStreamed(pipedInputStream, keyInfo.loadPrivateKey()), -1);
    }

    public void delete(String conversationId) throws IOException {
        delegate.delete(conversationId);
    }

    private CmsUtil getCmsUtil() {
        return cmsUtilProvider.getIfAvailable();
    }
}
