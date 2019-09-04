package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ApplicationContextHolder;
import no.difi.meldingsutveksling.TmpFile;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.MessageContext;
import no.difi.meldingsutveksling.noarkexchange.MessageContextException;
import no.difi.meldingsutveksling.noarkexchange.MessageContextFactory;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@Slf4j
public class DpoConversationStrategy implements ConversationStrategy {

    private final TransportFactory transportFactory;
    private final AsicHandler asicHandler;
    private final MessageContextFactory messageContextFactory;
    private final ApplicationContextHolder applicationContextHolder;

    @Override
    public void send(NextMoveOutMessage message) throws NextMoveException {
        Transport transport = transportFactory.createTransport(message.getSbd());

        if (message.getFiles() == null || message.getFiles().isEmpty()) {
            transport.send(applicationContextHolder.getApplicationContext(), message.getSbd());
            return;
        }

        MessageContext messageContext = getMessageContext(message);

        try (InputStream is = asicHandler.createEncryptedAsic(message, messageContext)) {
            TmpFile tmpFile = createTemporaryZipFile(is);

            try (InputStream is2 = tmpFile.getInputStream()) {
                transport.send(applicationContextHolder.getApplicationContext(), message.getSbd(), is2);
            } finally {
                tmpFile.delete();
            }
        } catch (IOException e) {
            throw new NextMoveException(String.format("Error sending message with messageId=%s to Altinn", message.getMessageId()), e);
        }

        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to altinn",
                message.getMessageId(), message.getServiceIdentifier()),
                markerFrom(message));
    }

    private TmpFile createTemporaryZipFile(InputStream is) throws IOException {
        TmpFile tmpFile = TmpFile.create();
        try {
            FileUtils.copyInputStreamToFile(is, tmpFile.getFile());
        } catch (IOException e) {
            tmpFile.delete();
            throw e;
        }

        return tmpFile;
    }

    private MessageContext getMessageContext(NextMoveOutMessage message) throws NextMoveException {
        try {
            return messageContextFactory.from(message);
        } catch (MessageContextException e) {
            throw new NextMoveException(String.format("Error sending message with messageId=%s to Altinn", message.getMessageId()), e);
        }
    }


}
