package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ApplicationContextHolder;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@Slf4j
public class DpoConversationStrategy implements ConversationStrategy {

    private final TransportFactory transportFactory;
    private final AsicHandler asicHandler;
    private final ApplicationContextHolder applicationContextHolder;

    @Override
    @Transactional
    public void send(NextMoveOutMessage message) throws NextMoveException {
        Transport transport = transportFactory.createTransport(message.getSbd());

        if (message.getFiles() == null || message.getFiles().isEmpty()) {
            transport.send(applicationContextHolder.getApplicationContext(), message.getSbd());
            return;
        }

        try (InputStream is = asicHandler.createEncryptedAsic(message)) {
            transport.send(applicationContextHolder.getApplicationContext(), message.getSbd(), is);
        } catch (IOException e) {
            Audit.error(String.format("Error sending message with messageId=%s to Altinn", message.getMessageId()), markerFrom(message), e);
            throw new NextMoveException(String.format("Error sending message with messageId=%s to Altinn", message.getMessageId()), e);
        }

        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to altinn",
                message.getMessageId(), message.getServiceIdentifier()),
                markerFrom(message));
    }

}
