package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.Decryptor;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.message.BugFix610;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.pipes.Pipe;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.pipes.Reject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.pipes.PipeOperations.close;
import static no.difi.meldingsutveksling.pipes.PipeOperations.copy;

@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoMessagePersisterImpl implements CryptoMessagePersister {

    private final MessagePersister delegate;
    private final ObjectProvider<CmsUtil> cmsUtilProvider;
    private final IntegrasjonspunktNokkel keyInfo;
    private final Plumber plumber;
    private final PromiseMaker promiseMaker;
    private final IntegrasjonspunktProperties props;

    public void write(String messageId, String filename, byte[] message) throws IOException {
        if (props.getNextmove().getApplyZipHeaderPatch() && ASIC_FILE.equals(filename)) {
            BugFix610.applyPatch(message, messageId);
        }
        byte[] encryptedMessage = getCmsUtil().createCMS(message, keyInfo.getX509Certificate());
        delegate.write(messageId, filename, encryptedMessage);
    }

    public void writeStream(String messageId, String filename, InputStream stream) {
        promiseMaker.promise(reject -> {
            try {
                InputStream inputStream = possiblyApplyZipHeaderPatch(messageId, filename, stream);
                Pipe pipe = plumber.pipe("CMS encrypt", inlet -> cmsUtilProvider.getIfAvailable().createCMSStreamed(inputStream, inlet, keyInfo.getX509Certificate()), reject);
                try (PipedInputStream is = pipe.outlet()) {
                    delegate.writeStream(messageId, filename, is, -1L);
                }
                return null;
            } catch (IOException e) {
                throw new NextMoveRuntimeException(String.format("Writing of file %s failed for messageId: %s", filename, messageId));
            }
        }).await();
    }

    private InputStream possiblyApplyZipHeaderPatch(String messageId, String filename, InputStream stream) throws IOException {
        if (props.getNextmove().getApplyZipHeaderPatch() && ASIC_FILE.equals(filename)) {
            return BugFix610.applyPatch(stream, messageId);
        }

        return stream;
    }

    public byte[] read(String messageId, String filename) throws IOException {
        return getCmsUtil().decryptCMS(delegate.read(messageId, filename), keyInfo.loadPrivateKey());
    }

    public FileEntryStream readStream(String messageId, String filename, Reject reject) {
        InputStream inputStream = delegate.readStream(messageId, filename).getInputStream();
        PipedInputStream pipedInputStream = plumber.pipe("Reading file", copy(inputStream).andThen(close(inputStream)), reject).outlet();
        return FileEntryStream.of(new Decryptor(keyInfo).decryptCMSStreamed(pipedInputStream), -1);
    }

    public void delete(String messageId) throws IOException {
        delegate.delete(messageId);
    }

    private CmsUtil getCmsUtil() {
        return cmsUtilProvider.getIfAvailable();
    }
}
